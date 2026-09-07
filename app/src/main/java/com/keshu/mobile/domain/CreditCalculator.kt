package com.keshu.mobile.domain

data class CreditInput(
    val largeRequired: Double,
    val smallRequired: List<Double>,
    val earnedBySmall: List<Double>,
)

object CreditCalculator {
    fun isLargeGroupMet(input: CreditInput): Boolean {
        val allSmallMet = input.smallRequired.zip(input.earnedBySmall).all { (required, earned) -> earned >= required }
        val totalEarned = input.earnedBySmall.sum()
        return allSmallMet && totalEarned >= input.largeRequired
    }
}
