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
    private var acceleration = Vector(0.0, 0.1)
    
    var falling = false
    
    var removed = false
    
    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        if (falling) {
            velocity += acceleration
            position += velocity * time.delta
        }
    }
    
    override fun render(view: View, renderer: Renderer) {
        renderer.drawSheet(Resources.gems, type.ordinal, color.ordinal, this)
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
    
    fun affectGame(row: Int, column: Int, game: Game) =
        type.affectGame(row, column, color, game)
    
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
    
    enum class Color {
        RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, WHITE;
        
        companion object {
            fun random(random: Random = Random.Default) =
                values()[random.nextInt(values().size)]
        }
    }
    
    enum class Type {
        BASIC,
        
        CROSS {
            override fun affectGame(row: Int, column: Int, color: Color, game: Game) {
                for (i in 0 until game.boardSize) {
                    game.removeAt(row, i)
                    game.removeAt(i, column)
                }
            }
        },
        
        EXPLODE {
            override fun affectGame(row: Int, column: Int, color: Color, game: Game) {
                for (rowOffset in -2..2) {
                    val actualRow = row + rowOffset
                    
                    if (actualRow !in 0 until game.boardSize) continue
                    
                    for (columnOffset in -2..2) {
                        val actualColumn = column + columnOffset
                        
                        if (actualColumn !in 0 until game.boardSize) continue
                        
                        if (manhattanDistance(0, 0, rowOffset, columnOffset) > 2) continue
                        
                        game.removeAt(row + rowOffset, column + columnOffset)
                    }
                }
            }
        },
        
        SOLE {
            override fun affectGame(row: Int, column: Int, color: Color, game: Game) {
                game.removeIf { gem -> color === gem.color }
            }
        },
        
        SCATTER {
            override fun affectGame(row: Int, column: Int, color: Color, game: Game) {
                repeat(15) {
                    var rr: Int
                    var cc: Int
                    
                    do {
                        rr = Random.nextInt(game.boardSize)
                        cc = Random.nextInt(game.boardSize)
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
            override fun affectGame(row: Int, column: Int, color: Color, game: Game) {
                game.gameTime += 10
            }
        },
        
        TWENTY_SECOND {
            override fun affectGame(row: Int, column: Int, color: Color, game: Game) {
                game.gameTime += 20
            }
        },
        
        THIRTY_SECOND {
            override fun affectGame(row: Int, column: Int, color: Color, game: Game) {
                game.gameTime += 30
            }
        };
        
        open val score = 10
        
        open fun allowMove(ra: Int, ca: Int, rb: Int, cb: Int) =
            manhattanDistance(ra, ca, rb, cb) == 1
        
        open fun affectGame(row: Int, column: Int, color: Color, game: Game) = Unit
    }
}