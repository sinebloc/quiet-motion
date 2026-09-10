package com.sinebloc.quietmotion

import androidx.compose.runtime.Composable

/**
 * Desktop has no portable reduce-motion setting, so this answers `false`.
 *
 * Windows exposes one through `SystemParametersInfo`, macOS through a
 * `com.apple.universalaccess` preference, and Linux through neither consistently.
 * Reaching any of them from here means a JNI call or spawning a process during
 * composition, which is a worse trade than being honest about not knowing.
 *
 * **This does not leave a desktop consumer without a way to honour the setting.**
 * Everything in this library multiplies by [LocalMotionScale], so an application that
 * knows the answer — from its own platform code, or from a preference of its own —
 * provides the scale directly and every primitive here obeys it:
 *
 * ```
 * CompositionLocalProvider(LocalMotionScale provides if (userWantsStillness) 0f else 1f) { ... }
 * ```
 *
 * [rememberMotionScale] is the convenience for the two platforms that can answer for
 * themselves; it is not the only way in.
 */
@Composable
actual fun prefersReducedMotion(): Boolean = false
