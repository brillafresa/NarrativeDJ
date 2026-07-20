package com.narrativedj.app.scheduler

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Cushion (bridge) song scheduler — Kotlin port of docs/research.md §2.1 Python reference.
 */
class CushionMusicScheduler(
    private val trackVectorDb: Map<String, DoubleArray>,
    private val maxBridges: Int = 2,
    private val alphaThreshold: Double = 0.55,
) {
    private val threshold: Double = alphaThreshold

    fun getDistance(v1: DoubleArray, v2: DoubleArray): Double {
        val bpmDist = abs(v1[0] - v2[0]) / 200.0
        val energyDist = abs(v1[1] - v2[1])
        val valenceDist = abs(v1[2] - v2[2])

        val emb1 = v1.copyOfRange(3, v1.size)
        val emb2 = v2.copyOfRange(3, v2.size)
        val norm1 = vectorNorm(emb1)
        val norm2 = vectorNorm(emb2)
        val cosineDist = if (norm1 == 0.0 || norm2 == 0.0) {
            1.0
        } else {
            1.0 - (dot(emb1, emb2) / (norm1 * norm2))
        }

        return 0.25 * bpmDist + 0.25 * energyDist + 0.20 * valenceDist + 0.30 * cosineDist
    }

    fun calculateCushionRoute(currentId: String, targetId: String): List<String>? {
        val vCurr = trackVectorDb[currentId] ?: return null
        val vTarget = trackVectorDb[targetId] ?: return null

        if (getDistance(vCurr, vTarget) <= threshold) {
            return emptyList()
        }

        var bestSingleBridge: List<String>? = null
        var minDeviation = Double.POSITIVE_INFINITY
        for ((candidateId, vCand) in trackVectorDb) {
            if (candidateId == currentId || candidateId == targetId) continue
            val d1 = getDistance(vCurr, vCand)
            val d2 = getDistance(vCand, vTarget)
            if (d1 < threshold && d2 < threshold) {
                val total = d1 + d2
                if (total < minDeviation) {
                    minDeviation = total
                    bestSingleBridge = listOf(candidateId)
                }
            }
        }
        if (bestSingleBridge != null) {
            return bestSingleBridge
        }

        var bestDoubleBridge: List<String>? = null
        var minDoubleDeviation = Double.POSITIVE_INFINITY
        for ((s1Id, vCand1) in trackVectorDb) {
            if (s1Id == currentId || s1Id == targetId) continue
            val d1 = getDistance(vCurr, vCand1)
            if (d1 >= threshold) continue
            for ((s2Id, vCand2) in trackVectorDb) {
                if (s2Id == currentId || s2Id == targetId || s2Id == s1Id) continue
                val d2 = getDistance(vCand1, vCand2)
                val d3 = getDistance(vCand2, vTarget)
                if (d2 < threshold && d3 < threshold) {
                    val total = d1 + d2 + d3
                    if (total < minDoubleDeviation) {
                        minDoubleDeviation = total
                        bestDoubleBridge = listOf(s1Id, s2Id)
                    }
                }
            }
        }
        return bestDoubleBridge
    }

    companion object {
        fun trackToVector(bpm: Double, energy: Double, valence: Double, embedding: DoubleArray): DoubleArray {
            return doubleArrayOf(bpm, energy, valence, *embedding)
        }

        private fun dot(a: DoubleArray, b: DoubleArray): Double {
            var sum = 0.0
            val n = minOf(a.size, b.size)
            for (i in 0 until n) sum += a[i] * b[i]
            return sum
        }

        private fun vectorNorm(v: DoubleArray): Double = sqrt(dot(v, v))
    }
}
