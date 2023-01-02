package kakkoiichris.gemboozled

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
interface GameMode {
    val modeName: String
    
    val time: Double
    
    val boardSize: Int
    
    fun getRandomStartGem(x: Double, y: Double): Gem
    
    fun getRandomGem(x: Double, y: Double): Gem
    
    enum class Builtin(override val modeName: String, override val time: Double, override val boardSize: Int) : GameMode {
        CLASSIC("Classic", 120.0, 10) {
            override fun getRandomStartGem(x: Double, y: Double) =
                Gem(x, y, Gem.Color.random(), Gem.Type.BASIC)
            
            override fun getRandomGem(x: Double, y: Double) =
                Gem(x, y, Gem.Color.random(), Gem.Type.BASIC)
        },
        
        TIME_TRIAL("Time Trial", 120.0, 10) {
            override fun getRandomStartGem(x: Double, y: Double) =
                Gem(x, y, Gem.Color.random(), Gem.Type.BASIC)
            
            override fun getRandomGem(x: Double, y: Double): Gem {
                val color = Gem.Color.random()
    
                val type = if (Random.nextDouble() < 0.95) {
                    Gem.Type.BASIC
                }
                else {
                    val t = Random.nextDouble()
        
                    when {
                        t < 1.0 / 2.0 -> Gem.Type.TEN_SECOND
                        t < 5.0 / 6.0 -> Gem.Type.TWENTY_SECOND
                        else          -> Gem.Type.THIRTY_SECOND
                    }
                }
    
                return Gem(x, y, color, type)
            }
        },
        
        CHAOS("Chaos", 120.0, 10) {
            override fun getRandomStartGem(x: Double, y: Double) =
                Gem(x, y, Gem.Color.random(), Gem.Type.BASIC)
            
            override fun getRandomGem(x: Double, y: Double): Gem {
                val color = Gem.Color.random()
                
                val type = if (Random.nextDouble() < 0.85) {
                    Gem.Type.BASIC
                }
                else {
                    val s = Random.nextDouble()
                    
                    when {
                        s < 1.0 / 7.0 -> Gem.Type.CROSS
                        s < 2.0 / 7.0 -> Gem.Type.EXPLODE
                        s < 3.0 / 7.0 -> Gem.Type.SOLE
                        s < 4.0 / 7.0 -> Gem.Type.SCATTER
                        s < 5.0 / 7.0 -> Gem.Type.WARP
                        s < 6.0 / 7.0 -> Gem.Type.BONUS
                        else          -> {
                            val t = Random.nextDouble()
                            
                            when {
                                t < 1.0 / 2.0 -> Gem.Type.TEN_SECOND
                                t < 5.0 / 6.0 -> Gem.Type.TWENTY_SECOND
                                else          -> Gem.Type.THIRTY_SECOND
                            }
                        }
                    }
                }
                
                return Gem(x, y, color, type)
            }
        }
    }
}