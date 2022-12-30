package kakkoiichris.gemboozled

import kakkoiichris.hypergame.input.Button
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.input.Key
import kakkoiichris.hypergame.media.Colors
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.State
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.view.View

// Christian Alexander, 12/28/2022
class Game(val boardSize: Int) : State {
    var gameTime = 120.0
    var totalTime = 0.0
    
    val boardBounds = Box(BORDER.toDouble(), BORDER.toDouble(), (Gem.SIZE * boardSize).toDouble(), (Gem.SIZE * boardSize).toDouble())
    
    val gems = mutableListOf<Gem>()
    
    override val name = ID
    
    init {
        for (y in 0 until boardSize) {
            for (x in 0 until boardSize) {
                val gem = Gem.random(
                    (x * Gem.SIZE).toDouble() + boardBounds.x,
                    (y * Gem.SIZE).toDouble() + boardBounds.y - boardBounds.height
                )
                
                gem.falling = true
                
                gems.add(gem)
            }
        }
    }
    
    override fun swapTo(view: View, passed: List<Any>) {
    }
    
    override fun swapFrom(view: View) {
    }
    
    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        val steps = 5
        
        val newTime = time / steps
        
        repeat(steps) {
            for (gem in gems) {
                gem.update(view, manager, newTime, input)
            }
            
            gems.removeIf(Gem::removed)
            
            for (thisGem in gems) {
                if (thisGem.falling) {
                    if (thisGem.bottom > boardBounds.bottom) {
                        thisGem.halt()
                        thisGem.bottom = boardBounds.bottom
                    }
                    
                    for (thatGem in gems) {
                        if (thisGem === thatGem) continue
                        
                        if (thisGem.intersects(thatGem) && !thatGem.falling) {
                            thisGem.halt(thatGem)
                            thisGem.bottom = thatGem.top
                        }
                    }
                }
            }
        }
    }
    
    override fun render(view: View, renderer: Renderer) {
        renderer.color = Colors.black
        renderer.fill(view.bounds.rectangle)
        
        renderer.push()
        renderer.clip = boardBounds.rectangle
        
        gems.forEach { it.render(view, renderer) }
        
        renderer.pop()
        
        renderer.color = Colors.white
        renderer.drawRoundRect(boardBounds, 8, 8)
    }
    
    override fun halt(view: View) {
    }
    
    fun isRemoved(row: Int, column: Int) = false
    
    fun removeAt(row: Int, column: Int) {
    
    }
    
    fun removeIf(predicate: (Gem) -> Boolean) {
        for (gem in gems) {
            if (predicate(gem)) {
                gem.removed = true
            }
        }
    }
    
    companion object {
        const val ID = "game"
        const val BORDER = 25
    }
}
