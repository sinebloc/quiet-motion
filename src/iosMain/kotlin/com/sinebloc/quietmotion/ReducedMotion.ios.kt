package com.sinebloc.quietmotion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

/**
 * Settings → Accessibility → Motion → Reduce Motion. UIKit exposes it as a plain
 * function, so there is nothing to observe and nothing to release.
 */
@Composable
actual fun prefersReducedMotion(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }
