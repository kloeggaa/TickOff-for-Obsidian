package io.github.kloeggaa.tickoff.api

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyCheckTest {

    @Test
    fun successCodesMeanKeyAndModelWork() {
        assertEquals(KeyCheckOutcome.OK, KeyCheck.outcomeFor(200))
        assertEquals(KeyCheckOutcome.OK, KeyCheck.outcomeFor(204))
    }

    @Test
    fun authenticationAndPermissionErrors() {
        assertEquals(KeyCheckOutcome.INVALID_KEY, KeyCheck.outcomeFor(401))
        assertEquals(KeyCheckOutcome.NO_ACCESS, KeyCheck.outcomeFor(403))
    }

    @Test
    fun unknownModelIsReportedSeparately() {
        assertEquals(KeyCheckOutcome.MODEL_NOT_FOUND, KeyCheck.outcomeFor(404))
    }

    @Test
    fun quotaAndServerProblems() {
        assertEquals(KeyCheckOutcome.RATE_LIMITED, KeyCheck.outcomeFor(429))
        assertEquals(KeyCheckOutcome.SERVER_ERROR, KeyCheck.outcomeFor(500))
        assertEquals(KeyCheckOutcome.SERVER_ERROR, KeyCheck.outcomeFor(529))
    }

    @Test
    fun anythingElseIsOther() {
        assertEquals(KeyCheckOutcome.OTHER, KeyCheck.outcomeFor(400))
        assertEquals(KeyCheckOutcome.OTHER, KeyCheck.outcomeFor(302))
    }
}
