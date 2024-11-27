package kakkoiichris.gemboozled.game

import kakkoiichris.gemboozled.Resources
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.view.View
import kotlin.math.abs
import kotlin.random.Random


/**
 * Gemboozled
 *
 * Copyright (C) 2022, KakkoiiChris
 *
 * File:    Gem.kt
 *
 * Created: Wednesday, December 28, 2022, 22:08:42
 *
 * @author Christian Bryce Alexander
 */
class Gem(x: Double, y: Double, val color: Color, val type: Type) : Box(x, y, SIZE.toDouble(), SIZE.toDouble()),
    Renderable {
    private val acceleration = Vector(0.0, 0.15)
    private var velocity = Vector()

    private val sizeOffset get() = if (hovering) 16.0 else 0.0

    var falling = false
    var removed = false
    var hovering = false

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        if (falling) {
            velocity += acceleration
            position += velocity * time.delta
        }
    }

    override fun render(view: View, renderer: Renderer) {
        renderer.drawSheet(Resources.gems, color.ordinal, type.ordinal, resized(sizeOffset))
    }

    fun remove() {
        removed = true

        falling = false

        velocity.zero()
    }

    fun halt() {
        falling = false

        velocity *= 0.0
    }

    fun affectGame(game: Game, row: Int, column: Int) =
        type.affectGame(game, row, column, color)

    companion object {
        const val SIZE = 51
    }

    enum class Color {
        RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, WHITE;

        val next get() = entries[(ordinal + 1) % entries.size]

        companion object {
            fun random(random: Random = Random.Default) =
                entries[random.nextInt(entries.size)]
        }
    }

    enum class Type {
        BASIC,

        CROSS {
            override fun affectGame(game: Game, row: Int, column: Int, color: Color) {
                for (i in 0 until game.gameMode.boardSize) {
                    game.removeAt(row, i)
                    game.removeAt(i, column)
                }
            }
        },

        EXPLODE {
            override fun affectGame(game: Game, row: Int, column: Int, color: Color) {
                for (rowOffset in -2..2) {
                    val actualRow = row + rowOffset

                    if (actualRow !in 0 until game.gameMode.boardSize) continue

                    for (columnOffset in -2..2) {
                        val actualColumn = column + columnOffset

                        if (actualColumn !in 0 until game.gameMode.boardSize) continue

                        if (manhattanDistance(0, 0, rowOffset, columnOffset) > 2) continue

                        game.removeAt(row + rowOffset, column + columnOffset)
                    }
                }
            }
        },

        SOLE {
            override fun affectGame(game: Game, row: Int, column: Int, color: Color) {
                game.removeIf { gem -> color === gem.color }
            }
        },

        SCATTER {
            override fun affectGame(game: Game, row: Int, column: Int, color: Color) {
                repeat(15) {
                    var rr: Int
                    var cc: Int

                    do {
                        rr = Random.nextInt(game.gameMode.boardSize)
                        cc = Random.nextInt(game.gameMode.boardSize)
                    }
                    while (game.isRemoved(rr, cc) == true)

                    game.removeAt(rr, cc)
                }
            }
        },

        WARP {
            override fun allowMove(ra: Int, ca: Int, rb: Int, cb: Int) = true
        },

        BONUS {
            override val score = 25
        },

        TEN_SECOND {
            override fun affectGame(game: Game, row: Int, column: Int, color: Color) {
                game.gameTime += 10
            }
        },

        TWENTY_SECOND {
            override fun affectGame(game: Game, row: Int, column: Int, color: Color) {
                game.gameTime += 20
            }
        },

        THIRTY_SECOND {
            override fun affectGame(game: Game, row: Int, column: Int, color: Color) {
                game.gameTime += 30
            }
        };

        open val score = 10

        open fun allowMove(ra: Int, ca: Int, rb: Int, cb: Int) =
            manhattanDistance(ra, ca, rb, cb) == 1

        open fun affectGame(game: Game, row: Int, column: Int, color: Color) = Unit

        protected fun manhattanDistance(ra: Int, ca: Int, rb: Int, cb: Int) =
            abs(ra - rb) + abs(ca - cb)
    }
}