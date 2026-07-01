package edu.jnu.smartedu.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditCalculatorTest {
    @Test
    fun overflowCreditsCountTowardLargeGroup() {
        val input = CreditInput(
            largeRequired = 10.0,
            smallRequired = listOf(2.0, 2.0),
            earnedBySmall = listOf(6.0, 4.0),
        )

        assertTrue(CreditCalculator.isLargeGroupMet(input))
    }

    @Test
    fun largeGroupRequiresEverySmallGroupToBeMet() {
        val input = CreditInput(
            largeRequired = 10.0,
            smallRequired = listOf(2.0, 2.0),
            earnedBySmall = listOf(9.0, 1.0),
        )

        assertFalse(CreditCalculator.isLargeGroupMet(input))
    }
}
