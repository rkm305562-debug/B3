-- =====================================================================
-- ZainQH Chat — migration 006: تغيير حد مكافأة الإعلان إلى 3 مرات/يوم
-- (بدل 3 مرات/ساعة سابقًا) — الحد مُطبَّق على الخادم فعليًا وليس فقط في
-- الواجهة، فلا يمكن الالتفاف عليه من العميل.
-- =====================================================================

create or replace function public.claim_ad_reward()
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_uid           text := auth.uid()::text;
    v_window_start  timestamptz;
    v_count         smallint;
    v_now           timestamptz := now();
    v_reward        constant integer := 15;
    v_coins         bigint;
begin
    if v_uid is null then raise exception 'not_authenticated'; end if;

    select ad_reward_window_start, ad_reward_count_in_window into v_window_start, v_count
    from public.users where id = v_uid for update;

    -- نافذة يومية (24 ساعة) بدل الساعة الواحدة سابقًا.
    if v_window_start is null or v_now > v_window_start + interval '24 hours' then
        v_window_start := v_now;
        v_count := 0;
    end if;

    if v_count >= 3 then
        raise exception 'ad_reward_limit_reached';
    end if;

    v_count := v_count + 1;

    update public.users
        set coins = coins + v_reward,
            ad_reward_window_start = v_window_start,
            ad_reward_count_in_window = v_count
        where id = v_uid
        returning coins into v_coins;

    insert into public.currency_transactions (user_id, type, currency, amount, balance_after, description)
    values (v_uid, 'ad_reward', 'coins', v_reward, v_coins, 'مكافأة مشاهدة إعلان');

    return json_build_object('reward', v_reward, 'remaining_today', 3 - v_count, 'coins', v_coins);
end;
$$;

-- =====================================================================
-- تم. آمن لإعادة التشغيل بالكامل في أي وقت.
-- =====================================================================
