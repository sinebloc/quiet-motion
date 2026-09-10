package com.sinebloc.quietmotion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.TimeSource

/**
 * Remembers when each thing on a screen was first drawn — kept *above* the list that
 * draws it.
 *
 * This exists to fix the standard bug in staggered list entrances. A `LazyColumn`
 * disposes a row that scrolls out of view and composes it again on the way back, which
 * takes any `remember { false }` entrance flag with it — so every scroll replays the
 * animation and the list never stops twitching. Holding first-seen times outside the
 * list makes a row's second appearance a *reappearance*: [ageMillis] comes back large,
 * and [arriving] draws it in place with no animation at all.
 *
 * ```
 * val arrivals = rememberArrivals()
 * LazyColumn {
 *     itemsIndexed(rows) { index, row ->
 *         Row(Modifier.arriving(arrivals, row.id, QuietMotion.stagger(index))) { ... }
 *     }
 * }
 * ```
 *
 * Deliberately a plain map and not snapshot state. Nothing here should ever cause a
 * recomposition — it is a record of what already happened, read during composition and
 * never observed.
 */
@Stable
class Arrivals {
    private val firstSeen = HashMap<Any, TimeSource.Monotonic.ValueTimeMark>()

    /** Milliseconds since [key] was first asked about. Zero on the first call. */
    fun ageMillis(key: Any): Long =
        firstSeen.getOrPut(key) { TimeSource.Monotonic.markNow() }
            .elapsedNow().inWholeMilliseconds
}

/**
 * A fresh [Arrivals] whenever [keys] change — so switching tabs, or changing filter,
 * counts as the screen arriving again, while scrolling does not.
 */
@Composable
fun rememberArrivals(vararg keys: Any?): Arrivals = remember(*keys) { Arrivals() }

/**
 * Fades and lifts this element into place the first time [key] is drawn, and does
 * nothing on every appearance after that.
 *
 * @param delayMillis its place in the stagger. Use [QuietMotion.stagger] for a list.
 * @param rise how far it travels. Short by design — settling, not flying in.
 */
@Composable
fun Modifier.arriving(
    arrivals: Arrivals,
    key: Any,
    delayMillis: Int = 0,
    rise: Dp = 10.dp,
    durationMillis: Int = QuietMotion.Deliberate,
): Modifier {
    val progress = arrivalState(arrivals, key, delayMillis, durationMillis) ?: return this
    val risePx = with(LocalDensity.current) { rise.toPx() }
    // Read inside the layer block, so each frame redraws without recomposing.
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * risePx
    }
}

/**
 * The same arrival as a number, 0 to 1, for the cases where it has to drive *layout*
 * rather than opacity — a rule that draws itself down the side of a paragraph as the
 * paragraph lands.
 *
 * Costs a recomposition per frame, unlike [arriving], because the value is read during
 * composition. Worth it only when something has to change size; reach for [arriving]
 * otherwise.
 */
@Composable
fun arrivalProgress(
    arrivals: Arrivals,
    key: Any,
    delayMillis: Int = 0,
    durationMillis: Int = QuietMotion.Deliberate,
): Float = arrivalState(arrivals, key, delayMillis, durationMillis)?.value ?: 1f

/**
 * Null once [key] has already arrived, or when the platform has asked for no motion —
 * so callers can drop out to a plain modifier and a plain value, holding no animation
 * state and paying nothing on every later scroll past.
 */
@Composable
private fun arrivalState(
    arrivals: Arrivals,
    key: Any,
    delayMillis: Int,
    durationMillis: Int,
): State<Float>? {
    val scale = LocalMotionScale.current
    if (scale == 0f || arrivals.ageMillis(key) > (delayMillis + durationMillis) * scale) return null

    var shown by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) { shown = true }
    return animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tweenMotion(durationMillis, delayMillis, QuietMotion.Settle),
        label = "arriving",
    )
}
