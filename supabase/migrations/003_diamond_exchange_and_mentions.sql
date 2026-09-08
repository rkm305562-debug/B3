-- =====================================================================
-- ZainQH Chat — migration 003: تبديل عملات↔ألماس + إشعارات الإشارة (@)
-- في الدردشة العامة. إضافي بالكامل وآمن لإعادة التشغيل، ولا يمسّ أي شيء
-- من schema.sql أو migrations/002_currency_system.sql.
-- =====================================================================

-- =====================================================================
-- 1. تبديل العملات إلى ألماس داخل التطبيق (1000 عملة = 1 ألماسة).
--    لا أموال حقيقية إطلاقًا — فقط تحويل بين رصيدين موجودين أصلًا.
-- =====================================================================
create or replace function public.exchange_coins_for_diamonds(p_diamonds integer)
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_uid          text := auth.uid()::text;
    v_coins_cost   bigint;
    v_coins        bigint;
    v_diamonds     bigint;
    v_rate         constant integer := 1000; -- 1000 عملة = 1 ألماسة
begin
    if v_uid is null then raise exception 'not_authenticated'; end if;
    if p_diamonds is null or p_diamonds <= 0 then raise exception 'invalid_amount'; end if;

    v_coins_cost := p_diamonds::bigint * v_rate;

    perform 1 from public.users where id = v_uid for update;

    if (select coins from public.users where id = v_uid) < v_coins_cost then
        raise exception 'insufficient_coins';
    end if;

    update public.users
        set coins = coins - v_coins_cost,
            diamonds = diamonds + p_diamonds
        where id = v_uid
        returning coins, diamonds into v_coins, v_diamonds;

    insert into public.currency_transactions (user_id, type, currency, amount, balance_after, description)
    values (v_uid, 'diamond_purchase', 'coins', -v_coins_cost, v_coins, 'تبديل عملات مقابل ' || p_diamonds || ' ألماسة');

    insert into public.currency_transactions (user_id, type, currency, amount, balance_after, description)
    values (v_uid, 'diamond_purchase', 'diamonds', p_diamonds, v_diamonds, 'ألماس من تبديل العملات');

    return json_build_object('coins', v_coins, 'diamonds', v_diamonds, 'spent', v_coins_cost);
end;
$$;

revoke all on function public.exchange_coins_for_diamonds(integer) from public, anon;
grant execute on function public.exchange_coins_for_diamonds(integer) to authenticated;

-- =====================================================================
-- 2. إشعارات حقيقية عند الإشارة لمستخدم (@) في الدردشة العامة — بدون
--    إغراق الجميع بإشعار عن كل رسالة، فقط من أُشير إليه فعليًا.
-- =====================================================================
alter table public.chat_messages
    add column if not exists mentioned_user_ids text[] not null default '{}';

alter table public.notifications drop constraint if exists notifications_type_check;
alter table public.notifications add constraint notifications_type_check
    check (type in ('message', 'follow', 'system', 'gift', 'mention'));

create or replace function public.handle_new_message()
returns trigger
language plpgsql
security definer set search_path = public
as $$
declare
    v_mentioned_id text;
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

    -- إشعار إشارة (@) في الدردشة العامة فقط لمن أُشير إليهم فعليًا.
    if new.is_public = true and new.mentioned_user_ids is not null then
        foreach v_mentioned_id in array new.mentioned_user_ids
        loop
            if v_mentioned_id is not null
                and v_mentioned_id <> new.sender_id
                and exists (select 1 from public.users where id = v_mentioned_id)
            then
                insert into public.notifications (user_id, type, title, body, related_id, actor_id)
                values (
                    v_mentioned_id,
                    'mention',
                    new.sender_name,
                    'أشار إليك في الدردشة العامة: ' || left(new.text, 100),
                    new.id,
                    new.sender_id
                );
            end if;
        end loop;
    end if;

    return new;
end;
$$;

-- =====================================================================
-- تم. آمن لإعادة التشغيل بالكامل في أي وقت.
-- =====================================================================
