-- =====================================================================
-- ZainQH Chat — migration 011
-- قسم "الأجهزة المحظورة" في لوحة الإدارة: عرض الأجهزة المحظورة نهائيًا
-- وفك حظر جهاز معيّن. إضافي بالكامل وآمن لإعادة التشغيل — لا يمسّ أي جدول.
-- شغّل 010 قبله.
-- =====================================================================

create or replace function public.admin_list_banned_devices()
returns setof public.banned_devices
language plpgsql
security definer set search_path = public
as $$
begin
    if not exists (
        select 1 from public.users where id = auth.uid()::text and role = 'admin'
    ) then
        raise exception 'not authorized: admin role required';
    end if;

    return query
        select * from public.banned_devices order by banned_at desc;
end;
$$;

create or replace function public.admin_unban_device(p_device_id text)
returns void
language plpgsql
security definer set search_path = public
as $$
declare
    v_admin_id text := auth.uid()::text;
    v_username text;
begin
    if not exists (select 1 from public.users where id = v_admin_id and role = 'admin') then
        raise exception 'not authorized: admin role required';
    end if;

    select banned_username into v_username
    from public.banned_devices where device_id = p_device_id;

    if not found then
        raise exception 'device_not_found';
    end if;

    delete from public.banned_devices where device_id = p_device_id;

    insert into public.admin_action_log (admin_id, target_user_id, action_type, reason)
    values (v_admin_id, null, 'unban_user',
            'فك حظر جهاز' || coalesce(' (' || v_username || ')', ''));
end;
$$;

revoke all on function public.admin_list_banned_devices() from public, anon;
revoke all on function public.admin_unban_device(text) from public, anon;
grant execute on function public.admin_list_banned_devices() to authenticated;
grant execute on function public.admin_unban_device(text) to authenticated;
