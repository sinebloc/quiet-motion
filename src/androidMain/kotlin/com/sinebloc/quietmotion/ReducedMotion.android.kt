package com.sinebloc.quietmotion

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android has no single "reduce motion" switch. The one users actually reach for is
 * Developer options → Animator duration scale, and Accessibility → Remove animations
 * writes to the same value — zero means every window and view animation is off, so an
 * app that keeps animating is the only thing still moving on the device.
 *
 * Read once per composition of whatever provides the scale: the setting cannot change
 * without the user leaving the app, and polling it every frame would be a
 * content-resolver call in the middle of one.
 */
@Composable
actual fun prefersReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
