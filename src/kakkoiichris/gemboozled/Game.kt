package kakkoiichris.gemboozled

import kakkoiichris.hypergame.input.Button
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Colors
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.State
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.util.math.easing.Path
import kakkoiichris.hypergame.view.View

// Christian Alexander, 12/28/2022
class Game(val boardSize: Int) : State {
    var gameTime = 120.0
    var totalTime = 0.0
    
    private val boardBounds = Box(BORDER.toDouble(), BORDER.toDouble(), (Gem.SIZE * boardSize).toDouble(), (Gem.SIZE * boardSize).toDouble())
    
    private val gems = mutableListOf<Gem>()
    
    private lateinit var gemFirst: Gem
    private lateinit var gemSecond: Gem
    
    private val pathFirst = Path(Path.Equation.CUBIC_BOTH, Path.Equation.CUBIC_BOTH, Vector(), Vector(), 30.0)
    private val pathSecond = Path(Path.Equation.CUBIC_BOTH, Path.Equation.CUBIC_BOTH, Vector(), Vector(), 30.0)
    
    private lateinit var grid: Array<Array<Gem>>
    
    private var state = GameState.FALL
    
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
        when (state) {
            GameState.SELECT_FIRST  -> {
                if (!input.buttonDown(Button.LEFT)) return
                
                for (gem in gems) {
                    if (input.mouse !in gem) continue
                    
                    gemFirst = gem
                    
                    state = GameState.SELECT_SECOND
                    
                    return
                }
            }
            
            GameState.SELECT_SECOND -> {
                if (!input.buttonDown(Button.LEFT)) return
                
                for (gem in gems) {
                    if (input.mouse !in gem) continue
                    
                    gemSecond = gem
    
                    gems.add(gemSecond)
                    gems.add(gemFirst)
                    
                    pathFirst.reset()
                    pathFirst.start = gemFirst.position
                    pathFirst.end = gemSecond.position
                    
                    pathSecond.reset()
                    pathSecond.start = gemSecond.position
                    pathSecond.end = gemFirst.position
                    
                    state = GameState.SWAP
                    
                    return
                }
            }
            
            GameState.SWAP          -> {
                pathFirst.update(time.delta)
                pathSecond.update(time.delta)
                
                gemFirst.position = pathFirst.getPoint()
                gemSecond.position = pathSecond.getPoint()
                
                if (!(pathFirst.hasElapsed || pathSecond.hasElapsed)) return
                
                state = GameState.MATCH
            }
            
            GameState.MATCH -> {
                gems.add(gemSecond)
                
                pathFirst.reset()
                pathSecond.reset()
                
                state = GameState.RETURN
            }
            
            GameState.RETURN        -> {
                pathFirst.update(time.delta)
                pathSecond.update(time.delta)
    
                gemFirst.position = pathSecond.getPoint()
                gemSecond.position = pathFirst.getPoint()
    
                if (!(pathFirst.hasElapsed || pathSecond.hasElapsed)) return
                
                state = GameState.SELECT_FIRST
            }
            
            GameState.FALL -> {
                val steps = 5
                
                val newTime = time / steps
                
                repeat(steps) {
                    for (gem in gems) {
                        gem.update(view, manager, newTime, input)
                    }
                    
                    gems.removeIf(Gem::removed)
                    
                    for (thisGem in gems) {
                        if (!thisGem.falling) continue
                        
                        if (thisGem.bottom > boardBounds.bottom) {
                            thisGem.halt()
                            
                            thisGem.bottom = boardBounds.bottom
                        }
                        
                        for (thatGem in gems) {
                            if (thisGem === thatGem) continue
                            
                            if (!thisGem.intersects(thatGem) || thatGem.falling) continue
                            
                            thisGem.halt(thatGem)
                            
                            thisGem.bottom = thatGem.top
                        }
                    }
                }
                
                if (gems.none{it.falling}) state = GameState.SELECT_FIRST
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
    
    enum class GameState {
        SELECT_FIRST,
        SELECT_SECOND,
        SWAP,
        MATCH,
        RETURN,
        FALL
    }
}
