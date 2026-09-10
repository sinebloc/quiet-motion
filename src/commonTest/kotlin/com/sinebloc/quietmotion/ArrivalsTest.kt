package com.sinebloc.quietmotion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [Arrivals] is a plain object with no Compose in it, which is the point — the whole
 * fix for the recycling bug is that the record of what has already been seen lives
 * outside composition, so it can be tested without a UI harness.
 */
class ArrivalsTest {

    @Test
    fun the_first_sighting_of_a_key_is_age_zero() {
        // A row that has never been drawn must animate, so its age has to start at the
        // bottom of the range rather than at "unknown".
        assertEquals(0L, Arrivals().ageMillis("row-1"))
    }

    @Test
    fun a_key_keeps_its_first_sighting_across_later_asks() {
        val arrivals = Arrivals()
        arrivals.ageMillis("row-1")
        busyWait()
        // This is the recycling case: the row was disposed and composed again. The
        // second answer must be the age of the *first* sighting, because that is what
        // makes the reappearance draw with no animation.
        assertTrue(arrivals.ageMillis("row-1") > 0L)
    }

    @Test
    fun keys_are_tracked_independently() {
        val arrivals = Arrivals()
        arrivals.ageMillis("row-1")
        busyWait()
        // A row scrolling into view for the first time still animates, however long the
        // list has been on screen — otherwise only the initial screenful ever moves.
        assertEquals(0L, arrivals.ageMillis("row-2"))
    }

    /**
     * Spins rather than sleeping: `kotlin.test` is common code with no suspending
     * harness here, and the assertions only need the monotonic clock to have advanced
     * by a whole millisecond.
     */
    private fun busyWait() {
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        while (start.elapsedNow().inWholeMilliseconds < 2L) { /* spin */ }
    }
}
