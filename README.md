# quiet-motion

Reduce-motion-safe animation primitives for Compose Multiplatform. Android and iOS.

Two problems, both small, both easy to get wrong:

1. **Staggered list entrances replay on every scroll.** A `LazyColumn` disposes a row
   that scrolls out of view and composes it again on the way back, taking any
   `remember { false }` entrance flag with it. The animation runs again, and the list
   never stops twitching.
2. **"Reduce motion" is medical, and it is easy to half-honour.** Vestibular disorders
   and migraine are why the setting exists. An app that checks a boolean at each call
   site will eventually forget one — and the animation it forgets is the one that makes
   somebody ill.

This library is the answer to both, in about 300 lines.

## Platforms

| Target | Reduce-motion detected from |
|---|---|
| Android | Animator duration scale — what **Accessibility → Remove animations** writes to |
| iOS (`iosArm64`, `iosSimulatorArm64`) | `UIAccessibilityIsReduceMotionEnabled` |
| JVM / desktop | Nothing — see below |

Desktop has no portable reduce-motion setting, so `prefersReducedMotion()` answers
`false` there. That does not leave you stuck: every primitive here multiplies by
`LocalMotionScale`, so provide the scale yourself and the whole library obeys it.
`rememberMotionScale()` is a convenience for the two platforms that can answer for
themselves, not the only way in.

No `iosX64`: Compose Multiplatform 1.11.x publishes no artifacts for the Intel-Mac
simulator, so the dependency could not resolve there anyway.

## Install

Publishing to Maven Central is wired up but **the first release has not been cut yet**
(it needs namespace verification and a signing key — see [RELEASING.md](RELEASING.md)).
Until then, clone this repo and:

```
./gradlew publishToMavenLocal
```

Then, with `mavenLocal()` in your repositories:

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation("com.sinebloc:quiet-motion:0.1.0")
}
```

Compose pulls transitive `androidx` artifacts, so your repositories need `google()`
alongside `mavenCentral()` — as any Compose Multiplatform consumer does.

## Provide the scale once

Everything here multiplies by one composition local. Provide it near the root of your
tree and you are done — there is no second thing to remember:

```kotlin
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMotionScale provides rememberMotionScale()) {
        content()
    }
}
```

`rememberMotionScale()` returns `0f` when the platform asks for reduced motion and `1f`
otherwise. On iOS that is `UIAccessibilityIsReduceMotionEnabled`; on Android it is the
animator duration scale, which is what **Accessibility → Remove animations** writes to
and the one users actually reach for.

A scale rather than a boolean, deliberately: `tweenScaled` *multiplies*, so a future
half-speed preference is a different number here and no second code path anywhere else.

## Arrivals

Hold the record of what has already been seen **above** the list that draws it:

```kotlin
val arrivals = rememberArrivals()

LazyColumn {
    itemsIndexed(rows) { index, row ->
        Card(Modifier.arriving(arrivals, row.id, delayMillis = QuietMotion.stagger(index)))
    }
}
```

A row's first appearance fades and lifts into place. Its second appearance — after
scrolling away and back — draws in place with no animation and holds no animation state
at all. `rememberArrivals(selectedTab)` makes a tab change count as arriving again,
while scrolling still does not.

`arrivalProgress(...)` gives you the same 0→1 value when it has to drive *layout* rather
than opacity. It costs a recomposition per frame, because the value is read during
composition; reach for `Modifier.arriving` unless something has to change size.

## The rest

| | |
|---|---|
| `tweenMotion(...)` | The standard tween, already scaled. |
| `tweenScaled(scale, ...)` | Same, for callers that cannot read a composition local — a `NavHost`'s transition slots and an `AnimatedContent`'s `transitionSpec` are not composable lambdas. Read `LocalMotionScale` in your own body and hand the value down. |
| `springMotion(...)` | Critically damped by default. A plain `snap()` under reduce-motion. |
| `scaledMillis(ms)` | A duration, scaled, for when you need the number itself. |
| `breathing(from, to, ...)` | A value oscillating for something that waits. Returns `to` and starts **no** animation under reduce-motion. |
| `sweeping(...)` | A 0→1 loop for a shimmer. `null` under reduce-motion — a sweep frozen halfway is a smudge, not a calmer sweep, so the caller draws nothing. |

`breathing` and `sweeping` exist because of a bug worth naming. The obvious way to make
an infinite transition respect the scale is to multiply its duration and clamp it to at
least a millisecond. When the scale is zero that yields a **one-millisecond loop** — a
strobe at frame rate, delivered to precisely the people who asked for less movement.
Five screens in the app this came from had written it that way. Do not hand-roll an
infinite loop; call these.

## What is tested, and what is not

`./gradlew testAndroidHostTest` runs the suite on the JVM.

Covered: `tweenScaled`'s contract (a `0f` scale collapses duration *and* delay to zero,
and the scale is a multiplier rather than a switch), `stagger`'s cap, and `Arrivals`'
first-seen bookkeeping — which is testable without a UI harness precisely because it
lives outside composition.

Not covered: `breathing`, `sweeping` and `springMotion`. Their guards are early returns
inside `@Composable` functions and need Compose UI test infrastructure this module does
not carry. They are the three things to check by hand after changing anything here.

## License

Apache-2.0. Extracted from a Compose Multiplatform app, where these primitives earned
their shape.
