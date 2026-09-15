# PiP Player Dev

Google TV / Android TV 10+ player: HTTP video input, floating playback, six positions, 15–80% window width, independent volume and a local HTTP control server.

This project contains only the TV player/server. Its core HTTP protocol was verified using the existing [APiP Android Remote](https://play.google.com/store/apps/details?id=com.asharelink.apip_companion), installed directly from Google Play. The intended iPhone client is the existing [APiP Remote](https://apps.apple.com/us/app/apip-remote-client/id6757357196); a physical iOS test is still pending. See [protocol evidence](docs/APIP-PROTOCOL.md).

Supported remote controls: stream selection, six positions, size, independent volume, fullscreen, PiP and Close App. The remote's separate Media, Clock and Timer tools are outside this minimal video player's scope.

## Build

Requirements: PowerShell 7.4+, JDK 21, Android SDK platform 36, build tools 36.0.0 and platform tools. Set `JAVA_HOME` and `ANDROID_HOME` to their installation directories. Accept the Android SDK licences with `sdkmanager --licenses`.

```powershell
pwsh -File ./scripts/build.ps1
```

Outputs:

- `dist/pip-player-dev.apk` — installable APK signed with this workstation's Android development key.
- `dist/pip-player-dev.apk.sha256` — SHA-256 checksum.

The script builds and runs Android lint before copying the APK. Gradle downloads its pinned build dependencies. Keep the same development key (`$HOME/.android/debug.keystore`) for subsequent updates. This is a development build for review.

## Install with Downloader by AFTVnews

1. Upload `dist/pip-player-dev.apk` to your file host and obtain a direct public HTTPS download URL. The address must download the APK without a sign-in page.
2. Install [Downloader by AFTVnews](https://www.aftvnews.com/downloader/) from Google Play on the Google TV.
3. Open Downloader, enter your APK download URL and select **Go**.
4. When Android requests permission to install apps from this source, open **Settings**, enable **Install unknown apps** for Downloader, then return to installation. On Google TV devices that hide this setting, enable Developer options by selecting **Settings → System → About → Android TV OS build** seven times first.
5. Select **Install**, then **Open**. The launcher name is **PiP Player Dev**.
6. Select **Allow display over other apps** and grant the permission in Android settings. Return to the player.
7. Select **Start player** to start the HTTP server. You may leave the stream URL empty and choose a video from APiP Remote. In the remote, open **Server Manager → Manual**, enter the TV's displayed `IP:8080`, add and select that server, then choose a source from **Open Stream Library**. Both devices must be on the same LAN. No discovery is required.
8. Press **Home** on the TV; the stream remains visible. Use the remote to change position, size or volume, or switch between Fullscreen and PiP. **Close App** stops the server and player. Reopen the TV app and select **Start player** to connect again.

For a numeric Downloader code, submit your hosted APK URL to the [AFTVnews URL Shortener](https://go.aftvnews.com/). Enter the resulting code in Downloader. **AFTVnews shortens URLs; it does not host your APK.** No APK host or Downloader code has been configured for this project.

Installation flow reference: [SmartTube installation](https://github.com/yuliskov/SmartTube#installation). Use this player's own APK URL/code, not SmartTube's.

## Install and launch for development

Enable debugging on the TV and authorize this computer. Connect using the TV's displayed debugging address:

```powershell
& "$env:ANDROID_HOME/platform-tools/adb.exe" connect 'TV_IP:DEBUG_PORT'
& "$env:ANDROID_HOME/platform-tools/adb.exe" devices -l
pwsh -File ./scripts/install-dev.ps1 -Serial 'TV_IP:DEBUG_PORT'
```

If the TV uses wireless debugging pairing, pair first with `adb pair TV_IP:PAIRING_PORT` and enter the code displayed on the TV. Pairing and connection ports are separate. Use the actual serial from `adb devices -l` in the installation script.

### Local Google TV emulator

This workstation has the `PiP_GoogleTV_API36` virtual device, using Google's API 36 x86_64 TV image and Windows Hypervisor Platform. Its SDK is installed separately at `$env:LOCALAPPDATA/Android/PiP-SDK`.

With the emulator stopped, open its review window using:

```powershell
$pipSdk = "$env:LOCALAPPDATA/Android/PiP-SDK"
Start-Process -FilePath "$pipSdk/emulator/emulator.exe" -ArgumentList '-avd', 'PiP_GoogleTV_API36', '-sysdir', "$pipSdk/system-images/android-36/google-tv/x86_64", '-port', '5554', '-gpu', 'host', '-no-snapshot', '-no-boot-anim'
```

After the TV finishes booting:

```powershell
pwsh -File ./scripts/install-dev.ps1 -Serial emulator-5554
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s emulator-5554 forward tcp:18080 tcp:8080
```

The existing installation script builds neither an emulator nor a remote app; it installs the APK and opens the player. Use keyboard arrows and Enter as the TV remote. Press Escape/Back to dismiss the on-screen keyboard before moving to Start player.

From the emulator, `10.0.2.2` addresses this PC. From this PC, the forwarded player API is `http://127.0.0.1:18080`. The emulator's displayed `10.0.2.15` address is not a LAN address for an iPhone.

For the existing Android companion running in a second emulator, enter `10.0.2.2:18080` in its manual server dialog. The APK does not include a companion app. Physical TV behavior and APiP iOS Remote interoperability remain unverified.

## HTTP controls

The player displays its local IPv4 address. The HTTP server listens on port **8080** while the player service runs. Select **Start player** on the TV before connecting the remote; playback is not required. Requests use manually entered addresses, without discovery. This development API has no authentication; use it on your trusted LAN.

POST bodies use `application/x-www-form-urlencoded`.

| Method | Path | Body | Result |
| --- | --- | --- | --- |
| GET | `/config` | — | JSON: `streamUrl`, `size`, `volume`, `position` |
| POST | `/config` | All four fields: `streamUrl`, `position`, `size`, `volume` | Apply stream and layout/audio settings |
| POST | `/action` | `action=fullscreen` | Expand the video to the TV screen |
| POST | `/action` | `action=pip` | Restore configured PiP size and position |
| POST | `/action` | `action=close_app` | Stop playback, close the overlay and server |

Position values: `top-left`, `top-center`, `top-right`, `bottom-left`, `bottom-center`, `bottom-right`. Size is an integer screen-width percentage; volume is an integer percentage from 0 to 100. GET responses use JSON numbers for size and volume. POST requests return `OK`; stream loading continues asynchronously. Updating settings with the same stream URL preserves playback. An empty stream URL clears playback while keeping the server available. Supply valid values; there is no application retry or recovery layer.

```powershell
Invoke-RestMethod -Method Post -Uri 'http://TV_IP:8080/config' -Body @{ streamUrl = 'https://YOUR_HOST/video.m3u8'; position = 'top-left'; size = 40; volume = 25 }
Invoke-RestMethod -Method Post -Uri 'http://TV_IP:8080/action' -Body @{ action = 'fullscreen' }
Invoke-RestMethod -Method Post -Uri 'http://TV_IP:8080/action' -Body @{ action = 'pip' }
Invoke-RestMethod -Uri 'http://TV_IP:8080/config'
```

## Review checks

1. Launch from the TV launcher and grant overlay permission.
2. Play an HTTP video and an HLS stream. Press Home and open another app; check that video continues and the TV remote controls the foreground app.
3. Exercise all six positions, minimum/maximum sizes and volume 0/50/100.
4. Use APiP Remote's manual server entry, select a stream, exercise its PiP controls and inspect `/config`.
5. Stop the player; verify that its window and HTTP listener disappear. Start it again.
6. Install a second build with the same signature and confirm that it updates the existing app.

## Stack

Kotlin, Android SDK, Compose for TV, Media3 ExoPlayer (HTTP/HLS), Android overlay window, foreground media playback service, Ktor CIO, Gradle Kotlin DSL.
