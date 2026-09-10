package com.sinebloc.quietmotion

import androidx.compose.runtime.Composable

/**
 * Whether the operating system has been asked to keep motion to a minimum.
 *
 * Both platforms have the setting and both mean it medically — vestibular disorders and
 * migraine are the reason it exists, not a taste preference. An app that adds motion
 * everywhere has to ask.
 *
 * Most callers want [rememberMotionScale] instead, which turns this into the number
 * [LocalMotionScale] wants.
 */
@Composable
expect fun prefersReducedMotion(): Boolean
