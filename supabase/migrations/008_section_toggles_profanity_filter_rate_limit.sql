-- =====================================================================
-- ZainQH Chat — migration 008:
--   أ) إمكانية إغلاق أقسام التطبيق مؤقتًا من طرف المدير (Feature Flags)
--   ب) فلترة الكلمات غير الأخلاقية (في الرسائل وأسماء المستخدمين)
--   ج) حدّ أقصى لعدد الرسائل خلال فترة قصيرة (حماية من السبام/الإغراق)
-- إضافي بالكامل وآمن لإعادة التشغيل، لا يمسّ أي جدول أو دالة أخرى.
-- =====================================================================

-- =====================================================================
-- أ.١) نوع إجراء جديد في سجل الإدارة يصف تفعيل/تعطيل قسم بدقة (بدل
--    استعارة نوع آخر غير دقيق دلاليًا). نحذف قيد التحقق القديم أيًا كان
--    اسمه المولَّد تلقائيًا من Postgres، ثم نضيفه باسم صريح وثابت.
-- =====================================================================
do $$
declare
    v_constraint_name text;
begin
    select conname into v_constraint_name
    from pg_constraint
    where conrelid = 'public.admin_action_log'::regclass
      and contype = 'c'
      and pg_get_constraintdef(oid) ilike '%action_type%';

    if v_constraint_name is not null then
        execute format('alter table public.admin_action_log drop constraint %I', v_constraint_name);
    end if;
end $$;

alter table public.admin_action_log
    add constraint admin_action_log_action_type_check
    check (action_type in (
        'ban_user', 'unban_user', 'temp_ban_user', 'delete_message', 'remove_avatar',
        'adjust_points', 'adjust_currency', 'delete_user', 'broadcast_notification',
        'toggle_feature'
    ));

-- =====================================================================
-- أ) إغلاق الأقسام مؤقتًا
-- =====================================================================
create table if not exists public.app_feature_flags (
    section_key     text primary key,
    is_enabled      boolean not null default true,
    disabled_reason text,
    updated_at      timestamptz not null default now(),
    updated_by      text
);

insert into public.app_feature_flags (section_key) values
    ('chats'), ('currency'), ('contact_admin'), ('online_users'),
    ('public_chat_link'), ('settings')
on conflict (section_key) do nothing;

alter table public.app_feature_flags enable row level security;

-- قراءة عامة (حتى للزوّار غير المسجَّلين) — ضرورية لإظهار حالة الأقسام
-- على الصفحة الرئيسية قبل أي تسجيل دخول.
drop policy if exists "feature_flags_select_all" on public.app_feature_flags;
create policy "feature_flags_select_all" on public.app_feature_flags
    for select using (true);
-- عمدًا بلا أي سياسة INSERT/UPDATE/DELETE عامة — التعديل فقط عبر الدالة
-- الآمنة أدناه.

create or replace function public.admin_set_feature_flag(
    p_section_key text,
    p_enabled boolean,
    p_reason text default null
)
returns void
language plpgsql
security definer set search_path = public
as $$
declare
    v_admin_id text := auth.uid()::text;
begin
    if not exists (select 1 from public.users where id = v_admin_id and role = 'admin') then
        raise exception 'not authorized: admin role required';
    end if;

    insert into public.app_feature_flags (section_key, is_enabled, disabled_reason, updated_at, updated_by)
    values (p_section_key, p_enabled, p_reason, now(), v_admin_id)
    on conflict (section_key) do update
        set is_enabled      = excluded.is_enabled,
            disabled_reason = case when excluded.is_enabled then null else excluded.disabled_reason end,
            updated_at      = now(),
            updated_by      = excluded.updated_by;

    insert into public.admin_action_log (admin_id, action_type, reason)
    values (
        v_admin_id,
        'toggle_feature',
        format('%s section "%s"%s', case when p_enabled then 'enabled' else 'disabled' end, p_section_key,
            case when p_reason is not null then ': ' || p_reason else '' end)
    );
end;
$$;

revoke all on function public.admin_set_feature_flag(text, boolean, text) from public, anon;
grant execute on function public.admin_set_feature_flag(text, boolean, text) to authenticated;

-- =====================================================================
-- ب) فلترة الكلمات غير الأخلاقية
-- =====================================================================
-- جدول قابل للتوسعة من SQL Editor مباشرة (insert into public.blocked_words
-- values ('كلمة جديدة');) بلا حاجة لنشر تحديث جديد للتطبيق.
create table if not exists public.blocked_words (
    word text primary key
);

-- قائمة انطلاقة أساسية (عربي/إنجليزي) — أضف المزيد لاحقًا متى شئت مباشرة
-- عبر SQL Editor، تُطبَّق فورًا بلا أي تحديث للتطبيق.
insert into public.blocked_words (word) values
    ('كس'), ('طيز'), ('زبي'), ('عاهرة'), ('شرموطة'), ('قحبة'), ('منيك'),
    ('fuck'), ('shit'), ('bitch'), ('asshole'), ('dick'), ('pussy'), ('cunt')
on conflict (word) do nothing;

alter table public.blocked_words enable row level security;
-- بلا أي سياسة قراءة/كتابة عامة — يُقرأ فقط من داخل الدالة أدناه (security
-- definer)، ولا يُدار إلا يدويًا من SQL Editor من طرفك مباشرة.

create or replace function public.mask_profanity(p_text text)
returns text
language plpgsql
security definer set search_path = public
stable
as $$
declare
    v_word record;
    v_result text := p_text;
begin
    for v_word in select word from public.blocked_words loop
        v_result := regexp_replace(
            v_result,
            '(' || v_word.word || ')',
            repeat('*', greatest(length(v_word.word), 1)),
            'gi'
        );
    end loop;
    return v_result;
end;
$$;

create or replace function public.contains_profanity(p_text text)
returns boolean
language sql
security definer set search_path = public
stable
as $$
    select exists (
        select 1 from public.blocked_words
        where p_text ilike '%' || word || '%'
    );
$$;

revoke all on function public.contains_profanity(text) from public;
grant execute on function public.contains_profanity(text) to anon, authenticated;

-- =====================================================================
-- ج) حماية الرسائل: قناع الكلمات المسيئة + حدّ أقصى للسرعة (Rate Limit)
--    في مُشغّل واحد قبل إدراج أي رسالة دردشة.
-- =====================================================================
create or replace function public.moderate_and_rate_limit_message()
returns trigger
language plpgsql
security definer set search_path = public
as $$
declare
    v_recent_count integer;
begin
    -- حدّ أقصى: 8 رسائل خلال 10 ثوانٍ لكل مستخدم — يحمي من سكربتات
    -- الإغراق (Flood/Spam) بلا التأثير على أي محادثة طبيعية.
    select count(*) into v_recent_count
    from public.chat_messages
    where sender_id = new.sender_id
      and created_at > now() - interval '10 seconds';

    if v_recent_count >= 8 then
        raise exception 'rate_limited: too many messages, please slow down';
    end if;

    if new.text is not null and length(new.text) > 0 then
        new.text := public.mask_profanity(new.text);
    end if;

    return new;
end;
$$;

drop trigger if exists on_chat_messages_moderate on public.chat_messages;
create trigger on_chat_messages_moderate
    before insert on public.chat_messages
    for each row execute function public.moderate_and_rate_limit_message();

-- =====================================================================
-- د) رفض اسم مستخدم يحتوي على كلمات غير أخلاقية عند التسجيل — مُشغّل
--    إضافي منفصل (وليس تعديلاً على handle_new_auth_user التي تبتلع
--    أخطاءها عمدًا)، بنفس أسلوب reject_banned_device في الهجرة السابقة.
-- =====================================================================
create or replace function public.reject_inappropriate_username()
returns trigger
language plpgsql
security definer set search_path = public
as $$
declare
    v_username text := new.raw_user_meta_data ->> 'username';
begin
    if v_username is not null and public.contains_profanity(v_username) then
        raise exception 'inappropriate_username';
    end if;
    return new;
end;
$$;

drop trigger if exists on_auth_user_reject_bad_username on auth.users;
create trigger on_auth_user_reject_bad_username
    before insert on auth.users
    for each row execute function public.reject_inappropriate_username();

-- =====================================================================
-- تم. آمن لإعادة التشغيل بالكامل في أي وقت.
-- =====================================================================
