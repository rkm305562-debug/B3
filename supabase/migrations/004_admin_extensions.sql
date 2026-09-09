-- =====================================================================
-- ZainQH Chat — migration 004: توسعة نظام الإدارة
-- (حذف مستخدمين، حظر مؤقت، تعديل عملات/ألماس، إشعار جماعي)
-- إضافي بالكامل وآمن لإعادة التشغيل، لا يمسّ أي جدول أو دالة من قبل.
-- =====================================================================

-- =====================================================================
-- 1. حظر مؤقت — عمود جديد + تحديث سياسة إرسال الرسائل لتحترمه فعليًا
--    (وليس فقط إخفاء واجهة).
-- =====================================================================
alter table public.users add column if not exists banned_until timestamptz;

drop policy if exists "messages_insert_self" on public.chat_messages;
create policy "messages_insert_self" on public.chat_messages
    for insert with check (
        auth.uid()::text = sender_id
        and not exists (
            select 1 from public.users u
            where u.id = auth.uid()::text
              and (u.is_banned = true or (u.banned_until is not null and u.banned_until > now()))
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

-- =====================================================================
-- 2. أنواع إجراءات إدارية جديدة في سجل الإجراءات.
-- =====================================================================
alter table public.admin_action_log drop constraint if exists admin_action_log_action_type_check;
alter table public.admin_action_log add constraint admin_action_log_action_type_check
    check (action_type in (
        'ban_user', 'unban_user', 'temp_ban_user', 'delete_message', 'remove_avatar',
        'adjust_points', 'adjust_currency', 'delete_user', 'broadcast_notification'
    ));

alter table public.admin_action_log add column if not exists coins_delta bigint;
alter table public.admin_action_log add column if not exists diamonds_delta bigint;

-- =====================================================================
-- 3. حظر مؤقت لمدة محددة بالساعات (RPC مستقل عن الحظر الدائم الحالي).
-- =====================================================================
create or replace function public.admin_set_temp_ban(
    p_target_user_id text,
    p_hours integer,
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
    if p_hours is null or p_hours <= 0 then
        raise exception 'invalid_duration';
    end if;

    update public.users
        set is_banned = true,
            banned_until = now() + make_interval(hours => p_hours)
        where id = p_target_user_id;

    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason)
    values (v_admin_id, p_target_user_id, 'temp_ban_user', coalesce(p_reason, p_hours || ' ساعة'));
end;
$$;

-- عند فك الحظر (الدائم) نُفرغ banned_until أيضًا كي لا يبقى القديم فعّالًا.
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

    update public.users
        set is_banned = p_banned,
            banned_until = case when p_banned then banned_until else null end
        where id = p_target_user_id;

    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason)
    values (v_admin_id, p_target_user_id, case when p_banned then 'ban_user' else 'unban_user' end, p_reason);
end;
$$;

-- =====================================================================
-- 4. حذف مستخدم نهائيًا (يشمل حساب المصادقة نفسه؛ كل بياناته المرتبطة
--    تُحذف تلقائيًا عبر on delete cascade في كل الجداول المرجعية).
-- =====================================================================
create or replace function public.admin_delete_user(
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
    if p_target_user_id = v_admin_id then
        raise exception 'cannot_delete_self';
    end if;
    if not exists (select 1 from public.users where id = p_target_user_id) then
        raise exception 'user_not_found';
    end if;

    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason)
    values (v_admin_id, p_target_user_id, 'delete_user', p_reason);

    -- حذف صف auth.users يُسقط تلقائيًا صف public.users المرتبط به
    -- (on delete cascade) وكل ما يشير إليه في كامل قاعدة البيانات.
    delete from auth.users where id = p_target_user_id::uuid;
end;
$$;

-- =====================================================================
-- 5. زيادة/خصم عملات أو ألماس المستخدم (منفصل عن نظام النقاط points).
-- =====================================================================
create or replace function public.admin_adjust_currency(
    p_target_user_id text,
    p_coins_delta bigint default 0,
    p_diamonds_delta bigint default 0,
    p_reason text default null
)
returns void
language plpgsql
security definer set search_path = public
as $$
declare
    v_admin_id text := auth.uid()::text;
    v_coins bigint;
    v_diamonds bigint;
begin
    if not exists (select 1 from public.users where id = v_admin_id and role = 'admin') then
        raise exception 'not authorized: admin role required';
    end if;
    if coalesce(p_coins_delta, 0) = 0 and coalesce(p_diamonds_delta, 0) = 0 then
        raise exception 'invalid_amount';
    end if;

    update public.users
        set coins = greatest(0, coins + coalesce(p_coins_delta, 0)),
            diamonds = greatest(0, diamonds + coalesce(p_diamonds_delta, 0))
        where id = p_target_user_id
        returning coins, diamonds into v_coins, v_diamonds;

    if v_coins is null then
        raise exception 'user_not_found';
    end if;

    if coalesce(p_coins_delta, 0) <> 0 then
        insert into public.currency_transactions (user_id, type, currency, amount, balance_after, description, related_id)
        values (p_target_user_id, 'admin_adjustment', 'coins', p_coins_delta, v_coins, coalesce(p_reason, 'تعديل إداري'), v_admin_id);
    end if;

    if coalesce(p_diamonds_delta, 0) <> 0 then
        insert into public.currency_transactions (user_id, type, currency, amount, balance_after, description, related_id)
        values (p_target_user_id, 'admin_adjustment', 'diamonds', p_diamonds_delta, v_diamonds, coalesce(p_reason, 'تعديل إداري'), v_admin_id);
    end if;

    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason, coins_delta, diamonds_delta)
    values (v_admin_id, p_target_user_id, 'adjust_currency', p_reason, p_coins_delta, p_diamonds_delta);
end;
$$;

-- =====================================================================
-- 6. إرسال إشعار لجميع المستخدمين دفعة واحدة.
-- =====================================================================
create or replace function public.admin_broadcast_notification(
    p_title text,
    p_body text
)
returns integer
language plpgsql
security definer set search_path = public
as $$
declare
    v_admin_id text := auth.uid()::text;
    v_count integer;
begin
    if not exists (select 1 from public.users where id = v_admin_id and role = 'admin') then
        raise exception 'not authorized: admin role required';
    end if;
    if p_title is null or length(trim(p_title)) = 0 then
        raise exception 'invalid_title';
    end if;

    insert into public.notifications (user_id, type, title, body, actor_id)
    select id, 'system', p_title, p_body, v_admin_id
    from public.users
    where id <> v_admin_id;

    get diagnostics v_count = row_count;

    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason)
    values (v_admin_id, null, 'broadcast_notification', p_title);

    return v_count;
end;
$$;

revoke all on function public.admin_set_temp_ban(text, integer, text) from public, anon;
revoke all on function public.admin_delete_user(text, text) from public, anon;
revoke all on function public.admin_adjust_currency(text, bigint, bigint, text) from public, anon;
revoke all on function public.admin_broadcast_notification(text, text) from public, anon;

grant execute on function public.admin_set_temp_ban(text, integer, text) to authenticated;
grant execute on function public.admin_delete_user(text, text) to authenticated;
grant execute on function public.admin_adjust_currency(text, bigint, bigint, text) to authenticated;
grant execute on function public.admin_broadcast_notification(text, text) to authenticated;

-- =====================================================================
-- تم. آمن لإعادة التشغيل بالكامل في أي وقت.
-- =====================================================================
