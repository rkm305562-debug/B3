-- =====================================================================
-- ZainQH Chat — migration 007: الحظر الدائم يحذف الحساب نهائيًا ويمنع
-- التسجيل بحساب جديد من نفس الجهاز حتى بعد حذف التطبيق وإعادة تثبيته.
-- إضافي بالكامل وآمن لإعادة التشغيل، لا يمسّ أي جدول أو دالة أخرى.
-- =====================================================================

-- =====================================================================
-- 1) عمود device_id على المستخدمين (معرّف الجهاز الذي سجّل منه الحساب،
--    يُرسَل من التطبيق وقت التسجيل — انظر SupabaseAuthServiceImpl.kt).
-- =====================================================================
alter table public.users add column if not exists device_id text;
create index if not exists idx_users_device_id on public.users (device_id);

-- =====================================================================
-- 2) جدول الأجهزة المحظورة نهائيًا — مستقل تمامًا عن جدول المستخدمين، لذا
--    يبقى محفوظًا حتى بعد حذف صاحبه بالكامل من public.users/auth.users.
-- =====================================================================
create table if not exists public.banned_devices (
    device_id       text primary key,
    banned_user_id  text,
    banned_username text,
    reason          text,
    banned_by       text,
    banned_at       timestamptz not null default now()
);

alter table public.banned_devices enable row level security;
-- عمدًا بلا أي سياسة SELECT/INSERT/UPDATE عامة — يُقرأ ويُكتب فقط عبر
-- الدوال الآمنة (security definer) أدناه، وليس مباشرة عبر PostgREST.

-- =====================================================================
-- 3) دالة عامة يستدعيها التطبيق قبل محاولة التسجيل مباشرة (بمفتاح anon،
--    قبل وجود أي جلسة)، لإظهار رسالة واضحة للمستخدم فورًا بدل خطأ عام
--    غامض من Supabase Auth (GoTrue) لاحقًا.
-- =====================================================================
create or replace function public.is_device_banned(p_device_id text)
returns boolean
language sql
security definer set search_path = public
stable
as $$
    select exists (
        select 1 from public.banned_devices where device_id = p_device_id
    );
$$;

revoke all on function public.is_device_banned(text) from public;
grant execute on function public.is_device_banned(text) to anon, authenticated;

-- =====================================================================
-- 4) تحديث مُشغّل إنشاء صف public.users ليحفظ device_id أيضًا.
-- =====================================================================
create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
declare
    v_age integer;
begin
    v_age := coalesce((new.raw_user_meta_data ->> 'age')::integer, 18);
    v_age := greatest(13, least(120, v_age));

    insert into public.users (id, username, name, age, device_id)
    values (
        new.id::text,
        coalesce(new.raw_user_meta_data ->> 'username', split_part(new.email, '@', 1)),
        coalesce(new.raw_user_meta_data ->> 'name', 'مستخدم جديد'),
        v_age,
        new.raw_user_meta_data ->> 'device_id'
    )
    on conflict (id) do nothing;
    return new;
exception when others then
    raise warning 'handle_new_auth_user failed for user %: %', new.id, sqlerrm;
    return new;
end;
$$;

-- =====================================================================
-- 5) حماية إضافية على مستوى القاعدة نفسها (Defense in depth): حتى لو
--    تجاوز أحدهم فحص التطبيق قبل التسجيل (مثلاً عبر استدعاء GoTrue
--    مباشرة)، يمنع هذا المُشغّل إدراج أي حساب مصادقة جديد من جهاز محظور
--    ويُلغي العملية بالكامل — خلافًا لمُشغّل handle_new_auth_user أعلاه
--    المصمَّم عمدًا ليبتلع أخطاءه وعدم إيقاف إنشاء الحساب أبدًا.
-- =====================================================================
create or replace function public.reject_banned_device()
returns trigger
language plpgsql
security definer set search_path = public
as $$
declare
    v_device_id text := new.raw_user_meta_data ->> 'device_id';
begin
    if v_device_id is not null and exists (
        select 1 from public.banned_devices where device_id = v_device_id
    ) then
        raise exception 'device_banned';
    end if;
    return new;
end;
$$;

drop trigger if exists on_auth_user_reject_banned_device on auth.users;
create trigger on_auth_user_reject_banned_device
    before insert on auth.users
    for each row execute function public.reject_banned_device();

-- =====================================================================
-- 6) الحظر الدائم أصبح يحذف الحساب فعليًا بالكامل (بدل مجرد وضع علامة
--    is_banned)، ويسجّل جهاز صاحبه في القائمة السوداء كي لا يستطيع نفس
--    الجهاز التسجيل بحساب جديد مطلقًا، حتى بعد حذف التطبيق وإعادة تثبيته.
--    فكّ الحظر (p_banned = false) بقي كما كان — يفيد فقط حالة الحظر
--    المؤقت (admin_set_temp_ban) التي لا تحذف صاحبها.
-- =====================================================================
create or replace function public.admin_set_ban_status(
    p_target_user_id text,
    p_banned boolean,
    p_reason text default null
)
returns void
language plpgsql
security definer set search_path = public
as $$
declare
    v_admin_id text := auth.uid()::text;
    v_device_id text;
    v_username text;
begin
    if not exists (select 1 from public.users where id = v_admin_id and role = 'admin') then
        raise exception 'not authorized: admin role required';
    end if;

    if not p_banned then
        update public.users
            set is_banned = false, banned_until = null
            where id = p_target_user_id;

        insert into public.admin_action_log (admin_id, target_user_id, action_type, reason)
        values (v_admin_id, p_target_user_id, 'unban_user', p_reason);
        return;
    end if;

    if p_target_user_id = v_admin_id then
        raise exception 'cannot_ban_self';
    end if;

    select device_id, username into v_device_id, v_username
    from public.users where id = p_target_user_id;

    if v_username is null then
        raise exception 'user_not_found';
    end if;

    if v_device_id is not null then
        insert into public.banned_devices (device_id, banned_user_id, banned_username, reason, banned_by)
        values (v_device_id, p_target_user_id, v_username, p_reason, v_admin_id)
        on conflict (device_id) do update
            set banned_user_id  = excluded.banned_user_id,
                banned_username = excluded.banned_username,
                reason          = excluded.reason,
                banned_by       = excluded.banned_by,
                banned_at       = now();
    end if;

    -- يُسجَّل قبل الحذف الفعلي (وليس بعده) حتى يبقى في سجل الإجراءات دومًا،
    -- تمامًا كما تفعل admin_delete_user الموجودة أصلاً.
    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason)
    values (v_admin_id, p_target_user_id, 'ban_user', p_reason);

    -- حذف صف auth.users يُسقط تلقائيًا صف public.users المرتبط به
    -- (on delete cascade) وكل ما يشير إليه في كامل قاعدة البيانات.
    delete from auth.users where id = p_target_user_id::uuid;
end;
$$;

-- =====================================================================
-- تم. آمن لإعادة التشغيل بالكامل في أي وقت.
-- =====================================================================
