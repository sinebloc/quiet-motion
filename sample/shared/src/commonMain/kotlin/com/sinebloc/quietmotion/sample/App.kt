package com.sinebloc.quietmotion.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinebloc.quietmotion.Arrivals
import com.sinebloc.quietmotion.LocalMotionScale
import com.sinebloc.quietmotion.QuietMotion
import com.sinebloc.quietmotion.arriving
import com.sinebloc.quietmotion.breathing
import com.sinebloc.quietmotion.rememberArrivals
import com.sinebloc.quietmotion.rememberMotionScale
import com.sinebloc.quietmotion.sweeping

/**
 * The whole sample, shared by the Android app and the iOS app.
 *
 * The switch at the top is an in-app override of [LocalMotionScale], so the difference
 * the library makes is visible without leaving for Settings. A real app would provide
 * `rememberMotionScale()` and nothing else — which is what happens here when the
 * override is off.
 */
@Composable
fun App() {
    val platformScale = rememberMotionScale()
    var overrideReduced by remember { mutableStateOf(false) }
    val scale = if (overrideReduced) 0f else platformScale

    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(LocalMotionScale provides scale) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Demo(
                    reduced = scale == 0f,
                    overrideReduced = overrideReduced,
                    onOverrideChange = { overrideReduced = it },
                )
            }
        }
    }
}

@Composable
private fun Demo(
    reduced: Boolean,
    overrideReduced: Boolean,
    onOverrideChange: (Boolean) -> Unit,
) {
    // Bumping this key hands out a fresh Arrivals, so the entrance can be replayed on
    // demand. It is the same mechanism as rememberArrivals(selectedTab).
    var run by remember { mutableIntStateOf(0) }
    val arrivals = rememberArrivals(run)

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(56.dp))
        Text(
            "quiet-motion",
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            if (reduced) "motion scale 0 — reduced" else "motion scale 1",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = overrideReduced, onCheckedChange = onOverrideChange)
            Spacer(Modifier.width(12.dp))
            Text(
                "Reduce motion",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            FilledTonalButton(onClick = { run++ }) { Text("Replay") }
        }

        Spacer(Modifier.height(16.dp))
        Waiting(arrivals)

        Spacer(Modifier.height(20.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            itemsIndexed(Primitives) { index, row ->
                RowCard(
                    row = row,
                    modifier = Modifier.arriving(
                        arrivals = arrivals,
                        key = row.id,
                        delayMillis = QuietMotion.stagger(index),
                    ),
                )
            }
        }
    }
}

/** The two infinite-loop primitives, which are the ones that go wrong hand-rolled. */
@Composable
private fun Waiting(arrivals: Arrivals) {
    Column(Modifier.arriving(arrivals, "waiting")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                val alpha = breathing(
                    from = 0.25f,
                    to = 1f,
                    durationMillis = 700,
                    label = "dot$i",
                    staggerMillis = i * 160,
                )
                Box(
                    Modifier.size(9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
                )
                Spacer(Modifier.width(7.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(
                "breathing()",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        Shimmer()
    }
}

/**
 * `sweeping()` answers null under reduce-motion, and the caller draws a plain bar —
 * a highlight frozen halfway across is a smudge, not a calmer sweep.
 */
@Composable
private fun Shimmer() {
    val sweep = sweeping(durationMillis = 1400, label = "shimmer")
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

    Box(
        Modifier.fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(base)
            .drawBehind {
                val progress = sweep?.value ?: return@drawBehind
                val travel = size.width * 2f
                val start = -size.width + progress * travel
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, highlight, Color.Transparent),
                        startX = start,
                        endX = start + size.width,
                    ),
                )
            },
    )
}

@Composable
private fun RowCard(row: Primitive, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                row.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                row.subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class Primitive(val id: String, val title: String, val subtitle: String)

private val Primitives = listOf(
    Primitive("scale", "LocalMotionScale", "One composition local, provided once at the root"),
    Primitive("arriving", "Modifier.arriving", "Fades and lifts in, exactly once per key"),
    Primitive("progress", "arrivalProgress", "The same 0→1, when it has to drive layout"),
    Primitive("tween", "tweenMotion", "The standard tween, already scaled"),
    Primitive("tweenScaled", "tweenScaled", "For the slots that are not composable lambdas"),
    Primitive("spring", "springMotion", "Critically damped, and a snap under reduce-motion"),
    Primitive("millis", "scaledMillis", "A duration, scaled, when you need the number"),
    Primitive("breathing", "breathing", "Oscillates, and starts nothing when scale is zero"),
    Primitive("sweeping", "sweeping", "Null under reduce-motion, so the caller draws nothing"),
    Primitive("stagger", "QuietMotion.stagger", "Nth sibling's head start, capped so lists end"),
    Primitive("settle", "QuietMotion.Settle", "Decelerate hard, arrive without recoil"),
    Primitive("arrivals", "Arrivals", "First-seen times, held above the list that draws them"),
)
