package kakkoiichris.gemboozled.game

import kakkoiichris.gemboozled.Resources
import kakkoiichris.hypergame.util.data.json.Node
import kotlin.random.Random

/**
 * Gemboozled
 *
 * Copyright (C) 2023, KakkoiiChris
 *
 * File:    GameMode.kt
 *
 * Created: Sunday, January 01, 2023, 16:12:39
 *
 * @author Christian Bryce Alexander
 */
data class GameMode(
    val name: String,
    val time: Double,
    val size: Int,
    val end: End,
    val startGemGenerator: GemGenerator,
    val gemGenerator: GemGenerator
) {
    fun getStartGem(x: Double, y: Double) =
        startGemGenerator.generate(x, y)

    fun getGem(x: Double, y: Double) =
        gemGenerator.generate(x, y)

    companion object {
        fun loadAll(): List<GameMode> {
            val gameModes = mutableListOf<GameMode>()

            val json = Resources.gameModes
            val list = json["modes"]!!.asObjectArray()

            for (modeData in list) {
                gameModes += parseGameMode(modeData)
            }

            return gameModes
        }

        private fun parseGameMode(modeData: Node.Object): GameMode {
            val name = modeData["name"]!!.asString()
            val time = modeData["time"]!!.asDouble()
            val size = modeData["size"]!!.asInt()

            val endData = modeData["end"]!!.asObject()
            val end = parseEnd(endData)

            val startGemData = modeData["startGemWeights"]!!.asObject()
            val startGemGenerator = parseGemGenerator(startGemData)

            val gemData = modeData["gemWeights"]!!.asObject()
            val gemGenerator = parseGemGenerator(gemData)

            return GameMode(name, time, size, end, startGemGenerator, gemGenerator)
        }

        private fun parseEnd(endData: Node.Object): End {
            val type = endData["type"]!!.asString()

            return when (type) {
                "clock" -> {
                    val over = endData["over"]!!.asBoolean()
                    val limit = endData["limit"]!!.asDouble()

                    End.Clock(over, limit)
                }

                "score" -> {
                    val limit = endData["limit"]!!.asInt()

                    End.Score(limit)
                }

                else    -> TODO("WRONG END")
            }
        }

        private fun parseGemGenerator(gemData: Node.Object): GemGenerator {
            val colorWeights = gemData["color"]!!.asIntArray()
            val typeWeights = gemData["type"]!!.asIntArray()

            return GemGenerator(colorWeights, typeWeights)
        }
    }
}

sealed interface End {
    fun isMet(game: Game): Boolean

    data class Clock(val over: Boolean, val limit: Double) : End {
        override fun isMet(game: Game) =
            if (over)
                game.gameTime >= limit
            else
                game.gameTime <= limit
    }

    data class Score(val limit: Int) : End {
        override fun isMet(game: Game) =
            game.score >= limit
    }
}

class GemGenerator(colorWeights: IntArray, typeWeights: IntArray) {
    val colorGradient = createGradient(colorWeights)
    val typeGradient = createGradient(typeWeights)

    private fun createGradient(weights: IntArray): Gradient {
        val cumulativeWeights = weights
            .mapIndexed { i, w -> Weight(i, w) }
            .sortedBy(Weight::weight)
            .filter { it.weight != 0 }
            .toMutableList()

        val sum = cumulativeWeights.sumOf { it.weight }

        for (i in 1..<cumulativeWeights.size) {
            cumulativeWeights[i] += cumulativeWeights[i - 1]
        }

        return Gradient(sum, cumulativeWeights.toTypedArray())
    }

    fun generate(x: Double, y: Double): Gem {
        val colorIndex = colorGradient.get()
        val typeIndex = typeGradient.get()

        return Gem(x, y, Gem.Color.entries[colorIndex], Gem.Type.entries[typeIndex])
    }

    data class Gradient(val sum:Int, val weights: Array<Weight>) {
        fun get(random: Random = Random.Default): Int {
            val g = random.nextInt(sum + 1)

            for ((element, weight) in weights) {
                if (g < weight) {
                    return element
                }
            }

            return weights.last().element
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Gradient

            if (sum != other.sum) return false
            if (!weights.contentEquals(other.weights)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = sum
            result = 31 * result + weights.contentHashCode()
            return result
        }
    }

    data class Weight(val element: Int, val weight: Int) {
        operator fun plus(other: Weight) =
            Weight(element, weight + other.weight)
    }
}