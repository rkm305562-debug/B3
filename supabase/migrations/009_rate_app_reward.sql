-- =====================================================================
-- ZainQH Chat — migration 009: مكافأة تقييم التطبيق (15 نقطة، مرة واحدة
-- فقط لكل مستخدم). إضافي بالكامل وآمن لإعادة التشغيل، لا يمسّ أي جدول أو
-- دالة أخرى.
-- =====================================================================

alter table public.users add column if not exists rate_app_reward_claimed boolean not null default false;

create or replace function public.claim_rate_app_reward()
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_uid    text := auth.uid()::text;
    v_points integer;
begin
    if v_uid is null then
        raise exception 'not_authenticated';
    end if;

    if exists (select 1 from public.users where id = v_uid and rate_app_reward_claimed) then
        raise exception 'rate_app_reward_already_claimed';
    end if;

    update public.users
        set points = points + 15,
            rate_app_reward_claimed = true
        where id = v_uid
        returning points into v_points;

    insert into public.points_transactions (user_id, amount, reason, related_id)
    values (v_uid, 15, 'bonus', 'rate_app');

    return json_build_object('reward', 15, 'points', v_points);
end;
$$;

revoke all on function public.claim_rate_app_reward() from public, anon;
grant execute on function public.claim_rate_app_reward() to authenticated;

-- =====================================================================
-- تم. آمن لإعادة التشغيل بالكامل في أي وقت.
-- =====================================================================
