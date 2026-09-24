-- =====================================================================
-- ZainQH Chat — migration 010
--   1) إصلاح زر "حذف حسابي" في الإعدادات: دالة delete_own_account لم تكن
--      موجودة في القاعدة أصلاً (كانت قد أُزيلت من schema.sql).
--   2) إصلاح الحظر الدائم للجهاز: كان يفشل بصمت لأي حساب لا يملك device_id
--      (حسابات قديمة أو أُنشئ ملفها احتياطيًا من التطبيق). دالة
--      register_my_device تربط الجهاز بالحساب عند كل دخول.
--   3) دمج "الحظر الدائم" و"الحذف النهائي" في إجراء واحد: admin_delete_user
--      أصبحت تستدعي admin_set_ban_status(true) نفسها.
-- إضافي وآمن لإعادة التشغيل. شغّله في Supabase → SQL Editor.
-- =====================================================================

-- (احتياطًا إن لم يُشغَّل 007 من قبل)
alter table public.users add column if not exists device_id text;
create index if not exists idx_users_device_id on public.users (device_id);

create table if not exists public.banned_devices (
    device_id       text primary key,
    banned_user_id  text,
    banned_username text,
    reason          text,
    banned_by       text,
    banned_at       timestamptz not null default now()
);
alter table public.banned_devices enable row level security;

create or replace function public.is_device_banned(p_device_id text)
returns boolean
language sql
security definer set search_path = public
stable
as $$
    select exists (select 1 from public.banned_devices where device_id = p_device_id);
$$;
revoke all on function public.is_device_banned(text) from public;
grant execute on function public.is_device_banned(text) to anon, authenticated;

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
-- 1) ربط جهاز المستخدم الحالي بحسابه (يُستدعى من التطبيق بعد كل دخول).
-- =====================================================================
create or replace function public.register_my_device(p_device_id text)
returns void
language plpgsql
security definer set search_path = public
as $$
begin
    if auth.uid() is null or p_device_id is null or length(trim(p_device_id)) < 8 then
        return;
    end if;
    update public.users
        set device_id = p_device_id
        where id = auth.uid()::text
          and device_id is distinct from p_device_id;
end;
$$;
revoke all on function public.register_my_device(text) from public, anon;
grant execute on function public.register_my_device(text) to authenticated;

-- =====================================================================
-- 2) حذف المستخدم لحسابه بنفسه (كان مفقودًا).
-- =====================================================================
create or replace function public.delete_own_account()
returns void
language plpgsql
security definer set search_path = public
as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then
        raise exception 'not authenticated';
    end if;

    -- ملفات التخزين (صور) إن وُجدت؛ أي فشل هنا لا يوقف حذف الحساب.
    begin
        delete from storage.objects where owner = v_uid;
    exception when others then
        null;
    end;

    -- مهم: public.users.id من نوع text وبلا مفتاح أجنبي نحو auth.users، لذا حذف
    -- حساب الدخول وحده كان يترك الملف الشخصي (واسم المستخدم ورسائله) كما هي.
    -- نحذف الملف الشخصي صراحةً (يُسقط ما يعتمد عليه بـ on delete cascade)،
    -- ثم حساب الدخول.
    delete from public.users where id = v_uid::text;
    delete from auth.users where id = v_uid;
end;
$$;
revoke all on function public.delete_own_account() from public, anon;
grant execute on function public.delete_own_account() to authenticated;

-- =====================================================================
-- 3) الحظر الدائم = حذف الحساب نهائيًا + حظر الجهاز (إجراء واحد).
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

    -- لا نحظر أبدًا قيمًا فارغة/مشتركة كي لا نحظر أجهزة بريئة.
    if v_device_id is not null
       and length(trim(v_device_id)) >= 8
       and lower(v_device_id) not in ('unknown', '9774d56d682e549c') then
        insert into public.banned_devices (device_id, banned_user_id, banned_username, reason, banned_by)
        values (v_device_id, p_target_user_id, v_username, p_reason, v_admin_id)
        on conflict (device_id) do update
            set banned_user_id  = excluded.banned_user_id,
                banned_username = excluded.banned_username,
                reason          = excluded.reason,
                banned_by       = excluded.banned_by,
                banned_at       = now();
    end if;

    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason)
    values (v_admin_id, p_target_user_id, 'ban_user', p_reason);

    begin
        delete from storage.objects where owner = p_target_user_id::uuid;
    exception when others then
        null;
    end;

    -- الملف الشخصي أولًا (لا مفتاح أجنبي بينه وبين auth.users) ثم حساب الدخول.
    delete from public.users where id = p_target_user_id;
    delete from auth.users where id = p_target_user_id::uuid;
end;
$$;

-- الحذف النهائي القديم يصبح نفس إجراء الحظر الدائم (زر واحد).
create or replace function public.admin_delete_user(
    p_target_user_id text,
    p_reason text default null
)
returns void
language plpgsql
security definer set search_path = public
as $$
begin
    perform public.admin_set_ban_status(p_target_user_id, true, p_reason);
end;
$$;

revoke all on function public.admin_set_ban_status(text, boolean, text) from public, anon;
revoke all on function public.admin_delete_user(text, text) from public, anon;
grant execute on function public.admin_set_ban_status(text, boolean, text) to authenticated;
grant execute on function public.admin_delete_user(text, text) to authenticated;

-- =====================================================================
-- اختياري (لا يُنفَّذ تلقائيًا): حسابات "حُذفت" سابقًا بالطريقة القديمة ما زال
-- ملفها الشخصي موجودًا بلا حساب دخول. لعرضها أولًا:
--   select id, username from public.users
--   where id not in (select id::text from auth.users);
-- وبعد التأكد أنها فعلًا حسابات محذوفة فقط يمكنك حذفها بـ:
--   delete from public.users where id not in (select id::text from auth.users);
-- =====================================================================

-- =====================================================================
-- تم. آمن لإعادة التشغيل بالكامل في أي وقت.
-- =====================================================================
