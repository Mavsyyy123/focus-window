# Focus Window

An Android app that really blocks your chosen apps outside the hours you allow.

Not a website. Not a reminder. When you open a blocked app during a blocked
time, a full screen covers it and sends you home.

---

## What you need

- An Android phone (Android 10 or newer)
- A Windows PC
- Android Studio (free) — https://developer.android.com/studio
- A USB cable

---

## Step 1 — Install Android Studio

Download it, run the installer, accept the defaults. First launch downloads the
Android SDK. This takes a while and needs several GB. Let it finish.

## Step 2 — Open this project

1. Unzip this folder somewhere simple, like `C:\Projects\FocusWindow`
2. Android Studio → **Open** → pick the `FocusWindow` folder (the one with
   `settings.gradle.kts` in it), not a folder inside it
3. Wait for "Gradle sync" to finish at the bottom. First time, it downloads
   Gradle and the libraries. Grab a snack.

If it complains about versions, click the fix it suggests. Android Studio is
usually right about this.

## Step 3 — Turn on Developer Options on your phone

1. Settings → About phone
2. Tap **Build number** seven times
3. Go back → Settings → System → Developer options
4. Turn on **USB debugging**

## Step 4 — Run it

1. Plug the phone into the PC with USB
2. On the phone, tap **Allow** when it asks about USB debugging
3. In Android Studio, pick your phone from the device dropdown at the top
4. Press the green **Run** button

The app installs and opens on your phone.

## Step 5 — Grant the two permissions

The app shows a setup card with two items. Both open Settings directly:

- **Usage access** — lets the app see which app is on screen
- **Display over other apps** — lets the block screen appear on top

Android hides these behind Settings on purpose. Nothing works without them.

## Step 6 — Use it

1. **Apps tab** — tick the apps you want blocked
2. **Schedule tab** — set the hours when those apps ARE allowed. Tap a time to
   change it. "Copy to week" applies that day to all seven.
3. **Today tab** — flip "Enforce my schedule" on

Try opening a blocked app. You should get the block screen.

---

## Making an APK you can share

Android Studio → **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**

The file lands in `app/build/outputs/apk/debug/app-debug.apk`. Copy it to any
phone and open it to install (the phone will ask you to allow installs from
unknown sources).

---

## How it works

| File | Job |
|---|---|
| `Store.kt` | Saves your schedule and app list. Answers "am I allowed right now?" |
| `BlockerService.kt` | Runs in the background, checks the foreground app every second |
| `BlockActivity.kt` | The block screen that covers the app |
| `MainActivity.kt` | The three tabs you interact with |
| `Theme.kt` | Colors and the day ribbon |

The loop is simple: the service asks Android which app is on screen, checks it
against your list and the clock, and launches the block screen if it's a match.

---

## Honest limits

- You can turn it off in two taps, or uninstall it. Any blocker you build for
  yourself is defeatable by you. The point is friction, not a prison.
- The permanent notification is required by Android for background services.
  You can't hide it.
- Some phones (Xiaomi, Oppo, Vivo, Samsung) kill background apps aggressively.
  If blocking stops working, find Battery settings for Focus Window and set it
  to **Unrestricted** / **Don't optimize**.
- Built for personal use. Publishing to Google Play would need a privacy policy
  and extra permission declarations.
