# quiet-motion sample

One screen, two hosts. `shared/` holds the whole UI; `androidApp/` and `iosApp/` are
thin shells around it.

The screen shows the three things worth seeing:

- **Arrivals.** The list fades and lifts in, staggered. Scroll it away and back: the
  rows draw in place, with no second animation. That is `Arrivals` living above the
  `LazyColumn` rather than inside it.
- **Replay.** The button hands out a fresh `Arrivals` — the same mechanism as
  `rememberArrivals(selectedTab)` — so the entrance runs again on demand, while
  scrolling still does not trigger it.
- **Reduce motion.** The switch overrides `LocalMotionScale` to `0f` in-app, so the
  difference is visible without a trip to Settings. Everything stops at once: the
  entrance snaps in, `breathing()` holds its end value, and `sweeping()` returns null
  so the shimmer draws as a plain bar. There is no per-call-site check anywhere in
  `App.kt` — providing the scale once is the whole integration.

With the switch off, the scale comes from `rememberMotionScale()`, which is the real
OS setting: **Accessibility → Remove animations** on Android,
**Accessibility → Motion → Reduce Motion** on iOS.

## Run it

Android — an emulator or a device, then:

```bash
./gradlew :sample:androidApp:installDebug
```

iOS — the Xcode project builds the Kotlin framework itself, through a run script that
calls Gradle, so there is no separate step:

```bash
open sample/iosApp/iosApp.xcodeproj
```

Or from the command line:

```bash
xcrun simctl boot 'iPhone 17 Pro'
xcodebuild -project sample/iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -sdk iphonesimulator -arch arm64 -derivedDataPath build/dd build
xcrun simctl install booted build/dd/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch booted com.sinebloc.quietmotion.sample
```

The sample depends on `project(":")` rather than on the published coordinate, so it
always builds against the working tree. A real consumer writes
`implementation("com.sinebloc:quiet-motion:0.1.0")` instead.

`sample/iosApp` carries no signing configuration — it is built for the simulator.
Recording the clips in `docs/media` is described in
[docs/media/README.md](../docs/media/README.md).
