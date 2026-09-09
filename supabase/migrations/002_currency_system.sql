-- =====================================================================
-- ZainQH Chat — قسم العملات (Currency System) — migration 002
-- شغّل هذا الملف مرة واحدة في Supabase SQL Editor بعد supabase/schema.sql.
--
-- ⚠️ بعكس supabase/schema.sql هذا الملف إضافي بالكامل (Additive) وليس
-- هداميًا: لا يحذف أي جدول أو دالة موجودة، ولا يمس المصادقة أو الدردشة أو
-- المتابعة أو نظام النقاط الحالي (points) إطلاقًا. كل عبارة هنا آمنة لإعادة
-- التشغيل (IF NOT EXISTS / CREATE OR REPLACE / ON CONFLICT DO NOTHING)
-- فلا يُنشئ جداولًا أو دوالًا مكررة إذا كانت موجودة أصلًا.
--
-- الأمان: عملات (coins) والألماس (diamonds) وملكية السيارات/الهدايا لا
-- يمكن للعميل تعديلها مباشرة أبدًا — فقط عبر دوال RPC أدناه (SECURITY
-- DEFINER) التي تتحقق من كل شرط على الخادم وتُنفَّذ ضمن معاملة واحدة ذرّية.
-- الأعمدة الجديدة على users غير مُدرجة في GRANT UPDATE الحالي (السطر الذي
-- ينفّذه schema.sql: grant update (name, age, avatar_url, ...) to
-- authenticated) لذلك تبقى غير قابلة للتعديل من العميل تلقائيًا بدون أي
-- حاجة لإعادة كتابة ذلك الـ GRANT.
-- =====================================================================

create extension if not exists "pgcrypto";

-- =====================================================================
-- 1. أعمدة جديدة على users (رصيد العملات/الألماس + حالة المكافآت)
-- =====================================================================
alter table public.users add column if not exists coins bigint not null default 0 check (coins >= 0);
alter table public.users add column if not exists diamonds bigint not null default 0 check (diamonds >= 0);
alter table public.users add column if not exists daily_reward_streak integer not null default 0 check (daily_reward_streak >= 0);
alter table public.users add column if not exists last_daily_reward_at timestamptz;
alter table public.users add column if not exists accumulator_started_at timestamptz not null default now();
alter table public.users add column if not exists ad_reward_window_start timestamptz;
alter table public.users add column if not exists ad_reward_count_in_window smallint not null default 0;

create index if not exists idx_users_coins on public.users (coins desc);

-- =====================================================================
-- 2. shop_cars — كتالوج السيارات (3 فقط، تصاميم خيالية أصلية)
-- =====================================================================
create table if not exists public.shop_cars (
    id                text primary key,
    name              text not null,
    tier              smallint not null,
    nominal_value_usd bigint not null,
    price_diamonds    integer not null check (price_diamonds > 0),
    icon              text not null,
    sort_order        smallint not null default 0,
    created_at        timestamptz not null default now()
);

alter table public.users
    add column if not exists selected_car_id text references public.shop_cars (id) on delete set null;

alter table public.shop_cars enable row level security;

drop policy if exists "shop_cars_select_all" on public.shop_cars;
create policy "shop_cars_select_all" on public.shop_cars for select using (true);
-- لا صلاحية INSERT/UPDATE/DELETE لأي عميل — الكتالوج يُدار فقط من SQL Editor.

insert into public.shop_cars (id, name, tier, nominal_value_usd, price_diamonds, icon, sort_order) values
    ('car_1', 'سيارة المدينة الذكية', 1, 2000,   15,  'directions_car', 1),
    ('car_2', 'السيارة الرياضية الماسية', 2, 10000,  100, 'sports_score',  2),
    ('car_3', 'الوحش الأسطوري الأرجواني', 3, 100000, 500, 'bolt',          3)
on conflict (id) do update set
    name = excluded.name,
    tier = excluded.tier,
    nominal_value_usd = excluded.nominal_value_usd,
    price_diamonds = excluded.price_diamonds,
    icon = excluded.icon,
    sort_order = excluded.sort_order;

-- =====================================================================
-- 3. user_cars — ملكية السيارات (تُظهَر في الملف الشخصي والمتصدرين)
-- =====================================================================
create table if not exists public.user_cars (
    user_id       text not null references public.users (id) on delete cascade,
    car_id        text not null references public.shop_cars (id) on delete cascade,
    purchased_at  timestamptz not null default now(),
    primary key (user_id, car_id)
);

create index if not exists idx_user_cars_user on public.user_cars (user_id);

alter table public.user_cars enable row level security;

drop policy if exists "user_cars_select_all" on public.user_cars;
create policy "user_cars_select_all" on public.user_cars
    for select using (true);   -- الملكية عامة كي تظهر في ملف أي مستخدم وفي المتصدرين
-- لا صلاحية INSERT/UPDATE/DELETE للعميل — فقط عبر purchase_car() أدناه.

-- =====================================================================
-- 4. shop_gifts — كتالوج الهدايا القابلة للإرسال
-- =====================================================================
create table if not exists public.shop_gifts (
    id              text primary key,
    name            text not null,
    icon            text not null,
    price_diamonds  integer not null check (price_diamonds > 0),
    coin_value      integer not null check (coin_value > 0),
    sort_order      smallint not null default 0,
    created_at      timestamptz not null default now()
);

alter table public.shop_gifts enable row level security;

drop policy if exists "shop_gifts_select_all" on public.shop_gifts;
create policy "shop_gifts_select_all" on public.shop_gifts for select using (true);

insert into public.shop_gifts (id, name, icon, price_diamonds, coin_value, sort_order) values
    ('gift_rose',    'وردة',        'local_florist', 5,   12,  1),
    ('gift_heart',   'قلب',         'favorite',      10,  25,  2),
    ('gift_crown',   'تاج',         'workspace_premium', 50,  130, 3),
    ('gift_diamond', 'خاتم الماس',  'diamond',       100, 260, 4)
on conflict (id) do update set
    name = excluded.name,
    icon = excluded.icon,
    price_diamonds = excluded.price_diamonds,
    coin_value = excluded.coin_value,
    sort_order = excluded.sort_order;

-- =====================================================================
-- 5. user_gifts_received — سجل الهدايا المُرسَلة/المُستلَمة
-- =====================================================================
create table if not exists public.user_gifts_received (
    id            bigint generated always as identity primary key,
    recipient_id  text not null references public.users (id) on delete cascade,
    sender_id     text not null references public.users (id) on delete cascade,
    gift_id       text not null references public.shop_gifts (id),
    coin_value    integer not null,
    status        text not null default 'pending' check (status in ('pending', 'claimed')),
    created_at    timestamptz not null default now(),
    claimed_at    timestamptz
);

create index if not exists idx_gifts_recipient on public.user_gifts_received (recipient_id, status, created_at desc);
create index if not exists idx_gifts_sender    on public.user_gifts_received (sender_id, created_at desc);

alter table public.user_gifts_received enable row level security;

drop policy if exists "gifts_select_involved" on public.user_gifts_received;
create policy "gifts_select_involved" on public.user_gifts_received
    for select using (auth.uid()::text = recipient_id or auth.uid()::text = sender_id);
-- لا صلاحية INSERT/UPDATE للعميل — فقط عبر send_gift() و claim_received_gift().

-- =====================================================================
-- 6. currency_transactions — سجل جميع العمليات المالية (Audit Log)
-- =====================================================================
create table if not exists public.currency_transactions (
    id            bigint generated always as identity primary key,
    user_id       text not null references public.users (id) on delete cascade,
    type          text not null check (type in (
                      'daily_reward', 'ad_reward', 'accumulator_collect',
                      'gift_sent', 'gift_received', 'diamond_purchase',
                      'car_purchase', 'admin_adjustment'
                  )),
    currency      text not null check (currency in ('coins', 'diamonds')),
    amount        bigint not null,             -- موجب = إضافة، سالب = خصم
    balance_after bigint not null,
    description   text not null,
    related_id    text,
    created_at    timestamptz not null default now()
);

create index if not exists idx_currency_tx_user on public.currency_transactions (user_id, created_at desc);

alter table public.currency_transactions enable row level security;

drop policy if exists "currency_tx_select_self" on public.currency_transactions;
create policy "currency_tx_select_self" on public.currency_transactions
    for select using (auth.uid()::text = user_id);
-- لا صلاحية INSERT/UPDATE/DELETE للعميل — تُكتَب فقط من دوال RPC أدناه.

-- السماح لنوع إشعار جديد 'gift' دون المساس بالأنواع الحالية.
alter table public.notifications drop constraint if exists notifications_type_check;
alter table public.notifications add constraint notifications_type_check
    check (type in ('message', 'follow', 'system', 'gift'));

-- =====================================================================
-- 7. دوال RPC — كل عملية مالية ذرّية (Atomic) بالكامل على الخادم
-- =====================================================================

-- 7.1 المكافأة اليومية المتصاعدة (10، 15، 20، 25...) — تعود من البداية عند الانقطاع.
create or replace function public.claim_daily_reward()
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_uid     text := auth.uid()::text;
    v_last    timestamptz;
    v_streak  integer;
    v_now     timestamptz := now();
    v_reward  integer;
    v_coins   bigint;
begin
    if v_uid is null then raise exception 'not_authenticated'; end if;

    select last_daily_reward_at, daily_reward_streak into v_last, v_streak
    from public.users where id = v_uid for update;

    if v_last is not null and v_now < v_last + interval '20 hours' then
        raise exception 'daily_reward_already_claimed';
    end if;

    -- انقطاع أكثر من 48 ساعة منذ آخر استلام = يعود العداد من البداية.
    if v_last is null or v_now > v_last + interval '48 hours' then
        v_streak := 0;
    end if;

    v_streak := v_streak + 1;
    v_reward := 10 + (v_streak - 1) * 5;

    update public.users
        set coins = coins + v_reward,
            daily_reward_streak = v_streak,
            last_daily_reward_at = v_now
        where id = v_uid
        returning coins into v_coins;

    insert into public.currency_transactions (user_id, type, currency, amount, balance_after, description)
    values (v_uid, 'daily_reward', 'coins', v_reward, v_coins, 'مكافأة يومية — اليوم رقم ' || v_streak);

    return json_build_object('reward', v_reward, 'streak', v_streak, 'coins', v_coins);
end;
$$;

-- 7.2 مكافأة إعلان مُشاهَد فعليًا (15 عملة) — 3 مرات كحد أقصى في الساعة.
-- ملاحظة أمان: هذه الدالة تضمن حد "3 مرات/ساعة" على الخادم بشكل غير قابل
-- للتلاعب من العميل. التحقق من "مشاهدة الإعلان فعليًا" نفسه يعتمد على أن
-- التطبيق لا يستدعي هذه الدالة إلا من داخل onUserEarnedReward لإعلان
-- Rewarded حقيقي من AdMob (راجع RewardedAdManager.kt) — تحقق خادمي إضافي
-- (Server-Side Verification من AdMob) غير مُفعَّل هنا لأنه يتطلب Webhook/
-- خادم منفصل خارج نطاق هذا التطبيق.
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

    if v_window_start is null or v_now > v_window_start + interval '1 hour' then
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

    return json_build_object('reward', v_reward, 'remaining_this_hour', 3 - v_count, 'coins', v_coins);
end;
$$;

-- 7.3 قراءة حالة التراكم فقط (بدون منح) — تُستخدم لعرض العدّاد الحي في
-- الواجهة بالاعتماد على ساعة الخادم لا ساعة الجهاز.
create or replace function public.get_accumulator_status()
returns json
language sql
security definer set search_path = public
stable
as $$
    select json_build_object('started_at', u.accumulator_started_at, 'server_now', now())
    from public.users u where u.id = auth.uid()::text;
$$;

-- 7.4 سحب العملات المتراكمة: +1 كل دقيقتين، +2 بعد 4 دقائق، +3 بعد 6...
-- (تراكم مثلثي: k دورة كل دورة دقيقتان تمنح مجموع 1+2+...+k). سقف أمان
-- 30 دورة (ساعة كاملة) لمنع التراكم غير المحدود أثناء غياب المستخدم.
create or replace function public.claim_accumulated_coins()
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_uid       text := auth.uid()::text;
    v_started   timestamptz;
    v_now       timestamptz := now();
    v_minutes   numeric;
    v_ticks     integer;
    v_reward    bigint;
    v_coins     bigint;
    v_max_ticks constant integer := 30;
begin
    if v_uid is null then raise exception 'not_authenticated'; end if;

    select accumulator_started_at into v_started from public.users where id = v_uid for update;
    if v_started is null then v_started := v_now; end if;

    v_minutes := extract(epoch from (v_now - v_started)) / 60.0;
    v_ticks := least(floor(v_minutes / 2.0)::integer, v_max_ticks);

    if v_ticks < 1 then
        raise exception 'accumulator_not_ready';
    end if;

    v_reward := (v_ticks * (v_ticks + 1)) / 2;

    update public.users
        set coins = coins + v_reward,
            accumulator_started_at = v_now
        where id = v_uid
        returning coins into v_coins;

    insert into public.currency_transactions (user_id, type, currency, amount, balance_after, description)
    values (v_uid, 'accumulator_collect', 'coins', v_reward, v_coins, 'سحب عملات متراكمة (' || v_ticks || ' دورة)');

    return json_build_object('reward', v_reward, 'ticks', v_ticks, 'coins', v_coins);
end;
$$;

-- 7.5 إرسال هدية لمستخدم آخر — خصم ألماس المرسِل + تسجيل + إشعار حقيقي.
create or replace function public.send_gift(p_recipient_id text, p_gift_id text)
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_uid         text := auth.uid()::text;
    v_price       integer;
    v_coin_value  integer;
    v_gift_name   text;
    v_diamonds    bigint;
    v_gift_row_id bigint;
begin
    if v_uid is null then raise exception 'not_authenticated'; end if;
    if p_recipient_id = v_uid then raise exception 'cannot_gift_self'; end if;
    if not exists (select 1 from public.users where id = p_recipient_id) then
        raise exception 'recipient_not_found';
    end if;

    select price_diamonds, coin_value, name into v_price, v_coin_value, v_gift_name
    from public.shop_gifts where id = p_gift_id;
    if v_price is null then raise exception 'gift_not_found'; end if;

    perform 1 from public.users where id = v_uid for update;

    if (select diamonds from public.users where id = v_uid) < v_price then
        raise exception 'insufficient_diamonds';
    end if;

    update public.users set diamonds = diamonds - v_price where id = v_uid
        returning diamonds into v_diamonds;

    insert into public.currency_transactions (user_id, type, currency, amount, balance_after, description, related_id)
    values (v_uid, 'gift_sent', 'diamonds', -v_price, v_diamonds, 'إرسال هدية: ' || v_gift_name, p_recipient_id);

    insert into public.user_gifts_received (recipient_id, sender_id, gift_id, coin_value, status)
    values (p_recipient_id, v_uid, p_gift_id, v_coin_value, 'pending')
    returning id into v_gift_row_id;

    insert into public.notifications (user_id, type, title, body, related_id, actor_id)
    select p_recipient_id, 'gift', u.name, 'أرسل لك هدية 🎁 ' || v_gift_name, v_gift_row_id::text, v_uid
    from public.users u where u.id = v_uid;

    return json_build_object('sent', true, 'diamonds', v_diamonds, 'gift_record_id', v_gift_row_id);
end;
$$;

-- 7.6 استلام هدية واردة — يحوّل قيمتها لعملات مرة واحدة فقط (Idempotent).
create or replace function public.claim_received_gift(p_gift_record_id bigint)
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_uid        text := auth.uid()::text;
    v_recipient  text;
    v_status     text;
    v_coin_value integer;
    v_coins      bigint;
begin
    if v_uid is null then raise exception 'not_authenticated'; end if;

    select recipient_id, status, coin_value into v_recipient, v_status, v_coin_value
    from public.user_gifts_received where id = p_gift_record_id for update;

    if v_recipient is null then raise exception 'gift_not_found'; end if;
    if v_recipient <> v_uid then raise exception 'not_authorized'; end if;
    if v_status = 'claimed' then raise exception 'gift_already_claimed'; end if;

    update public.user_gifts_received set status = 'claimed', claimed_at = now() where id = p_gift_record_id;

    update public.users set coins = coins + v_coin_value where id = v_uid returning coins into v_coins;

    insert into public.currency_transactions (user_id, type, currency, amount, balance_after, description, related_id)
    values (v_uid, 'gift_received', 'coins', v_coin_value, v_coins, 'استلام هدية', p_gift_record_id::text);

    return json_build_object('reward', v_coin_value, 'coins', v_coins);
end;
$$;

-- 7.7 شراء سيارة بالألماس (يمتلكها المستخدم ويمكن اختيارها لاحقًا).
create or replace function public.purchase_car(p_car_id text)
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_uid      text := auth.uid()::text;
    v_price    integer;
    v_diamonds bigint;
begin
    if v_uid is null then raise exception 'not_authenticated'; end if;

    if exists (select 1 from public.user_cars where user_id = v_uid and car_id = p_car_id) then
        raise exception 'car_already_owned';
    end if;

    select price_diamonds into v_price from public.shop_cars where id = p_car_id;
    if v_price is null then raise exception 'car_not_found'; end if;

    perform 1 from public.users where id = v_uid for update;
    if (select diamonds from public.users where id = v_uid) < v_price then
        raise exception 'insufficient_diamonds';
    end if;

    update public.users set diamonds = diamonds - v_price where id = v_uid returning diamonds into v_diamonds;

    insert into public.user_cars (user_id, car_id) values (v_uid, p_car_id);

    -- أول سيارة يمتلكها المستخدم تُختار تلقائيًا لإظهارها في ملفه الشخصي.
    update public.users set selected_car_id = p_car_id where id = v_uid and selected_car_id is null;

    insert into public.currency_transactions (user_id, type, currency, amount, balance_after, description, related_id)
    values (v_uid, 'car_purchase', 'diamonds', -v_price, v_diamonds, 'شراء سيارة', p_car_id);

    return json_build_object('purchased', true, 'diamonds', v_diamonds);
end;
$$;

-- 7.8 اختيار سيارة مملوكة لإظهارها في الملف الشخصي والمتصدرين (أو null لإلغاء الاختيار).
create or replace function public.select_owned_car(p_car_id text)
returns void
language plpgsql
security definer set search_path = public
as $$
declare
    v_uid text := auth.uid()::text;
begin
    if v_uid is null then raise exception 'not_authenticated'; end if;
    if p_car_id is not null and not exists (
        select 1 from public.user_cars where user_id = v_uid and car_id = p_car_id
    ) then
        raise exception 'car_not_owned';
    end if;
    update public.users set selected_car_id = p_car_id where id = v_uid;
end;
$$;

revoke all on function public.claim_daily_reward() from public, anon;
revoke all on function public.claim_ad_reward() from public, anon;
revoke all on function public.get_accumulator_status() from public, anon;
revoke all on function public.claim_accumulated_coins() from public, anon;
revoke all on function public.send_gift(text, text) from public, anon;
revoke all on function public.claim_received_gift(bigint) from public, anon;
revoke all on function public.purchase_car(text) from public, anon;
revoke all on function public.select_owned_car(text) from public, anon;

grant execute on function public.claim_daily_reward() to authenticated;
grant execute on function public.claim_ad_reward() to authenticated;
grant execute on function public.get_accumulator_status() to authenticated;
grant execute on function public.claim_accumulated_coins() to authenticated;
grant execute on function public.send_gift(text, text) to authenticated;
grant execute on function public.claim_received_gift(bigint) to authenticated;
grant execute on function public.purchase_car(text) to authenticated;
grant execute on function public.select_owned_car(text) to authenticated;

-- =====================================================================
-- 8. عرض المتصدرين حسب الثروة (منفصل تمامًا عن عرض leaderboard الحالي
--    القائم على النقاط points — لا يمسّه ولا يستبدله).
--    الثروة = العملات + الألماس + مجموع قيمة (بالألماس) السيارات المملوكة.
-- =====================================================================
create or replace view public.currency_leaderboard as
select
    u.id,
    u.username,
    u.name,
    u.avatar_url,
    u.coins,
    u.diamonds,
    coalesce(car_totals.cars_value, 0) as cars_value,
    (u.coins + u.diamonds + coalesce(car_totals.cars_value, 0)) as wealth_score,
    u.selected_car_id
from public.users u
left join (
    select uc.user_id, sum(sc.price_diamonds) as cars_value
    from public.user_cars uc
    join public.shop_cars sc on sc.id = uc.car_id
    group by uc.user_id
) car_totals on car_totals.user_id = u.id
order by wealth_score desc
limit 100;

-- =====================================================================
-- 9. Realtime — تفعيل التحديث الفوري للهدايا الواردة (رصيد users مُفعَّل
--    Realtime أصلًا في schema.sql، وأي عمود جديد عليه يصل تلقائيًا ضمن
--    الصف الكامل بدون أي إعداد إضافي).
-- =====================================================================
alter table public.user_gifts_received replica identity full;
alter table public.currency_transactions replica identity full;

do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and tablename = 'user_gifts_received'
    ) then
        alter publication supabase_realtime add table public.user_gifts_received;
    end if;

    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and tablename = 'currency_transactions'
    ) then
        alter publication supabase_realtime add table public.currency_transactions;
    end if;
end
$$;

-- =====================================================================
-- تم. هذا الملف آمن لإعادة التشغيل بالكامل في أي وقت.
-- =====================================================================
