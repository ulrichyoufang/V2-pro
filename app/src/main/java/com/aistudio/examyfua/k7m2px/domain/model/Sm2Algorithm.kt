package com.aistudio.examyfua.k7m2px.domain.model

import kotlin.math.max
import kotlin.math.roundToInt

data class Sm2Result(
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val nextReviewAtEpochMs: Long
)

object Sm2Algorithm {
    private const val ONE_DAY_MS = 86_400_000L

    fun calculateNextReview(
        currentEaseFactor: Double,
        currentIntervalDays: Int,
        currentRepetitions: Int,
        quality: Sm2Quality,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Sm2Result {
        val q = quality.q
        val newRepetitions: Int
        val newIntervalDays: Int

        if (q < 3) {
            newRepetitions = 0
            newIntervalDays = 1
        } else {
            newRepetitions = currentRepetitions + 1
            newIntervalDays = when (newRepetitions) {
                1 -> 1
                2 -> 6
                else -> (currentIntervalDays * currentEaseFactor).roundToInt().coerceAtLeast(1)
            }
        }

        val delta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
        val newEaseFactor = max(1.3, (currentEaseFactor + delta)).coerceAtMost(5.0)
        val nextReview = nowEpochMs + newIntervalDays * ONE_DAY_MS

        return Sm2Result(
            easeFactor = newEaseFactor,
            intervalDays = newIntervalDays,
            repetitions = newRepetitions,
            nextReviewAtEpochMs = nextReview
        )
    }
}
