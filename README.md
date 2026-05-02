# Data Usage Monitor Android App

A native Android application built with Kotlin to monitor Wi-Fi and mobile data usage on an Android device.

The app shows daily and monthly network usage, app-wise data usage, and the time period when the user consumed the most data.

---

## Features

- View today's total Wi-Fi usage
- View monthly total Wi-Fi usage
- View today's total mobile data usage
- View monthly total mobile data usage
- View app-wise Wi-Fi usage
- View app-wise mobile data usage
- Display top data-consuming applications
- Show peak Wi-Fi usage time for today
- Show peak mobile data usage time for today
- Simple native Android UI

---

## Tech Stack

- Kotlin
- Android SDK
- NetworkStatsManager
- AppOpsManager
- Android Activity-based UI

---

## Minimum Requirements

- Android Studio
- Kotlin
- Android device with Android 6.0 or higher
- Minimum SDK: 23
- Recommended testing device: Real Android phone

> Note: The app may not work correctly on some emulators because network usage statistics are better tested on a real device.

---

## Required Permissions

The app uses the following Android permissions:

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

### Why these permissions are needed

`PACKAGE_USAGE_STATS` is required to access network usage statistics.

`QUERY_ALL_PACKAGES` is used to map application UIDs to package names and display readable app names.

> Note: `PACKAGE_USAGE_STATS` is a special permission. The user must manually enable Usage Access permission from Android settings.

---

## How to Enable Usage Access Permission

After installing the app:

1. Open the app
2. Tap **Open Usage Access Settings**
3. Find **Data Usage Monitor**
4. Enable usage access
5. Go back to the app
6. Tap **Load Data Usage**

---

## Project Structure

```text
DataUsageMonitor/
│
├── app/
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           │
│           ├── java/com/example/datausagemonitor/
│           │   ├── MainActivity.kt
│           │   ├── DataUsageRepository.kt
│           │   ├── AppUsageInfo.kt
│           │   ├── HourlyUsageInfo.kt
│           │   ├── UsagePermissionHelper.kt
│           │   └── ByteFormatter.kt
│           │
│           └── res/
│               └── values/
│                   └── styles.xml
│
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Main Components

### MainActivity.kt

Handles the user interface and displays:

- Wi-Fi usage
- Mobile data usage
- App-wise usage
- Peak usage time

### DataUsageRepository.kt

Handles all data usage calculations using Android `NetworkStatsManager`.

It calculates:

- Today's Wi-Fi usage
- Monthly Wi-Fi usage
- Today's mobile data usage
- Monthly mobile data usage
- App-wise Wi-Fi usage
- App-wise mobile data usage
- Hourly usage for peak usage detection

### UsagePermissionHelper.kt

Checks whether Usage Access permission is enabled and opens the Usage Access Settings screen.

### AppUsageInfo.kt

Data model for app-wise usage information.

### HourlyUsageInfo.kt

Data model for hourly network usage information.

### ByteFormatter.kt

Formats raw byte values into readable units such as KB, MB, and GB.

---

## How the App Works

The app uses Android's `NetworkStatsManager` to collect network usage data.

For Wi-Fi usage, the app uses:

```kotlin
ConnectivityManager.TYPE_WIFI
```

For mobile data usage, the app uses:

```kotlin
ConnectivityManager.TYPE_MOBILE
```

The app checks network usage from:

```text
Today 00:00 AM → Current Time
```

and:

```text
First day of current month 00:00 AM → Current Time
```

For peak usage time, the app groups today's data usage into hourly intervals and finds the hour with the highest total usage.

---

## Example Output

```text
Today's Wi-Fi Usage: 1.25 GB
Monthly Wi-Fi Usage: 42.80 GB

Today's Mobile Data Usage: 350.20 MB
Monthly Mobile Data Usage: 8.40 GB

Peak Wi-Fi Usage Time Today:
08:00 PM - 09:00 PM, Used: 950.25 MB

Peak Mobile Data Usage Time Today:
02:00 PM - 03:00 PM, Used: 120.60 MB

Top Wi-Fi Apps Today:
1. YouTube - 650.30 MB
2. Chrome - 210.50 MB
3. WhatsApp - 95.10 MB

Top Mobile Data Apps Today:
1. Instagram - 420.60 MB
2. TikTok - 300.00 MB
3. Chrome - 85.70 MB
```

---

## Limitations

- Mobile data usage may show `0 B` on some devices due to Android manufacturer restrictions.
- Some Android versions restrict access to mobile network statistics.
- Usage statistics may not be available immediately after installing the app.
- The app does not monitor data usage in real time.
- The app reads historical usage statistics provided by Android.
- Results may vary depending on device brand, Android version, and system privacy settings.
- App-wise usage depends on whether Android can map UID values to installed applications.

---

## Testing Notes

For best results:

- Test on a real Android device
- Enable Usage Access permission
- Use Wi-Fi and mobile data for a while before testing
- Restart the app after enabling permission
- Compare results with Android system data usage settings

---

## Future Improvements

- Add charts for hourly usage
- Add daily usage history
- Add weekly and monthly graphs
- Add custom usage limit alerts
- Add background usage tracking
- Add notifications when usage exceeds a limit
- Add Wi-Fi vs mobile comparison chart
- Add dark mode
- Improve UI using Jetpack Compose
- Store historical usage data locally using Room database

---

## App Scope

This app focuses only on device data usage monitoring.

It does not include website URL data usage testing.

Current scope:

```text
Kotlin Android App for Data Usage Monitoring

1. Show today's Wi-Fi usage
2. Show monthly Wi-Fi usage
3. Show today's mobile data usage
4. Show monthly mobile data usage
5. Show app-wise Wi-Fi usage
6. Show app-wise mobile data usage
7. Show top data-consuming apps
8. Show peak data usage time
```

---

## License

This project is created for learning and academic purposes.
