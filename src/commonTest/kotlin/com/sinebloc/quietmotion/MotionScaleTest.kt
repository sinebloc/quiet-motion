package com.sinebloc.quietmotion

import androidx.compose.animation.core.TweenSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The reduce-motion contract.
 *
 * Every timed thing in this library routes through [tweenScaled] — `tweenMotion` is a
 * thin composable wrapper over it, and callers whose transition slots are not
 * composable lambdas call it directly. So the scale arriving here as `0f` is the single
 * point where "the user asked for less movement" becomes "nothing moves", and it is
 * worth pinning.
 *
 * What this file cannot reach: [breathing] and [sweeping], whose early returns are the
 * actual strobe guard, and [springMotion]'s snap. All three are `@Composable` and would
 * need Compose UI test infrastructure this module does not carry.
 */
class MotionScaleTest {

    private fun spec(scale: Float, duration: Int, delay: Int = 0): TweenSpec<Float> =
        tweenScaled<Float>(scale, duration, delay) as TweenSpec<Float>

    @Test
    fun reduced_motion_collapses_a_tween_to_a_cut() {
        val cut = spec(scale = 0f, duration = QuietMotion.Deliberate, delay = QuietMotion.Stagger)
        assertEquals(0, cut.durationMillis)
        assertEquals(0, cut.delay)
    }

    @Test
    fun reduced_motion_collapses_every_default_duration() {
        // The strobe bug was a *loop* surviving the scale. Nothing timed may survive it,
        // however long it was authored to be.
        listOf(QuietMotion.Standard, QuietMotion.Deliberate, QuietMotion.Stagger)
            .forEach { authored ->
                assertEquals(0, spec(scale = 0f, duration = authored).durationMillis)
            }
    }

    @Test
    fun full_motion_leaves_the_authored_timing_alone() {
        val normal = spec(scale = 1f, duration = QuietMotion.Deliberate, delay = QuietMotion.Stagger)
        assertEquals(QuietMotion.Deliberate, normal.durationMillis)
        assertEquals(QuietMotion.Stagger, normal.delay)
    }

    @Test
    fun the_scale_is_a_multiplier_not_a_switch() {
        // Nothing sets a fractional scale today, but tweenScaled multiplies rather than
        // branching on `if (reduced) 0`, so a future half-speed preference should not
        // need a second code path. Rounds rather than truncates.
        val half = spec(scale = 0.5f, duration = QuietMotion.Standard)
        assertEquals(QuietMotion.Standard / 2, half.durationMillis)
    }

    @Test
    fun stagger_caps_so_a_long_list_still_finishes() {
        assertEquals(0, QuietMotion.stagger(index = 0))
        assertEquals(QuietMotion.Stagger * 3, QuietMotion.stagger(index = 3))
        // Past the cap every sibling shares the last head start rather than queueing.
        assertEquals(QuietMotion.stagger(index = 8), QuietMotion.stagger(index = 40))
    }

    @Test
    fun stagger_is_scaled_by_the_caller_not_by_itself() {
        // stagger() returns an authored delay; it is tweenScaled that zeroes it. Proven
        // here so the two halves cannot drift into both scaling, or neither.
        val authored = QuietMotion.stagger(index = 2)
        assertTrue(authored > 0)
        assertEquals(0, spec(scale = 0f, duration = QuietMotion.Standard, delay = authored).delay)
    }
}
