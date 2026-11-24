# Kecak Permissions: Office Hour and Holidays

This bundle provides **Work Hours Permission** and **Day-of-Week and Holiday
Permission**.

## Project structure

All implementation packages are under
`com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays`:

- `activator` — OSGi bundle lifecycle and plugin registration
- `holiday.client` — national holiday API client
- `holiday.model` — holiday data model
- `holiday.repository` — database access
- `holiday.service` — holiday synchronization and lookup
- `permission.dayofweek` — day-of-week and holiday permission
- `permission.workhours` — work-hours permission
- `permission.validator` — day-of-week and holiday rules

Permission class names changed during this refactor. Existing Kecak
configurations that reference the previous `PermissionByDate` or
`WorkHourPermission` class names must be updated to the new plugin names.

## Build

Run `mvn clean package` from this directory. The plugin bundle is created under
`target`.

## Work Hours Permission

Configure allowed work intervals in the permission settings. The current user's
timezone is used when available; Asia/Jakarta is the fallback.

## Day-of-Week and Holiday Permission

This Form Permission plugin allows access according to the current day of the
week and Indonesian national holidays. Access can be independently allowed or
blocked for each day, Monday through Sunday. **Allow access on national
holidays** controls whether a national holiday bypasses the selected weekdays.
When enabled, holiday access is allowed even if that weekday is unchecked;
when disabled, access is blocked on holidays. On other dates, access follows
the allowed-days selection. Existing configurations with individual day
properties remain supported.

Before deploying, create the `app_holiday_calendar` table in the application's
`setupDataSource` database using
[`src/main/resources/app_holiday_calendar.sql`](src/main/resources/app_holiday_calendar.sql).
If the current year has no holiday records, the plugin retrieves confirmed
holidays from APIHariLibur_V2 on its first permission evaluation and stores
them locally. Network, API, or database failures are surfaced rather than
silently treated as non-holidays.

The API source is maintained by a third party, not an Indonesian government
service. Collective leave (`Cuti Bersama` / `Joint Holiday`) and tentative
(`belum pasti`) dates are excluded. The initial sync requires internet access
to `raw.githubusercontent.com`; subsequent evaluations use the local database.

The internal table is the default source and continues its yearly API sync.
Alternatively, enable **Use a custom holiday calendar source**, then select a
form and one of its Date Picker fields. The table and field lists are scoped to
forms in the current app. The date field must store dates in `yyyy-MM-dd`
format, and the form table must already contain the holiday records; this
plugin does not create forms or populate the selected form table.

An optional status field can store `Active`/`Inactive`, `Aktif`/`Tidak Aktif`,
or boolean values. An active holiday follows **Allow access on national
holidays**. An inactive record is treated as a non-holiday and access follows
the allowed-days setting. If there is no matching record, the allowed-days
setting also applies. Without a status field, custom holidays retain the
previous behavior.

When this permission is assigned to the root Form, its `blockedMessage`
overrides the standard unauthorized message when date validation denies
access. For a field or section, Kecak omits the element; it does not display a
permission-specific message for element-level denial.

The plugin title in Kecak is **Day-of-Week and Holiday Permission**.
