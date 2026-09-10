# How the clips were recorded

Both GIFs show the same sequence, driven against `sample/`: the list arrives staggered,
scrolls away and back **without replaying**, replays on demand, and then goes still when
reduce motion is switched on.

Nothing here needs `ffmpeg`. `mp4gif.swift` turns a screen recording into a GIF using
AVFoundation and ImageIO, both of which ship with macOS:

```bash
swiftc -O -o /tmp/mp4gif docs/media/mp4gif.swift
/tmp/mp4gif in.mp4 out.gif <fps> <width> "start:end,start:end,..."
```

The trailing argument is a list of time ranges, stitched into one clip in order — a
recording driven by a tool with seconds of latency between taps is mostly dead air, and
this cuts the dead air out without a video editor.

## Android

```bash
./gradlew :sample:androidApp:assembleDebug
adb install -r sample/androidApp/build/outputs/apk/debug/androidApp-debug.apk
# A clean status bar, so the clip does not date itself or leak a device name:
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1000
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

adb shell screenrecord --time-limit 16 --size 720x1606 --bit-rate 10M /sdcard/qm.mp4
# ...meanwhile, in another shell: am start, input swipe, input tap.
adb pull /sdcard/qm.mp4
```

## iOS

```bash
xcrun simctl boot 'iPhone 17 Pro'
xcodebuild -project sample/iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -sdk iphonesimulator -arch arm64 -derivedDataPath /tmp/dd build
xcrun simctl install booted /tmp/dd/Build/Products/Debug-iphonesimulator/iosApp.app

xcrun simctl io booted recordVideo --codec h264 --force ios.mov   # Ctrl-C to stop
xcrun simctl launch booted com.sinebloc.quietmotion.sample
```

Taps go in through whatever drives the simulator; the ranges passed to `mp4gif` are what
keeps the result tight.
