package kakkoiichris.gemboozled

import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.view.View

/**
 * Gemboozled
 *
 * Copyright (C) 2022, KakkoiiChris
 *
 * File:    Board.kt
 *
 * Created: Wednesday, December 28, 2022, 23:50:14
 *
 * @author Christian Bryce Alexander
 */
class Board(val size: Int) : Renderable {
    var gameTime = 120.0
    var totalTime = 0.0
    
    val gems = mutableListOf<Gem>()
    
    init {
        for (y in 0 until size) {
            for (x in 0 until size) {
                gems.add(Gem.random((x * Gem.SIZE).toDouble(), (y * Gem.SIZE).toDouble()))
            }
        }
    }
    
    fun isRemoved(row: Int, column: Int) = true
    
    fun removeAt(row: Int, column: Int) {}
    
    fun removeIf(predicate: (Gem) -> Boolean) {
        for (gem in gems) {
            if (predicate(gem)) {
                gem.removed = true
            }
        }
    }
    
    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        gems.forEach { it.update(view, manager, time, input) }
    }
    
    override fun render(view: View, renderer: Renderer) {
        gems.forEach { it.render(view, renderer) }
    }
}