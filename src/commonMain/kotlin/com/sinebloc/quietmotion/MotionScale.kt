package com.sinebloc.quietmotion

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.roundToInt

/**
 * Multiplies every duration that routes through this library. 1 normally; **0 when the
 * platform's own reduce-motion setting is on**, which collapses every animation to a cut.
 *
 * A global scale rather than a boolean checked at each call site: an animation that
 * forgot to ask would be the one that made someone ill, and a scale composes — the
 * specs in this file all multiply by it, so opting in is automatic and opting out is
 * impossible to forget.
 *
 * Provide it once, near the root of the tree:
 *
 * ```
 * CompositionLocalProvider(LocalMotionScale provides rememberMotionScale()) { ... }
 * ```
 *
 * Static because it changes only when the OS setting does, which in practice means
 * never inside one launch.
 */
val LocalMotionScale = staticCompositionLocalOf { 1f }

/**
 * The motion scale this device is asking for: 0 when the OS reduce-motion setting is
 * on, 1 otherwise. Feed it to [LocalMotionScale].
 *
 * A scale rather than the raw boolean, because [tweenScaled] multiplies rather than
 * branching — a future half-speed preference needs a different number here and no
 * second code path anywhere else.
 */
@Composable
fun rememberMotionScale(): Float = if (prefersReducedMotion()) 0f else 1f

/** Defaults, so nothing in this library has to invent a duration at a call site. */
object QuietMotion {
    /** Something entering or leaving in place. */
    const val Standard = 260

    /** Something arriving: a card, a screen, a panel of prose. */
    const val Deliberate = 420

    /** Between siblings in a staggered reveal. Small — a ripple, not a queue. */
    const val Stagger = 70

    /**
     * Decelerate hard, arrive without recoil. Near-instant off the mark so a tap feels
     * answered, then a long tail so the end is calm. Nothing here overshoots: an
     * entrance that bounces is an entrance that has to be watched.
     */
    val Settle: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    /**
     * Nth sibling's head start in a staggered reveal, capped so a long list still ends.
     *
     * Returns an *authored* delay — it is [tweenScaled] that zeroes it under
     * reduce-motion, not this. Scaling in both places would double-count.
     */
    fun stagger(index: Int, step: Int = Stagger, max: Int = 8): Int =
        index.coerceAtMost(max) * step
}

/** [millis] as the current [LocalMotionScale] wants it. Zero means snap. */
@Composable
fun scaledMillis(millis: Int): Int = (millis * LocalMotionScale.current).roundToInt()

/**
 * The standard tween at an explicitly supplied [scale].
 *
 * The composable [tweenMotion] is what most callers want. This plain overload exists
 * because a `NavHost`'s transition slots and an `AnimatedContent`'s `transitionSpec`
 * are *not* composable lambdas — they cannot read a CompositionLocal — so the caller
 * reads [LocalMotionScale] once in its own body and hands the value down.
 */
fun <T> tweenScaled(
    scale: Float,
    durationMillis: Int = QuietMotion.Standard,
    delayMillis: Int = 0,
    easing: Easing = QuietMotion.Settle,
): FiniteAnimationSpec<T> = tween(
    durationMillis = (durationMillis * scale).roundToInt(),
    delayMillis = (delayMillis * scale).roundToInt(),
    easing = easing,
)

/** The standard tween, already scaled. */
@Composable
fun <T> tweenMotion(
    durationMillis: Int = QuietMotion.Standard,
    delayMillis: Int = 0,
    easing: Easing = QuietMotion.Settle,
): FiniteAnimationSpec<T> =
    tweenScaled(LocalMotionScale.current, durationMillis, delayMillis, easing)

/**
 * A spring for values that are *nudged* rather than scheduled — a pip filling, a
 * selection dot taking. Critically damped by default, so it settles without wobble,
 * and a plain [snap] under reduce-motion.
 */
@Composable
fun <T> springMotion(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMediumLow,
): FiniteAnimationSpec<T> =
    if (LocalMotionScale.current == 0f) snap() else spring(dampingRatio, stiffness)

/**
 * A value breathing between [from] and [to], for the things that wait: a "thinking"
 * indicator, a placeholder, a row still being filled in.
 *
 * When the user has asked for reduced motion this returns [to] and starts no animation
 * at all — and that is the entire reason it exists. The obvious spelling, scaling the
 * duration by the motion scale and clamping it to at least a millisecond, produces a
 * **one-millisecond** loop when the scale is zero: a strobe at frame rate, delivered to
 * exactly the people who asked for less movement. Reach for this rather than
 * hand-rolling an infinite transition.
 */
@Composable
fun breathing(
    from: Float,
    to: Float,
    durationMillis: Int,
    label: String,
    staggerMillis: Int = 0,
): Float {
    val scale = LocalMotionScale.current
    if (scale == 0f) return to
    val transition = rememberInfiniteTransition(label = label)
    val value by transition.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (durationMillis * scale).roundToInt().coerceAtLeast(1),
            ),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset((staggerMillis * scale).roundToInt()),
        ),
        label = label,
    )
    return value
}

/**
 * A value travelling 0→1 and starting over, for a light passing across something that
 * is still being worked on — a shimmer, a progress sweep.
 *
 * Null when the user has asked for reduced motion, because a sweep has no still
 * equivalent: freezing a highlight halfway across a row is not a calmer version of the
 * effect, it is a smudge. The caller draws nothing instead. See [breathing] for the
 * strobe this shape guards against.
 *
 * Returns the [State] rather than the value so the caller can read it inside a draw
 * lambda. Reading it during composition instead would recompose the whole row every
 * frame to move a gradient a few pixels.
 */
@Composable
fun sweeping(durationMillis: Int, label: String): State<Float>? {
    val scale = LocalMotionScale.current
    if (scale == 0f) return null
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (durationMillis * scale).roundToInt().coerceAtLeast(1),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = label,
    )
}
