# Cloudys Launcher
**A launcher that stays out of your way, until you need it.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/min%20SDK-26%20(Oreo)-3DDC84?style=flat-square&logo=android&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-27AE60?style=flat-square)
![RAM](https://img.shields.io/badge/RAM-~50%20MB-27AE60?style=flat-square&logo=memory&logoColor=white)
![Latest Release](https://img.shields.io/github/v/release/01satria/LauncherAppv2?style=flat-square&color=27AE60&label=latest)

Your wallpaper shines through. Your apps load fast. Your phone feels like yours again.

---

## Why Cloudys Launcher?

Most launchers are either cluttered with features you'll never use, or so minimal they're useless. Cloudys Launcher sits in the sweet spot, a clean home screen with a quiet set of tools ready when you actually need them.

- **Transparent background** — your wallpaper is the design
- **No icon backgrounds** — app icons render with full transparency, exactly as intended
- **Blazing fast** — lightweight and smooth on any device
- **No bloat** — every feature earns its place

---

## Features

### 🏠 Home Screen
Grid or list layout, toggle app names on/off, hide apps you don't want cluttering your drawer. Long press any app for quick actions like pin, hide, or uninstall. Without ever leaving the home screen.

In **Grid mode**, apps are organized into pages you swipe left and right, just like iOS or One UI. A dot indicator appears above the Brief pill so you always know which page you're on. The number of columns and rows per page is fully customizable from Settings and persists across restarts.

App icons are rendered without any clipping or background. Adaptive icons and transparent icons display exactly as the developer intended, with no white boxes or rounded rectangles imposed on top.

### 📌 Dock
Pin up to 4 favorite apps for instant access. Your avatar lives here too — tap to open the Dashboard, long press for Settings. Dock icons are also rendered without artificial backgrounds.

### 🧩 Home Widgets
Add widgets directly to your home screen to glance at live information without opening anything. Three widgets are available: **Clock** (large time + date display), **Date** (calendar-style day card), and **Battery** (live charge level with a color-coded bar that turns red when low and green while charging).

### 💬 Chat
A lightweight personal assistant that knows your name, greets you by time of day, and keeps your conversation alive throughout the session.

### 🌤️ Weather
Realtime weather for any city in the world. No account, no API key, completely free. Save up to 8 locations and check an hourly forecast for the rest of the day.

### 💱 Currency
Live exchange rates for 14 currencies including USD, EUR, IDR, GBP, JPY, CNY, SGD, AUD, KRW, MYR, THB, INR, SAR, and AED. Convert instantly, anytime.

Rates are fetched by scraping **Google Finance** directly for maximum accuracy and real-time data, with automatic fallback to the fawazahmed0 exchange API if Google Finance is unavailable.

### 📝 To-Do
A minimal task list that remembers everything. Check things off with a satisfying animated tick, smooth canvas drawn checkmark with spring animation. No accounts, no sync, no nonsense.

### ⏳ Countdown
Track what matters — birthdays, deadlines, trips. Color coded so you always know how close you are.

### 🕌 Prayer Times
Full salah schedule for any city in the world. The current prayer is highlighted automatically based on your local time. Save up to 8 cities for quick access, works the same way as the Weather tool. No account, no API key, uses the open [MuslimSalat](https://muslimsalat.com) API.

### 🍅 Pomodoro
A fullscreen focus timer built around the Pomodoro technique. Set your own work duration (not locked to 25 minutes), watch the circular progress arc count down, and let the screen stay on automatically while you focus. An ambient clock is shown throughout so you never lose track of time.

### 🧮 Calculator
A built-in calculator that handles everyday arithmetic with no internet required. Supports addition, subtraction, multiplication, and division with a clean, tap friendly button layout.

### 📐 Unit Converter
Offline conversion across six categories with no network needed: **Length** (km, miles, feet, inches, cm, mm, yards), **Weight** (kg, lbs, g, oz, tonnes), **Temperature** (°C, °F, K), **Speed** (km/h, mph, m/s, knots), **Volume** (liters, gallons, ml, fluid oz, cups, tablespoons), and **Area** (m², km², ft², acres, hectares).

### 💪 Habits
Build streaks that stick. Add daily habits, tap to mark them done, and watch a 🔥 streak counter grow with each consecutive day. The habit checkmark uses the same smooth animated canvas tick as the To-Do list, consistent and satisfying across both tools. The Dashboard shows your progress at a glance with a live completion badge.

### 💰 Money Manager
A full-featured personal budget and expense tracker built right into the launcher. Manage multiple **wallets** (each with its own emoji, color, and currency), log **income**, **expenses**, and **wallet to wallet transfers** across 15 categorized transaction types. Four tabs give you complete control:

- **Overview** — wallet card carousel, this month's spending by category with progress bars, and recent transactions
- **History** — all transactions grouped by date with swipe to delete
- **Analytics** — donut chart breakdown, income vs. expense comparison, daily spending trend, and top categories
- **Wallets** — create, view, and delete wallets with live balance tracking

Supports 8 currencies (IDR, USD, EUR, SGD, MYR, JPY, GBP, AUD) and includes **JSON export/import** for full data backup and restore. All data is stored locally, no accounts, no cloud, no tracking.

---

## Customization & Settings

Long press your avatar in the Dock to open Settings at any time. Everything about your home screen is adjustable:

- **Layout** — switch between Grid and List view
- **Grid size** — when in Grid mode, choose how many columns (3 – 6) and rows (3 – 7) fit on each page using a tap selector; your choice is saved and restored after a restart
- **App names** — show or hide labels under icons
- **Icon size** — slider for both home screen and Dock icons independently
- **Dark / Light mode** — toggle the app theme globally
- **Avatar** — pick any photo from your gallery; it's automatically cropped to a circle
- **Hidden apps** — manage which apps are invisible in the drawer
- **Your name & assistant name** — personalize the Chat greeting

---

## Performance

Cloudys Launcher is built with RAM efficiency as a first-class concern:

- **No icon backgrounds or clipping** — ARGB_8888 bitmaps preserve full alpha transparency
- **LruCache** — icons are cached with automatic eviction when memory is tight (3 MB limit)
- **Pre-cached at load time** — Drawable objects are converted to Bitmap once and immediately released, not held in memory
- **Smart app refresh** — icons only reload if the installed app list actually changes
- **Secondary flows pause when idle** — DataStore flows for tools (todos, habits, weather, etc.) use `WhileSubscribed(5000)` and stop running when the Dashboard is closed
- **derivedStateOf** — badge counts and page numbers only recompute when their inputs change, not on every recompose
- **R8 full mode** — aggressive tree-shaking strips unused code from the release APK

---

## Installation

Download the APK for your device from the [Releases](../../releases) page:

| APK | Device |
|---|---|
| `arm64-v8a.apk` | Most Android phones (2017 and newer) — **pick this if unsure** |
| `armeabi-v7a.apk` | Older 32-bit Android phones |
| `x86_64.apk` | Emulators and ChromeOS |

1. Download the right APK
2. Enable **Install from unknown sources** in your Settings
3. Open the APK and tap Install
4. Set as your **default launcher** when prompted

---

*Made with ❤ by [Satria Bagus](https://github.com/01satria)*
