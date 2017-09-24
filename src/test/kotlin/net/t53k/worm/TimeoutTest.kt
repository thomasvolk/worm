package net.t53k.worm

import org.junit.Assert.assertTrue
import org.junit.Test

class TimeoutTest {
    val DURATION = 10L
    @Test
    fun timeout() {
        val begin = System.currentTimeMillis()
        val timeout = MilliSecondsTimeout(DURATION)
        timeout.start {  }
        val currentDuration = System.currentTimeMillis() - begin
        assertTrue("currentDuration=$currentDuration", currentDuration >= DURATION)
    }
}