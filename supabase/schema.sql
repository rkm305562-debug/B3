-- =====================================================================
-- ZainQH Chat — Supabase Schema (schema.sql)
-- Project: tofkrflxfndloewwyqcc
-- Run this whole file once in the Supabase SQL Editor (safe to re-run).
--
-- Design notes:
--   * Auth is Supabase Auth (auth.users). public.users.id = auth.uid()::text
--     so RLS policies can rely on auth.uid() directly.
--   * Column names mirror the app's Room entities (UserEntity,
--     ChatMessageEntity, FollowEntity, BlockedUserEntity) so the local
--     offline cache and the remote Postgres schema stay in sync.
--   * Every table has RLS enabled with explicit, minimal policies.
-- =====================================================================

-- =====================================================================
-- 0. RESET (destructive — intentional)
-- =====================================================================
-- ROOT CAUSE of "column "name" of relation "users" does not exist":
-- `create table if not exists public.users (...)` is a NO-OP if a table
-- named public.users already exists — even if that existing table has a
-- completely different (incompatible/legacy) set of columns. The script
-- would then reach `grant update (name, ...) on public.users` further down
-- and fail, because the *pre-existing* table never had a "name" column.
-- `IF NOT EXISTS` alone can never protect against this class of error.
--
-- The correct, permanent fix is to make this script unconditionally drop
-- and recreate exactly the objects it owns before creating them, so the
-- end state is always identical regardless of what existed before.
--
-- ⚠️ THIS DELETES ALL DATA in these tables. Only intended for the SQL
-- Editor when you deliberately want a clean/consistent schema (as you did
-- here). Do NOT run this against a production database with real user data
-- you want to keep.
drop trigger if exists on_auth_user_created on auth.users;

drop view if exists public.chat_previews cascade;
drop view if exists public.leaderboard cascade;

drop table if exists public.admin_action_log cascade;
drop table if exists public.points_transactions cascade;
drop table if exists public.notifications cascade;
drop table if exists public.reports cascade;
drop table if exists public.blocks cascade;
drop table if exists public.follows cascade;
drop table if exists public.chat_messages cascade;
drop table if exists public.user_settings cascade;
drop table if exists public.users cascade;

drop function if exists public.handle_new_auth_user() cascade;
drop function if exists public.handle_follow_change() cascade;
drop function if exists public.handle_new_message() cascade;
drop function if exists public.handle_follow_notification() cascade;
drop function if exists public.admin_set_ban_status(text, boolean, text) cascade;
drop function if exists public.admin_delete_message(text, text) cascade;
drop function if exists public.admin_remove_avatar(text, text) cascade;
drop function if exists public.admin_adjust_points(text, integer, text) cascade;

create extension if not exists "pgcrypto";

-- =====================================================================
-- 1. users  (public profile, 1:1 with auth.users)
-- =====================================================================
create table if not exists public.users (
    id                     text primary key,                 -- = auth.uid()::text
    username               text not null unique,
    name                   text not null,
    age                    integer not null default 18 check (age >= 13 and age <= 120),
    avatar_url             text,
    points                 integer not null default 0 check (points >= 0),
    follower_count         integer not null default 0 check (follower_count >= 0),
    following_count        integer not null default 0 check (following_count >= 0),
    is_online              boolean not null default false,
    last_active_timestamp  bigint not null default (extract(epoch from now()) * 1000)::bigint,
    created_at_timestamp   bigint not null default (extract(epoch from now()) * 1000)::bigint,
    created_at             timestamptz not null default now(),
    -- نظام صلاحيات المدير: عمود role وis_banned. هذان العمودان ليسا ضمن قائمة
    -- GRANT UPDATE أدناه المتاحة لأي مستخدم موثَّق (authenticated) — أي لا
    -- يمكن لأي مستخدم عادي (ولا حتى مدير) تعديلهما عبر UPDATE مباشر على
    -- الجدول إطلاقًا، فقط عبر: (أ) تعديل role يدويًا من SQL Editor من طرفك
    -- أنت فقط (لا يوجد أي مسار برمجي لتغييره)، أو (ب) دوال RPC الإدارية
    -- المحمية أدناه لـ is_banned. هذا يمنع أي تصعيد صلاحيات ذاتي حتى عبر
    -- استدعاء Supabase مباشرة.
    role                   text not null default 'user' check (role in ('user', 'admin')),
    is_banned              boolean not null default false
);

create index if not exists idx_users_username    on public.users (username);
create index if not exists idx_users_is_online    on public.users (is_online);
create index if not exists idx_users_points       on public.users (points desc);

alter table public.users enable row level security;

create policy "users_select_all" on public.users
    for select using (true);                       -- profiles are public within the app

create policy "users_insert_self" on public.users
    for insert with check (auth.uid()::text = id);

create policy "users_update_self" on public.users
    for update using (auth.uid()::text = id)
    with check (auth.uid()::text = id);

create policy "users_delete_self" on public.users
    for delete using (auth.uid()::text = id);

-- Auto-create a public.users row whenever someone signs up via Supabase Auth.
-- Defensive by design: age is clamped to the CHECK-constraint range, and any
-- unexpected error here is caught so it can NEVER abort the auth.users
-- transaction (i.e. account creation must still succeed even if this trigger
-- has a problem). The app has its own client-side self-heal fallback
-- (SupabaseDatabaseService.ensureUserProfileExists) for the rare case this
-- trigger doesn't end up creating the row.
create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
declare
    v_age integer;
begin
    v_age := coalesce((new.raw_user_meta_data ->> 'age')::integer, 18);
    v_age := greatest(13, least(120, v_age)); -- clamp to match the users.age CHECK constraint

    insert into public.users (id, username, name, age)
    values (
        new.id::text,
        coalesce(new.raw_user_meta_data ->> 'username', split_part(new.email, '@', 1)),
        coalesce(new.raw_user_meta_data ->> 'name', 'مستخدم جديد'),
        v_age
    )
    on conflict (id) do nothing;
    return new;
exception when others then
    raise warning 'handle_new_auth_user failed for user %: %', new.id, sqlerrm;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_auth_user();

-- Defense in depth *on top of* RLS: RLS above is row-level only, so without
-- this a user could still legally PATCH their own row's `points`,
-- `follower_count`, or `following_count` since "users_update_self" allows
-- updating the row. Restrict which *columns* the authenticated client role
-- may write; points/follower_count/following_count may then only ever be
-- changed by the SECURITY DEFINER trigger functions below, which run as the
-- table owner and are not subject to these column grants.
revoke update on public.users from authenticated;
grant update (name, age, avatar_url, is_online, last_active_timestamp) on public.users to authenticated;

-- =====================================================================
-- 2. user_settings  (1:1 with users)
-- =====================================================================
create table if not exists public.user_settings (
    user_id                text primary key references public.users (id) on delete cascade,
    notifications_enabled  boolean not null default true,
    sound_enabled          boolean not null default true,
    dark_theme             boolean not null default true,
    updated_at             timestamptz not null default now()
);

alter table public.user_settings enable row level security;

create policy "settings_select_self" on public.user_settings
    for select using (auth.uid()::text = user_id);

create policy "settings_upsert_self" on public.user_settings
    for insert with check (auth.uid()::text = user_id);

create policy "settings_update_self" on public.user_settings
    for update using (auth.uid()::text = user_id)
    with check (auth.uid()::text = user_id);

create policy "settings_delete_self" on public.user_settings
    for delete using (auth.uid()::text = user_id);

-- =====================================================================
-- 3. follows
-- =====================================================================
create table if not exists public.follows (
    follower_id   text not null references public.users (id) on delete cascade,
    following_id  text not null references public.users (id) on delete cascade,
    "timestamp"   bigint not null default (extract(epoch from now()) * 1000)::bigint,
    primary key (follower_id, following_id),
    constraint follows_no_self check (follower_id <> following_id)
);

create index if not exists idx_follows_following on public.follows (following_id);

alter table public.follows enable row level security;

create policy "follows_select_all" on public.follows
    for select using (true);

create policy "follows_insert_self" on public.follows
    for insert with check (auth.uid()::text = follower_id);

create policy "follows_delete_self" on public.follows
    for delete using (auth.uid()::text = follower_id);

-- Keep follower_count / following_count on users in sync automatically.
create or replace function public.handle_follow_change()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    if tg_op = 'INSERT' then
        update public.users set following_count = following_count + 1 where id = new.follower_id;
        update public.users set follower_count  = follower_count + 1  where id = new.following_id;
        insert into public.points_transactions (user_id, amount, reason, related_id)
        values (new.follower_id, 5, 'follow', new.following_id);
        update public.users set points = points + 5 where id = new.follower_id;
    elsif tg_op = 'DELETE' then
        update public.users set following_count = greatest(following_count - 1, 0) where id = old.follower_id;
        update public.users set follower_count  = greatest(follower_count - 1, 0)  where id = old.following_id;
    end if;
    return null;
end;
$$;

drop trigger if exists on_follow_change on public.follows;
create trigger on_follow_change
    after insert or delete on public.follows
    for each row execute function public.handle_follow_change();

-- =====================================================================
-- 4. blocks  (matches Room's BlockedUserEntity)
-- =====================================================================
create table if not exists public.blocks (
    blocker_id       text not null references public.users (id) on delete cascade,
    blocked_user_id  text not null references public.users (id) on delete cascade,
    reason           text not null default '',
    "timestamp"      bigint not null default (extract(epoch from now()) * 1000)::bigint,
    primary key (blocker_id, blocked_user_id),
    constraint blocks_no_self check (blocker_id <> blocked_user_id)
);

alter table public.blocks enable row level security;

create policy "blocks_select_self" on public.blocks
    for select using (auth.uid()::text = blocker_id);

create policy "blocks_insert_self" on public.blocks
    for insert with check (auth.uid()::text = blocker_id);

create policy "blocks_delete_self" on public.blocks
    for delete using (auth.uid()::text = blocker_id);

-- =====================================================================
-- 5. reports
-- =====================================================================
create table if not exists public.reports (
    id           uuid primary key default gen_random_uuid(),
    reporter_id  text not null references public.users (id) on delete cascade,
    reported_id  text not null references public.users (id) on delete cascade,
    message_id   text,
    reason       text not null,
    status       text not null default 'pending' check (status in ('pending', 'reviewed', 'dismissed')),
    created_at   timestamptz not null default now()
);

create index if not exists idx_reports_reported on public.reports (reported_id);

alter table public.reports enable row level security;

create policy "reports_select_own" on public.reports
    for select using (auth.uid()::text = reporter_id);

create policy "reports_insert_self" on public.reports
    for insert with check (auth.uid()::text = reporter_id);

-- =====================================================================
-- 6. chat_messages  (public chat + 1:1 private messages)
-- =====================================================================
create table if not exists public.chat_messages (
    id                 text primary key default gen_random_uuid()::text,
    sender_id          text not null references public.users (id) on delete cascade,
    sender_name        text not null,
    sender_avatar_url  text,
    sender_tier_name   text not null default 'NEW',
    recipient_id       text references public.users (id) on delete cascade,
    is_public          boolean not null default true,
    text               text not null default '',
    image_url          text,
    "timestamp"        bigint not null default (extract(epoch from now()) * 1000)::bigint,
    is_read            boolean not null default false,
    read_timestamp     bigint,
    created_at         timestamptz not null default now(),
    constraint chat_messages_public_or_direct check (
        (is_public = true and recipient_id is null) or
        (is_public = false and recipient_id is not null)
    )
);

create index if not exists idx_messages_public     on public.chat_messages (is_public, "timestamp" desc);
create index if not exists idx_messages_sender      on public.chat_messages (sender_id, "timestamp" desc);
create index if not exists idx_messages_recipient   on public.chat_messages (recipient_id, "timestamp" desc);
create index if not exists idx_messages_conversation on public.chat_messages (sender_id, recipient_id, "timestamp" desc);

alter table public.chat_messages enable row level security;

-- Read: public messages are visible to everyone that is authenticated;
-- private messages only to sender or recipient; blocked users are hidden.
create policy "messages_select" on public.chat_messages
    for select using (
        auth.uid() is not null
        and (
            is_public = true
            or auth.uid()::text = sender_id
            or auth.uid()::text = recipient_id
        )
        and not exists (
            select 1 from public.blocks b
            where (b.blocker_id = auth.uid()::text and b.blocked_user_id = sender_id)
               or (b.blocker_id = sender_id and b.blocked_user_id = auth.uid()::text)
        )
    );

-- Insert: users may only send messages as themselves, and only to people
-- who belong to their conversation (public) or a direct target they have
-- not blocked / are not blocked by.
create policy "messages_insert_self" on public.chat_messages
    for insert with check (
        auth.uid()::text = sender_id
        and not exists (
            select 1 from public.users u where u.id = auth.uid()::text and u.is_banned = true
        )
        and (
            is_public = true
            or (
                recipient_id is not null
                and not exists (
                    select 1 from public.blocks b
                    where (b.blocker_id = auth.uid()::text and b.blocked_user_id = recipient_id)
                       or (b.blocker_id = recipient_id and b.blocked_user_id = auth.uid()::text)
                )
            )
        )
    );

-- Update: only the recipient may mark a direct message as read; the
-- sender may edit their own message content.
create policy "messages_update" on public.chat_messages
    for update using (
        auth.uid()::text = sender_id or auth.uid()::text = recipient_id
    ) with check (
        auth.uid()::text = sender_id or auth.uid()::text = recipient_id
    );

create policy "messages_delete_own" on public.chat_messages
    for delete using (auth.uid()::text = sender_id);

-- Award +1 point to the sender for every message sent (per business rule).
create or replace function public.handle_new_message()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    update public.users set points = points + 1 where id = new.sender_id;
    insert into public.points_transactions (user_id, amount, reason, related_id)
    values (new.sender_id, 1, 'message', new.id);

    if new.is_public = false and new.recipient_id is not null then
        insert into public.notifications (user_id, type, title, body, related_id, actor_id)
        values (
            new.recipient_id,
            'message',
            new.sender_name,
            left(new.text, 140),
            new.id,
            new.sender_id
        );
    end if;
    return new;
end;
$$;

drop trigger if exists on_new_message on public.chat_messages;
create trigger on_new_message
    after insert on public.chat_messages
    for each row execute function public.handle_new_message();

-- =====================================================================
-- 7. points_transactions  (audit log backing users.points)
-- =====================================================================
create table if not exists public.points_transactions (
    id          bigint generated always as identity primary key,
    user_id     text not null references public.users (id) on delete cascade,
    amount      integer not null,
    reason      text not null check (reason in ('message', 'follow', 'bonus', 'adjustment')),
    related_id  text,
    created_at  timestamptz not null default now()
);

create index if not exists idx_points_tx_user on public.points_transactions (user_id, created_at desc);

alter table public.points_transactions enable row level security;

create policy "points_tx_select_self" on public.points_transactions
    for select using (auth.uid()::text = user_id);

-- Rows are only ever written by the SECURITY DEFINER triggers above,
-- so no INSERT/UPDATE/DELETE policy is granted to regular clients.

-- =====================================================================
-- 8. notifications
-- =====================================================================
create table if not exists public.notifications (
    id          bigint generated always as identity primary key,
    user_id     text not null references public.users (id) on delete cascade,
    type        text not null check (type in ('message', 'follow', 'system')),
    title       text not null,
    body        text,
    related_id  text,
    actor_id    text references public.users (id) on delete set null,
    is_read     boolean not null default false,
    created_at  timestamptz not null default now()
);

create index if not exists idx_notifications_user on public.notifications (user_id, is_read, created_at desc);

alter table public.notifications enable row level security;

create policy "notifications_select_self" on public.notifications
    for select using (auth.uid()::text = user_id);

create policy "notifications_update_self" on public.notifications
    for update using (auth.uid()::text = user_id)
    with check (auth.uid()::text = user_id);

create policy "notifications_delete_self" on public.notifications
    for delete using (auth.uid()::text = user_id);

-- Notify the new follower's target as well.
create or replace function public.handle_follow_notification()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    insert into public.notifications (user_id, type, title, body, related_id, actor_id)
    select new.following_id, 'follow', u.name, 'بدأ بمتابعتك', new.follower_id, new.follower_id
    from public.users u where u.id = new.follower_id;
    return new;
end;
$$;

drop trigger if exists on_follow_notification on public.follows;
create trigger on_follow_notification
    after insert on public.follows
    for each row execute function public.handle_follow_notification();

-- =====================================================================
-- 8b. Admin system — real server-side authorization, not UI-only
-- =====================================================================
-- Design: `role` and `is_banned` on public.users are NEVER client-writable
-- (excluded from the GRANT UPDATE list above). The only way to become an
-- admin is a manual SQL Editor UPDATE by the project owner (see report /
-- comment above). Every admin-only mutation below happens through a
-- SECURITY DEFINER function that independently re-verifies the caller's
-- role from the database on every single call — so even a user who calls
-- the Supabase REST/RPC API directly (bypassing the app UI entirely)
-- cannot perform any admin action without an actual admin row in `users`.

create table if not exists public.admin_action_log (
    id                 bigint generated always as identity primary key,
    admin_id           text references public.users (id) on delete set null,
    target_user_id     text references public.users (id) on delete set null,
    action_type        text not null check (
        action_type in ('ban_user', 'unban_user', 'delete_message', 'remove_avatar', 'adjust_points')
    ),
    reason             text,
    points_delta       integer,
    related_message_id text,
    created_at         timestamptz not null default now()
);

create index if not exists idx_admin_action_log_admin on public.admin_action_log (admin_id, created_at desc);

alter table public.admin_action_log enable row level security;

-- القراءة للمديرين فقط. لا توجد أي سياسة INSERT/UPDATE/DELETE لأي دور عميل
-- إطلاقًا — الكتابة تتم فقط من داخل دوال SECURITY DEFINER أدناه، فلا يمكن
-- حتى لمدير حقيقي تعديل أو حذف سجل الإجراءات مباشرة عبر REST.
create policy "admin_action_log_select_admins" on public.admin_action_log
    for select using (
        exists (select 1 from public.users u where u.id = auth.uid()::text and u.role = 'admin')
    );

-- حظر/فك حظر مستخدم (يمنعه فعليًا من إرسال أي رسالة، مُطبَّق عبر سياسة
-- messages_insert_self أعلاه — وليس مجرد إخفاء واجهة).
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
begin
    if not exists (select 1 from public.users where id = v_admin_id and role = 'admin') then
        raise exception 'not authorized: admin role required';
    end if;

    update public.users set is_banned = p_banned where id = p_target_user_id;

    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason)
    values (v_admin_id, p_target_user_id, case when p_banned then 'ban_user' else 'unban_user' end, p_reason);
end;
$$;

-- حذف رسالة من الدردشة العامة تحديدًا (نطاق صلاحية المدير المذكور في
-- الطلب: مراقبة/إدارة الدردشة العامة فقط، وليس الرسائل الخاصة بين مستخدمين
-- آخرين، حفاظًا على خصوصيتهم).
create or replace function public.admin_delete_message(
    p_message_id text,
    p_reason text default null
)
returns void
language plpgsql
security definer set search_path = public
as $$
declare
    v_admin_id text := auth.uid()::text;
    v_sender_id text;
begin
    if not exists (select 1 from public.users where id = v_admin_id and role = 'admin') then
        raise exception 'not authorized: admin role required';
    end if;

    select sender_id into v_sender_id from public.chat_messages where id = p_message_id and is_public = true;

    delete from public.chat_messages where id = p_message_id and is_public = true;

    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason, related_message_id)
    values (v_admin_id, v_sender_id, 'delete_message', p_reason, p_message_id);
end;
$$;

-- إزالة الصورة الشخصية المخالفة لمستخدم (يعود المستخدم تلقائيًا للصورة
-- الافتراضية لأن avatar_url = null). حذف الملف الفعلي من Storage يتم من
-- طرف التطبيق عبر سياسات storage.objects المحدَّثة أدناه (قسم 11).
create or replace function public.admin_remove_avatar(
    p_target_user_id text,
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

    update public.users set avatar_url = null where id = p_target_user_id;

    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason)
    values (v_admin_id, p_target_user_id, 'remove_avatar', p_reason);
end;
$$;

-- إضافة/خصم نقاط كمكافأة أو عقوبة، مسجَّلة في points_transactions الموجود
-- أصلاً (reason = 'adjustment'، القيمة المسموحة أصلاً في القيد الحالي) وفي
-- admin_action_log أيضًا لتتبّع "مَن فعل ماذا ولماذا" بوضوح.
create or replace function public.admin_adjust_points(
    p_target_user_id text,
    p_delta integer,
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

    update public.users set points = greatest(0, points + p_delta) where id = p_target_user_id;

    insert into public.points_transactions (user_id, amount, reason, related_id)
    values (p_target_user_id, p_delta, 'adjustment', v_admin_id);

    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason, points_delta)
    values (v_admin_id, p_target_user_id, 'adjust_points', p_reason, p_delta);
end;
$$;

revoke all on function public.admin_set_ban_status(text, boolean, text) from public, anon;
revoke all on function public.admin_delete_message(text, text) from public, anon;
revoke all on function public.admin_remove_avatar(text, text) from public, anon;
revoke all on function public.admin_adjust_points(text, integer, text) from public, anon;
grant execute on function public.admin_set_ban_status(text, boolean, text) to authenticated;
grant execute on function public.admin_delete_message(text, text) to authenticated;
grant execute on function public.admin_remove_avatar(text, text) to authenticated;
grant execute on function public.admin_adjust_points(text, integer, text) to authenticated;

-- =====================================================================
-- 9. Views
-- =====================================================================

-- Chat previews (one row per conversation) — mirrors ChatPreview domain model.
create or replace view public.chat_previews as
with direct_messages as (
    select
        m.*,
        case
            when m.sender_id = auth.uid()::text then m.recipient_id
            else m.sender_id
        end as other_user_id
    from public.chat_messages m
    where m.is_public = false
      and (m.sender_id = auth.uid()::text or m.recipient_id = auth.uid()::text)
),
latest_direct as (
    select distinct on (other_user_id)
        other_user_id, id, text, "timestamp", sender_id
    from direct_messages
    order by other_user_id, "timestamp" desc
),
unread_counts as (
    select sender_id as other_user_id, count(*) as unread_count
    from public.chat_messages
    where is_public = false
      and recipient_id = auth.uid()::text
      and is_read = false
    group by sender_id
)
select
    u.id as target_user_id,
    u.name as title,
    false as is_public,
    u.avatar_url,
    coalesce(ld.text, '') as last_message_text,
    coalesce(ld."timestamp", 0) as last_message_timestamp,
    coalesce(uc.unread_count, 0) as unread_count,
    u.is_online,
    u.points
from latest_direct ld
join public.users u on u.id = ld.other_user_id
left join unread_counts uc on uc.other_user_id = ld.other_user_id;

-- Public leaderboard view (top users by points).
create or replace view public.leaderboard as
select id, username, name, avatar_url, points, is_online
from public.users
order by points desc
limit 100;

-- =====================================================================
-- 10. Realtime
-- =====================================================================
-- REPLICA IDENTITY FULL is required so Realtime can include complete row
-- data on UPDATE/DELETE events (not just the primary key) and correctly
-- evaluate RLS policies for postgres_changes subscribers.
alter table public.chat_messages replica identity full;
alter table public.users replica identity full;
alter table public.notifications replica identity full;

-- Enable Realtime replication for live chat + presence updates.
alter publication supabase_realtime add table public.chat_messages;
alter publication supabase_realtime add table public.users;
alter publication supabase_realtime add table public.notifications;

-- =====================================================================
-- 11. Storage buckets & policies (avatars, chat-images)
-- =====================================================================
insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true)
on conflict (id) do nothing;

insert into storage.buckets (id, name, public)
values ('chat-images', 'chat-images', true)
on conflict (id) do nothing;

drop policy if exists "avatars_public_read" on storage.objects;
create policy "avatars_public_read" on storage.objects
    for select using (bucket_id = 'avatars');

drop policy if exists "avatars_owner_write" on storage.objects;
create policy "avatars_owner_write" on storage.objects
    for insert with check (bucket_id = 'avatars' and auth.uid() is not null);

drop policy if exists "avatars_owner_update" on storage.objects;
create policy "avatars_owner_update" on storage.objects
    for update using (bucket_id = 'avatars' and owner = auth.uid())
    with check (bucket_id = 'avatars' and owner = auth.uid());

drop policy if exists "avatars_owner_delete" on storage.objects;
create policy "avatars_owner_delete" on storage.objects
    for delete using (
        bucket_id = 'avatars' and (
            owner = auth.uid()
            or exists (select 1 from public.users u where u.id = auth.uid()::text and u.role = 'admin')
        )
    );

drop policy if exists "chat_images_public_read" on storage.objects;
create policy "chat_images_public_read" on storage.objects
    for select using (bucket_id = 'chat-images');

drop policy if exists "chat_images_owner_write" on storage.objects;
create policy "chat_images_owner_write" on storage.objects
    for insert with check (bucket_id = 'chat-images' and auth.uid() is not null);

drop policy if exists "chat_images_owner_delete" on storage.objects;
create policy "chat_images_owner_delete" on storage.objects
    for delete using (
        bucket_id = 'chat-images' and (
            owner = auth.uid()
            or exists (select 1 from public.users u where u.id = auth.uid()::text and u.role = 'admin')
        )
    );

-- =====================================================================
-- Done. Section 0 makes re-running this script always safe and correct:
-- every run drops and recreates exactly what this file owns, so the end
-- result never depends on what existed before.
-- =====================================================================
