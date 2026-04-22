# X9 App — Smoke Test Guide

> Run these tests on a **physical Android device** or a fully configured emulator with Google Play Services installed.
> For geofence and speech tests, a physical device is strongly preferred.
> Complete tests in order — later sections depend on state set up in earlier ones.

---

## Prerequisites

| Requirement | Detail |
|-------------|--------|
| Device | Android 9 (API 28) or higher |
| Google app | Installed and signed in (required for SpeechRecognizer) |
| Internet | Active Wi-Fi or mobile data |
| APK | `app-debug.apk` built via `./gradlew :app:assembleDebug` |
| Firebase | Project configured and `google-services.json` present |
| Test account | A Google account or email/password to register with |
| Location | GPS enabled on device; for geofence tests, Extended Controls if using emulator |

Install the APK fresh (uninstall any previous build first to clear SharedPreferences and permission state):
```
adb uninstall dk.itu.moapd.x9.s25134
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Section 1: Permission Onboarding

These dialogs appear automatically on first launch. Test that each step fires in the correct sequence.

### 1.1 Notification permission (Android 13+ only)

**Setup:** Fresh install on API 33+ device. Revoke all permissions if re-testing.

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Launch the app | "Stay ahead of traffic hazards" rationale dialog appears |
| 2 | Tap **Not now** | Dialog closes; Fine Location rationale appears next |
| 3 | Relaunch the app | Notification rationale appears again (flow restarts on each launch until granted) |
| 4 | Tap **Continue** | System notification permission dialog appears |
| 5 | Tap **Allow** | System dialog closes; Fine Location rationale appears |

### 1.2 Fine location permission

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | From the fine location rationale dialog, tap **Continue** | System location dialog appears with Precise/Approximate options |
| 2 | Tap **While using the app** (Precise) | Dialog closes; Background location rationale appears |
| 3 | Tap **Not now** on background rationale | Flow completes; geofences do NOT register |
| 4 | Relaunch the app | Fine location step is skipped (already granted); background location rationale appears |

### 1.3 Background location permission (API 29+)

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | From the background location rationale dialog, tap **Continue** | System Settings screen opens ("Allow all the time" option visible) |
| 2 | Select **Allow all the time** | Return to app; geofences register (check Logcat: `GeofenceRepository` tag) |

### 1.4 Onboarding complete

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Grant all three permissions in sequence | No rationale dialogs appear on subsequent launches |
| 2 | Relaunch the app | App goes straight to Home screen with no permission prompts |

---

## Section 2: Authentication

### 2.1 Guest mode

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Launch the app | Login screen is shown |
| 2 | Tap **Continue as guest** | Navigated to Home screen; user is not signed in |
| 3 | Tap the **+** button in the bottom bar | "Add Report" choice sheet appears |
| 4 | Choose **Type Manually** | Snackbar: "Sign in to create reports" — form does NOT open |

### 2.2 Email registration

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | From Login screen, tap **Register** tab | Registration form appears |
| 2 | Fill in Display Name, Email, Password, Confirm Password | Fields accept input |
| 3 | Leave Password blank, tap **Register** | Validation prevents submission |
| 4 | Enter mismatched passwords, tap **Register** | Error: "Passwords do not match" |
| 5 | Fill all fields correctly, tap **Register** | Account created; navigated to Home screen |
| 6 | Verify display name shows in Profile screen | Name matches what was entered |

### 2.3 Email sign-in

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Sign out from Profile, return to Login screen | Login screen shows |
| 2 | Enter wrong password, tap **Sign In** | Error snackbar: authentication failed message |
| 3 | Enter correct credentials, tap **Sign In** | Navigated to Home screen |

### 2.4 Google sign-in

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | From Login screen, tap **Sign in with Google** | Google account picker appears |
| 2 | Select a Google account | Sign-in completes; navigated to Home screen |
| 3 | Check Profile screen | Google account display name and photo shown |

### 2.5 Sign out

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Navigate to Profile screen | Profile screen shows |
| 2 | Tap **Sign Out** | Navigated to Home screen; user is now guest |
| 3 | Tap **+** to add a report | Snackbar: "Sign in to create reports" |

---

## Section 3: Dashboard (Home Screen)

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Sign in and navigate to Home | Dashboard shows stat cards: Active Reports, Critical Alerts, Your Reports, Resolved Today |
| 2 | Verify stat counts are non-negative integers | No crashes or NaN values |
| 3 | Tap **View all →** in Recent Reports | Navigates to Report List screen |
| 4 | Press back | Returns to Home screen |
| 5 | Tap **New Report** quick action | Report form opens (if signed in) |
| 6 | Press back | Returns to Home screen |
| 7 | Tap **View Map** quick action | Map screen opens |
| 8 | Press back | Returns to Home screen |

---

## Section 4: Report Creation (Manual)

**Prerequisites:** Signed in. Location permission granted. At least one test report will be created here — keep it for use in later sections.

### 4.1 Opening the form

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Tap the **+** button in the bottom bar | "Add Report" choice sheet slides up with "Use Voice" and "Type Manually" options |
| 2 | Tap **Type Manually** | Report form opens; GPS location loads automatically |
| 3 | Verify the map preview shows your current location | Map visible with a pin; address string shown below map |

### 4.2 Filling the form

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Tap the **Type** dropdown | Options: Speed Camera, Heavy Traffic, Accident, Road Work |
| 2 | Select **Accident** | Dropdown closes; "Accident" shown in field |
| 3 | Drag the severity slider to **HIGH (4)** | Slider moves; label updates to "High (4/5)" in orange |
| 4 | Tap the description field and type "Test accident report" | Text appears in field |
| 5 | Tap **Camera** in the photo section | Camera opens (grant permission if prompted) |
| 6 | Take a photo | Photo preview appears below the photo buttons |
| 7 | Tap **Remove photo** | Photo preview disappears |

### 4.3 Discard dialog

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | With description text entered, tap the back arrow | "Discard Report?" dialog appears |
| 2 | Tap **Keep Editing** | Dialog closes; form state preserved |
| 3 | Clear the description field completely | Field is blank |
| 4 | Tap back again | No discard dialog — navigates back directly (no meaningful input) |
| 5 | Re-open the form, add ONLY a photo (no description) | Photo preview shown |
| 6 | Tap back | Discard dialog appears (photo counts as meaningful input) |
| 7 | Tap **Discard** | Navigates back; form state cleared |

### 4.4 Submitting

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Fill in Type = Accident, Severity = High, Description = "Smoke test report 1" | Form filled |
| 2 | Tap **Submit Report** | Button shows loading spinner briefly, then navigates back to previous screen |
| 3 | Navigate to Report List | New report "Smoke test report 1" visible |

---

## Section 5: Report List

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Navigate to Reports via bottom bar | Report list shows all reports |
| 2 | Tap **My Reports** filter toggle | List filters to only show reports created by the current user |
| 3 | Tap **My Reports** again to deselect | All reports shown |
| 4 | Tap **Critical Only** filter | List shows only CRITICAL severity reports |
| 5 | Tap **Critical Only** again to deselect | All reports shown |
| 6 | Type in the search bar | List filters in real time as you type |
| 7 | Clear the search bar | Full list restores |
| 8 | Scroll down if many reports exist | Smooth scrolling with no crashes |

---

## Section 6: Report Detail, Edit, and Delete

**Prerequisites:** At least one report created by the current user exists.

### 6.1 Viewing a report

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Tap any report in the list | Detail screen opens |
| 2 | Verify type, severity badge, description, location, and timestamp are shown | All fields visible |
| 3 | If the report has an image, verify it loads | Image visible (may take a moment on slow connection) |

### 6.2 Editing a report (owner only)

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Open the detail for a report you own | Edit and Delete buttons visible |
| 2 | Tap **Edit** | Report form opens pre-filled with existing values |
| 3 | Change the description to "Updated smoke test" | Description field updated |
| 4 | Tap **Submit Report** | Navigates back to detail screen; updated description shown |
| 5 | Open a report owned by another user | Edit and Delete buttons NOT visible |

### 6.3 Deleting a report (owner only)

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Open the detail for a report you own | Delete button visible |
| 2 | Tap **Delete** | Confirmation dialog: "Delete Report?" |
| 3 | Tap **Cancel** | Dialog closes; report is NOT deleted |
| 4 | Tap **Delete** again, then confirm | Report deleted; navigated back to list; report no longer appears |

---

## Section 7: Map Screen

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Tap the **Map** tab in the bottom bar | Map screen opens centered on the user's location |
| 2 | Verify report markers appear on the map | Colored pins visible at report locations |
| 3 | Tap a single report marker | Info card slides up from bottom with report details |
| 4 | Tap **View Full Report** in the info card | Navigates to Report Detail screen |
| 5 | Press back | Returns to Map screen |
| 6 | Zoom out until multiple nearby markers are visible | Markers cluster into a red count bubble |
| 7 | Tap the cluster bubble | Bottom sheet shows a scrollable list of reports in the cluster |
| 8 | Scroll through the cluster list | Each report shows type, severity badge, and distance |
| 9 | Tap the **Layers** button (top-right) | Map type picker appears: Normal, Satellite, Hybrid, Terrain |
| 10 | Select **Satellite** | Map switches to satellite imagery |
| 11 | Select **Normal** | Map returns to standard view |

---

## Section 8: Profile Screen

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Tap the **Profile** tab | Profile screen shows display name, email |
| 2 | Toggle **Dark Mode** on | App switches to dark theme immediately |
| 3 | Toggle **Dark Mode** off | App switches to light theme |
| 4 | Kill and relaunch the app | Dark mode preference persists (whichever was last set) |
| 5 | Tap **Sign Out** | Navigated to Home as guest; Profile shows sign-in prompt |

---

## Section 9: Geo-Proximity Notifications

**Prerequisites:** All three location permissions granted. Active reports with HIGH or CRITICAL severity exist in Firebase with coordinates within ~2 km of your test location.

**Emulator setup:** Use **Extended Controls → Location** to set the emulator's coordinates near a known test report.

### 9.1 Notification triggers by severity

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Create a CRITICAL report at your current GPS coordinates | Report saved to Firebase |
| 2 | Move the device (or emulator) at least 600 m away, then back within 500 m | Notification appears: "Nearby Accident CRITICAL" (or the appropriate type) |
| 3 | Create a HIGH severity report at the same location | Notification triggers when entering the 500 m radius |
| 4 | Create a MODERATE severity report at the same location | **No notification** should appear — MODERATE is below the alert threshold |
| 5 | Create a LOW severity report | **No notification** |
| 6 | Create a MINOR severity report | **No notification** |

### 9.2 Notification ordering

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Have at least one CRITICAL and one HIGH report within 500 m | Both notifications should be posted |
| 2 | Pull down the notification shade | CRITICAL notification appears above HIGH notification |

### 9.3 Cooldown (duplicate suppression)

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Trigger a notification for a specific report | Notification appears |
| 2 | Move out and back into the 500 m radius within 1 hour | **No second notification** — cooldown suppresses it |
| 3 | Wait 1 hour (or clear SharedPreferences via `adb shell pm clear dk.itu.moapd.x9.s25134`) | Notification fires again on next ENTER event |

### 9.4 Geofence re-registration after reboot

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | With HIGH/CRITICAL reports in Firebase and all permissions granted, reboot the device | Device reboots |
| 2 | Do NOT open the app | Wait 30 seconds after boot |
| 3 | Check Logcat (filter tag: `BootReceiver`) | Log entry shows geofences re-registered |
| 4 | Move the device into a report's 500 m radius | Notification fires without needing to open the app first |

---

## Section 10: Speech-Enabled Report Creation

**Prerequisites:** Signed in. Google app installed (provides the speech recognition service). Microphone available.

### 10.1 Microphone permission request

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Tap the **+** button in the bottom bar | "Add Report" choice sheet appears with "Use Voice" and "Type Manually" |
| 2 | Tap **Use Voice** (first time only) | System microphone permission dialog appears |
| 3 | Tap **Deny** | Permission denied; snackbar: "Microphone permission is required to use voice input" |
| 4 | Tap **+** → **Use Voice** again | Permission dialog appears again |
| 5 | Tap **Allow** | Speech overlay slides up with pulsing mic icon and "Listening…" text |

### 10.2 Overlay states

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Open the speech overlay (mic permission already granted) | Pulsing mic icon + "Listening…" label visible |
| 2 | Stay silent for ~8 seconds | Overlay changes to "Couldn't understand, please try again." with **Retry** button |
| 3 | Tap **Retry** | Overlay returns to Listening state (pulsing mic) |
| 4 | Tap outside the overlay (on the scrim) | Overlay dismisses; returns to current screen |
| 5 | Speak clearly but say random unrecognised words (e.g. "banana sunshine") | "Couldn't understand" state shown with Retry button |

### 10.3 Type keyword routing

Speak each utterance clearly after the overlay shows "Listening…":

| Utterance | Expected type pre-filled in form |
|-----------|----------------------------------|
| *"accident"* | Accident |
| *"there was a crash on the motorway"* | Accident |
| *"collision reported"* | Accident |
| *"heavy traffic"* | Heavy Traffic |
| *"bad congestion ahead"* | Heavy Traffic |
| *"traffic jam on the ring road"* | Heavy Traffic |
| *"speed camera"* | Speed Camera |
| *"camera spotted"* | Speed Camera |
| *"road work blocking the lane"* | Road Work |
| *"road works ahead"* | Road Work |
| *"construction zone"* | Road Work |
| *"works on the motorway"* | Road Work |

For each: after speaking, the overlay should dismiss and the Report Form should open with the correct type pre-selected in the dropdown.

### 10.4 Severity keyword routing

| Utterance | Expected severity pre-filled |
|-----------|------------------------------|
| *"accident minor"* | MINOR (slider at 1) |
| *"traffic low"* | LOW (slider at 2) |
| *"crash moderate"* | MODERATE (slider at 3) |
| *"accident medium"* | MODERATE (slider at 3) |
| *"accident high"* | HIGH (slider at 4) |
| *"crash serious"* | HIGH (slider at 4) |
| *"accident critical"* | CRITICAL (slider at 5) |
| *"collision severe"* | CRITICAL (slider at 5) |
| *"accident danger"* | CRITICAL (slider at 5) |

### 10.5 Combined type + severity

| Utterance | Expected type | Expected severity |
|-----------|---------------|-------------------|
| *"accident high severity"* | Accident | HIGH |
| *"speed camera critical"* | Speed Camera | CRITICAL |
| *"road work moderate"* | Road Work | MODERATE |
| *"heavy traffic low"* | Heavy Traffic | LOW |

### 10.6 Leftmost keyword wins (ambiguous utterances)

| Utterance | Expected behaviour |
|-----------|-------------------|
| *"accident and heavy traffic"* | Type = Accident (appears first in utterance) |
| *"spotted a speed camera"* | Type = Speed Camera (multi-word "speed camera" matched before single-word "camera") |
| *"road works on the highway"* | Type = Road Work |

### 10.7 Bottom bar voice flow (navigates to form)

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | From Home screen (not on the form), tap **+** → **Use Voice** | Overlay opens |
| 2 | Speak *"accident high"* | Overlay dismisses; Report Form opens with Accident + HIGH pre-filled; description is blank |
| 3 | Verify form is a new report (no existing data) | `reportId` is null; form shows "New Report" title |
| 4 | Add a description and submit | Report saves successfully |

### 10.8 In-form mic FAB (updates form in place)

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Open the report form via **Type Manually** | Form opens; mic FAB visible in bottom-right corner |
| 2 | Select "Heavy Traffic" and set severity to LOW manually | Dropdown and slider updated |
| 3 | Tap the mic FAB | Overlay opens |
| 4 | Speak *"road work critical"* | Overlay dismisses; form stays open; type updates to Road Work; severity updates to CRITICAL |
| 5 | Verify form did NOT navigate away | Still on the Report Form screen |

### 10.9 Speech unavailable (graceful degradation)

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | On a device where `SpeechRecognizer.isRecognitionAvailable()` returns false (no Google app), tap **+** | Choice sheet only shows **Type Manually** — no "Use Voice" option |
| 2 | Open the report form via Type Manually | Mic FAB is NOT shown in the form |

---

## Section 11: Edge Cases and Error Handling

### 11.1 No internet connection

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Enable airplane mode | Wi-Fi and mobile data disabled |
| 2 | Open the app | App opens; cached reports visible (Firebase offline persistence) |
| 3 | Attempt to submit a new report | Report queued; syncs automatically when connectivity returns |
| 4 | Disable airplane mode | Previously queued report appears in the list |

### 11.2 Location unavailable

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Deny location permission (or move to a location with no GPS fix) | Report form shows "Your location could not be determined" message in the map area |
| 2 | Attempt to submit | Submit button is disabled until location is obtained |

### 11.3 Editing someone else's report

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Sign in as User A, create a report | Report saved |
| 2 | Sign out, sign in as User B | Different account |
| 3 | Open User A's report | Edit and Delete buttons NOT visible |

### 11.4 Image upload failure

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Attach an image to a new report | Photo preview shown |
| 2 | Disconnect internet just before tapping Submit | Airplane mode on |
| 3 | Tap **Submit Report** | Snackbar: "Image upload failed. Report saved without photo." Report still saves |

---

## Logcat Tags Reference

Use these filter tags in Android Studio's Logcat to trace execution during testing:

| Tag | What it logs |
|-----|-------------|
| `MainActivity` | Permission step transitions, `onCreate` |
| `ReportFormViewModel` | Location load, submission start/success/failure |
| `ReportRepository` | Firebase read/write events |
| `GeofenceRepository` | Geofence add/remove, sync results |
| `BootReceiver` | Geofence re-registration after reboot |
| `ProximityNotificationReceiver` | ENTER events, cooldown checks, notification posting |
| `ReportFormScreen` | Camera/gallery errors |
