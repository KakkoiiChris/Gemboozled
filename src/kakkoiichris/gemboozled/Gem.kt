package kakkoiichris.gemboozled

import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.view.View

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
class Gem(x: Double, y: Double, val color: Color, val type: Type) : Box(x, y, SIZE.toDouble(), SIZE.toDouble()), Renderable {
    private var velocity = Vector()
    private var acceleration = Vector()
    
    var removed = false
    
    fun affectBoard(row: Int, column: Int, board: Board) =
        type.affectBoard(row, column, color, board)
    
    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        velocity += acceleration * time.delta
        position += velocity
    }
    
    override fun render(view: View, renderer: Renderer) {
        renderer.drawSheet(Resources.gems, type.ordinal, color.ordinal, this)
    }
    
    enum class Color {
        RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, WHITE;
        
        companion object {
            fun random(random: Random = Random.Default) =
                values()[random.nextInt(values().size)]
        }
    }
    
    companion object {
        const val SIZE = 51
        
        fun random(x: Double, y: Double, random: Random = Random.Default): Gem {
            val color = Color.random(random)
            
            val type = if (random.nextDouble() < 0.85) {
                Type.BASIC
            }
            else {
                val s = random.nextDouble()
                
                when {
                    s < 1.0 / 7.0 -> Type.CROSS
                    s < 2.0 / 7.0 -> Type.EXPLODE
                    s < 3.0 / 7.0 -> Type.SOLE
                    s < 4.0 / 7.0 -> Type.SCATTER
                    s < 5.0 / 7.0 -> Type.WARP
                    s < 6.0 / 7.0 -> Type.BONUS
                    else          -> {
                        val t = random.nextDouble()
                        
                        when {
                            t < 1.0 / 2.0 -> Type.TEN_SECOND
                            t < 5.0 / 6.0 -> Type.TWENTY_SECOND
                            else          -> Type.THIRTY_SECOND
                        }
                    }
                }
            }
            
            return Gem(x, y, color, type)
        }
    }
    
    enum class Type {
        BASIC,
        
        CROSS {
            override fun affectBoard(row: Int, column: Int, color: Color, board: Board) {
                for (i in 0 until board.size) {
                    board.removeAt(row, i)
                    board.removeAt(i, column)
                }
            }
        },
        
        EXPLODE {
            override fun affectBoard(row: Int, column: Int, color: Color, board: Board) {
                for (rowOffset in -2..2) {
                    val actualRow = row + rowOffset
                    
                    if (actualRow !in 0 until board.size) continue
                    
                    for (columnOffset in -2..2) {
                        val actualColumn = column + columnOffset
                        
                        if (actualColumn !in 0 until board.size) continue
                        
                        if (manhattanDistance(0, 0, rowOffset, columnOffset) > 2) continue
                        
                        board.removeAt(row + rowOffset, column + columnOffset)
                    }
                }
            }
        },
        
        SOLE {
            override fun affectBoard(row: Int, column: Int, color: Color, board: Board) {
                board.removeIf { gem -> color === gem.color }
            }
        },
        
        SCATTER {
            override fun affectBoard(row: Int, column: Int, color: Color, board: Board) {
                repeat(10) {
                    var rr: Int
                    var cc: Int
                    
                    do {
                        rr = Random.nextInt(board.size)
                        cc = Random.nextInt(board.size)
                    }
                    while (!board.isRemoved(rr, cc))
                    
                    board.removeAt(rr, cc)
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
            override fun affectBoard(row: Int, column: Int, color: Color, board: Board) {
                board.gameTime += 10
            }
        },
        
        TWENTY_SECOND {
            override fun affectBoard(row: Int, column: Int, color: Color, board: Board) {
                board.gameTime += 20
            }
        },
        
        THIRTY_SECOND {
            override fun affectBoard(row: Int, column: Int, color: Color, board: Board) {
                board.gameTime += 30
            }
        };
        
        open val score = 10
        
        open fun allowMove(ra: Int, ca: Int, rb: Int, cb: Int) =
            manhattanDistance(ra, ca, rb, cb) == 1
        
        open fun affectBoard(row: Int, column: Int, color: Color, board: Board) = Unit
    }
}