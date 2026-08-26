# Getting an APK without installing anything

GitHub will build the app for you on their computers, for free. You only need
a browser. Takes about 10 minutes the first time.

## 1. Make a GitHub account

Go to github.com and sign up. Free.

## 2. Make a new repository

- Click the **+** in the top right → **New repository**
- Name it `focus-window`
- Leave it Public (private also works, free)
- Click **Create repository**

## 3. Upload the project files

- On the empty repo page, click **uploading an existing file**
- Unzip FocusWindow.zip on your computer first
- Drag the **contents** of the FocusWindow folder into the browser
  (the `app` folder, `.github` folder, `settings.gradle.kts`, `build.gradle.kts`,
  `gradle.properties`, `gradle` folder)
- IMPORTANT: drag what's INSIDE the FocusWindow folder, not the folder itself
- Click **Commit changes**

## 4. Wait for the build

- Click the **Actions** tab at the top
- You'll see a run called "Build APK" with a yellow dot (working) that turns
  into a green check (done) or a red X (failed)
- Takes about 3-5 minutes

## 5. Download the APK

- Click the finished run
- Scroll to the bottom, find **Artifacts** → `focus-window-apk`
- Click it to download. You get a .zip containing `app-debug.apk`
- You can do this step on your phone's browser directly

## 6. Install on your phone

- Unzip the download (the Files app can do this)
- Tap `app-debug.apk`
- Android will warn you about installing from an unknown source. Allow it for
  your browser or Files app, then install
- The warning is normal. It appears for every app not from the Play Store

## If the build fails (red X)

Click the failed run, click the "Build the APK" step, and copy the red error
text. Send it to me and I'll fix the code. First builds failing on version
mismatches is completely normal.
