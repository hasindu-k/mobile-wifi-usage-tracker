# Architecture

Data Usage Monitor is a small Android application organized around a UI layer,
a repository layer, and a local Room database. The main data flow is:

```text
Fragments / Worker
        |
        +--> DataUsageRepository --> Android NetworkStatsManager
        |
        +--> AppDatabase --> UsageLimitDao --> Room / SQLite
        |
        +--> AppUsageAdapter --> RecyclerView item views
```

## Layers

### UI layer

The fragments and `MainActivity` display usage information and receive user
actions:

- `DashboardFragment` displays totals and hourly usage.
- `AppsFragment` displays application-level usage.
- `LimitsFragment` displays and edits monthly limits.
- `PermissionFragment` handles the Usage Access permission flow.
- `MainActivity` hosts the application navigation.

The UI layer should format values for display with `ByteFormatter` and should
not contain the logic for querying or aggregating network statistics.

### Adapter

`fragments/AppUsageAdapter.kt` is the RecyclerView adapter for the app usage
list. It converts each `AppUsageInfo` model into an `item_app_usage.xml` row,
including the app name, formatted usage, network label, progress indicator, and
icon view.

The adapter is a presentation component. It does not query the database or
`NetworkStatsManager`; callers provide its data through `updateData`.

### Repository

`DataUsageRepository.kt` is the access point for network usage data. It wraps
Android's `NetworkStatsManager` and provides application-facing operations for:

- Device Wi-Fi and mobile usage for today or the current month.
- Hotspot/tethering usage.
- Per-application usage grouped by package name.
- Hourly usage used to identify peak periods.

The repository also maps UIDs to package names and application labels using
`PackageManager`. It owns the time-window calculations (start of day, start of
month, and start of hour), so consumers do not need to duplicate that logic.

Network statistics failures are currently handled by returning `0L` for device
totals or an empty list for list-based queries.

### Database (DB)

`db/AppDatabase.kt` defines the Room database named `data_usage_db`. It is a
singleton database instance created with the application context. The database
currently contains one entity:

- `UsageLimit` — a monthly limit for a network type, including the byte limit,
  warning percentage, and enabled state.

Room persists this data in SQLite and supplies coroutine-compatible database
operations through the DAO.

### DAO

`db/UsageLimitDao.kt` is the database access object for `UsageLimit`. It exposes:

- `getAllLimits()` as a `Flow<List<UsageLimit>>` for observing changes.
- `getLimitForNetwork()` for retrieving one Wi-Fi or mobile limit.
- `insertOrUpdateLimit()` for saving a limit.
- `setEnabled()` for enabling or disabling a limit.

SQL details remain inside the DAO. Callers use these methods rather than
embedding SQL statements in UI or worker code.

## Runtime flows

### Reading usage

1. A fragment requests usage from `DataUsageRepository`.
2. The repository queries `NetworkStatsManager` for the requested network type
   and time range.
3. The repository aggregates received and transmitted bytes into domain models
   such as `AppUsageInfo` and `HourlyUsageInfo`.
4. The fragment updates summary views or passes app data to
   `AppUsageAdapter`.

### Saving and displaying limits

1. The user enters a monthly limit in `LimitsFragment`.
2. The fragment creates a `UsageLimit` and saves it through `UsageLimitDao`.
3. `getAllLimits()` emits the updated list through a Room `Flow`.
4. `LimitsFragment` recalculates progress and updates the limit UI using the
   latest repository usage values.

### Background monitoring

`monitoring/UsageMonitorWorker.kt` runs the periodic background check. It reads
Wi-Fi and mobile limits through `UsageLimitDao`, reads the corresponding
monthly usage through `DataUsageRepository`, and logs a warning or alert when a
configured threshold is reached. WorkManager controls the worker lifecycle.

## Ownership rules

- The database owns persistent user-configured limits.
- The DAO owns Room queries and writes.
- The repository owns network-statistics access and aggregation.
- Fragments own screen state and user interaction.
- The adapter owns only RecyclerView row binding.
- Models (`UsageLimit`, `AppUsageInfo`, and `HourlyUsageInfo`) carry data between
  these components.

There is currently no separate network-data-source adapter. If another source
of usage data is added later, it should be isolated behind an interface or
adapter and consumed by `DataUsageRepository`, keeping Android API details out
of the UI layer.

