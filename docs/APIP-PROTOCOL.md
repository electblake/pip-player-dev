# APiP Remote protocol evidence

Client: `com.asharelink.apip_companion`, installed from Google Play in the Android 14 phone emulator. Server: this project's TV player in the Google TV API 36 emulator. No remote application was built or modified.

## Observed requests

The original app polls `GET /config` to check server availability and reads it when selecting a server or pressing Sync. A response containing the following fields correctly populated the original remote's URL, selected position and sliders:

```json
{"streamUrl":"http://10.0.2.2:8765/review-test.mp4","position":"bottom-center","size":42,"volume":26}
```

Selecting a source and changing position, size or volume sends all four values together:

```http
POST /config
Content-Type: application/x-www-form-urlencoded

streamUrl=http%3A%2F%2F10.0.2.2%3A8765%2Freview-test.mp4&position=top-right&size=43&volume=79
```

The bottom action buttons send:

```http
POST /action
Content-Type: application/x-www-form-urlencoded

action=fullscreen
```

Other captured values for `action`: `pip`, `close_app` (after confirming the remote's Close dialog).

The remote accepts a server address containing an explicit port. This player uses port 8080; the app's prefilled sample port does not need to match it.

## Scope

The Android companion is the protocol reference used here. iOS interoperability must still be confirmed on an iPhone; no claim is made that inspecting the Android client proves identical behavior across all iOS versions.

The separate Media, Clock and Timer controls are not part of this minimal video player. Media mode was observed sending `streamUrl=media`; it is not a video URL. Do not select these tools with this build.

## Local integration verification

Tests use the unmodified installed remote, with manual server address `10.0.2.2:18081`, forwarded by ADB to TV port 8080. A generated HTTP test video is served by the PC at port 8765. These addresses are test-environment settings, not hardcoded in the APK.

- Source selection from the remote started video playback in the TV overlay.
- All six remote position buttons produced the matching `/config` position.
- Remote slider gestures produced size 25 and volume 69 in the running TV server.
- Fullscreen produced a 1920×1080 overlay.
- PiP restored a 576×324 bottom-right overlay at size 30 after fullscreen.
- The final build displayed opaque video in fullscreen.
- Close App removed the activity, overlay and player service; reopening the app allowed the server to start again.

Build and Android lint are run by `scripts/build.ps1`. Device checks and further verification are recorded in the task's tool results.
