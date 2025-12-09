package com.example.healthapp.presentation.exercise

import kotlin.math.abs
import kotlin.math.min

object DynamicTimeWarping {

    // Berechnet die euklidische Distanz zwischen zwei Punkten
    private fun distance(p1: Double, p2: Double): Double {
        return abs(p1 - p2)
    }

    /**
     * Berechnet die DTW-Distanz zwischen zwei Zeitreihen.
     * Ein niedrigerer Wert bedeutet eine höhere Ähnlichkeit.
     */
    fun calculateDistance(ts1: DoubleArray, ts2: DoubleArray): Double {
        val n = ts1.size
        val m = ts2.size
        val dtwMatrix = Array(n + 1) { DoubleArray(m + 1) { Double.POSITIVE_INFINITY } }
        dtwMatrix[0][0] = 0.0

        for (i in 1..n) {
            for (j in 1..m) {
                val cost = distance(ts1[i - 1], ts2[j - 1])
                dtwMatrix[i][j] = cost + minOf(
                    dtwMatrix[i - 1][j],      // Insertion
                    dtwMatrix[i][j - 1],      // Deletion
                    dtwMatrix[i - 1][j - 1]   // Match
                )
            }
        }
        return dtwMatrix[n][m]
    }
}

