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
import java.awt.Color
import kotlin.math.max
import kotlin.math.pow

// Christian Alexander, 12/28/2022
class Game(val boardSize: Int) : State {
    var gameTime = 120.0
    var totalTime = 0.0
    
    private val boardBounds = Box(BORDER.toDouble(), BORDER.toDouble(), (Gem.SIZE * boardSize).toDouble(), (Gem.SIZE * boardSize).toDouble())
    
    private val gems = mutableListOf<Gem>()
    
    private lateinit var gemFirst: Gem
    private lateinit var gemSecond: Gem
    
    private val pathFirst = Path(Path.Equation.BACK_OUT, Path.Equation.BACK_OUT, Vector(), Vector(), 40.0)
    private val pathSecond = Path(Path.Equation.BACK_OUT, Path.Equation.BACK_OUT, Vector(), Vector(), 40.0)
    
    private val grid = mutableMapOf<String, Gem>()
    
    private val scoredGems = mutableListOf<Gem>()
    
    private var chaining = false
    
    private var score = 0
    private var combo = 0
    private var maxCombo = 0
    
    private var state = GameState.FALL
    
    private val explosions = mutableListOf<Explosion>()
    
    override val name = ID
    
    init {
        fillOffBoard()
    }
    
    private fun fillOffBoard() {
        for (row in 0 until boardSize) {
            for (column in 0 until boardSize) {
                val gem = Gem.random(
                    (column * Gem.SIZE).toDouble() + boardBounds.x,
                    (row * Gem.SIZE).toDouble() + boardBounds.y - boardBounds.height
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
        if (explosions.isNotEmpty()) {
            explosions.forEach { it.update(view, manager, time, input) }
            
            explosions.filter(Explosion::removed).forEach { explosions.remove(it) }
        }
        
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
                Resources.select.update(time)
                
                if (!input.buttonDown(Button.LEFT)) return
                
                for (gem in gems) {
                    if (input.mouse !in gem) continue
                    
                    gemSecond = gem
                    
                    val (ra, ca) = getCoords(gemFirst)
                    val (rb, cb) = getCoords(gemSecond)
                    
                    if (!(gemFirst.type.allowMove(ra, ca, rb, cb) || gemSecond.type.allowMove(ra, ca, rb, cb))) {
                        println("NOPE!")
                        
                        state = GameState.SELECT_FIRST
                        
                        return
                    }
                    
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
                
                state = GameState.VALIDATE
            }
            
            GameState.VALIDATE      -> {
                generateGrid()
                
                val (ra, ca) = getCoords(gemFirst)
                val (rb, cb) = getCoords(gemSecond)
                
                if (!(validateMatch(ra, ca) || validateMatch(rb, cb))) {
                    gems.add(gemSecond)
                    
                    pathFirst.reset()
                    pathSecond.reset()
                    
                    state = GameState.RETURN
                    
                    return
                }
                
                state = GameState.MATCH
            }
            
            GameState.MATCH         -> {
                generateGrid()
                
                if (!matchGems()) {
                    chaining = false
                    
                    maxCombo = max(combo, maxCombo)
                    
                    combo = 0
                    
                    state = GameState.SELECT_FIRST
                }
                
                if (removeGems()) {
                    chaining = true
                    
                    combo++
                    
                    for (gem in scoredGems) {
                        score += gem.type.score * (2.0.pow(combo - 1)).toInt()
                    }
                    
                    scoredGems.clear()
                    
                    gems.removeIf(Gem::removed)
                    
                    generateGrid()
                    
                    for (gem in gems.sortedByDescending { it.y }) {
                        val (row, column) = getCoords(gem)
                        
                        if (row == boardSize - 1) continue
                        
                        val other = get(row + 1, column)
                        
                        if (other == null || other.falling) {
                            gem.falling = true
                        }
                    }
                    
                    for (row in -boardSize..-1) {
                        for (column in 0 until boardSize) {
                            if (get(row, column) != null) continue
                            
                            val gem = Gem.random(
                                (column * Gem.SIZE).toDouble() + boardBounds.x,
                                (row * Gem.SIZE).toDouble() + boardBounds.y - boardBounds.height
                            )
                            
                            gem.falling = true
                            
                            gems.add(gem)
                        }
                    }
                    
                    state = GameState.FALL
                }
            }
            
            GameState.RETURN        -> {
                pathFirst.update(time.delta)
                pathSecond.update(time.delta)
                
                gemFirst.position = pathSecond.getPoint()
                gemSecond.position = pathFirst.getPoint()
                
                if (!(pathFirst.hasElapsed || pathSecond.hasElapsed)) return
                
                state = GameState.SELECT_FIRST
            }
            
            GameState.FALL          -> {
                val steps = 10
                
                val newTime = time / steps * 0.95
                
                repeat(steps) {
                    for (gem in gems) {
                        gem.update(view, manager, newTime, input)
                    }
                    
                    for (thisGem in gems) {
                        if (!thisGem.falling) continue
                        
                        if (thisGem.bottom > boardBounds.bottom) {
                            thisGem.halt()
                            
                            thisGem.bottom = boardBounds.bottom
                        }
                        
                        for (thatGem in gems) {
                            if (thisGem === thatGem) continue
                            
                            if (!thisGem.intersects(thatGem) || thatGem.falling) continue
                            
                            thisGem.halt()
                            
                            thisGem.bottom = thatGem.top
                        }
                    }
                }
                
                if (gems.none { it.falling }) state = GameState.MATCH
            }
        }
    }
    
    private fun generateGrid() {
        grid.clear()
        
        grid.putAll(gems.associateBy {
            val (row, column) = getCoords(it)
            
            "$row,$column"
        })
    }
    
    private fun getCoords(gem: Gem) =
        (gem.y - boardBounds.y).toInt() / Gem.SIZE to (gem.x - boardBounds.x).toInt() / Gem.SIZE
    
    private fun getCoordsString(gem: Gem): String {
        val (row, column) = getCoords(gem)
        
        return getCoordsString(row, column)
    }
    
    private fun getCoordsString(row: Int, column: Int) =
        "$row,$column"
    
    private fun get(row: Int, column: Int) =
        grid[getCoordsString(row, column)]
    
    private fun validateMatch(row: Int, column: Int): Boolean {
        val colorCenter = get(row, column)!!.color
        
        val colorUp = if (row - 1 >= 0)
            get(row - 1, column)?.color
        else null
        
        val colorDown = if (row + 1 < boardSize)
            get(row + 1, column)?.color
        else null
        
        val colorLeft = if (column - 1 >= 0)
            get(row, column - 1)?.color
        else null
        
        val colorRight = if (column + 1 < boardSize)
            get(row, column + 1)?.color
        else null
        
        val color2Up = if (row - 2 >= 0)
            get(row - 2, column)?.color
        else null
        
        val color2Down = if (row + 2 < boardSize)
            get(row + 2, column)?.color
        else null
        
        val color2Left = if (column - 2 >= 0)
            get(row, column - 2)?.color
        else null
        
        val color2Right = if (column + 2 < boardSize)
            get(row, column + 2)?.color
        else null
        
        return when {
            color2Up == colorUp && colorUp == colorCenter          -> true
            colorUp == colorCenter && colorCenter == colorDown     -> true
            colorCenter == colorDown && colorDown == color2Down    -> true
            color2Left == colorLeft && colorLeft == colorCenter    -> true
            colorLeft == colorCenter && colorCenter == colorRight  -> true
            colorCenter == colorRight && colorRight == color2Right -> true
            //cc == GemColor.ALL.ordinal && (cu == cd && cu != -1) -> true
            //cc == GemColor.ALL.ordinal && (cl == cr && cl != -1) -> true
            else                                                   -> false
        }
    }
    
    private fun matchGems(): Boolean {
        var matched = false
        
        for (row in 0 until boardSize) {
            for (column in 0 until boardSize) {
                val colorCenter = get(row, column)?.color ?: continue
                
                val colorUp = if (row - 1 >= 0)
                    get(row - 1, column)?.color
                else null
                
                val colorDown = if (row + 1 < boardSize)
                    get(row + 1, column)?.color
                else null
                
                val colorLeft = if (column - 1 >= 0)
                    get(row, column - 1)?.color
                else null
                
                val colorRight = if (column + 1 < boardSize)
                    get(row, column + 1)?.color
                else null
                
                if (colorUp === colorCenter && colorCenter === colorDown) {
                    matchVertical(row, column, colorCenter)
                    
                    matched = true
                }
                
                if (colorLeft === colorCenter && colorCenter === colorRight) {
                    matchHorizontal(row, column, colorCenter)
                    
                    matched = true
                }
                
                /*
                if (colorCenter == Gem.Color.ALL.ordinal && (colorUp == colorDown && colorUp != -1)) {
                    matchVertical(row, column, colorUp)
                    matched = true
                }
                
                if (colorCenter == Gem.Color.ALL.ordinal && (colorLeft == colorRight && colorLeft != -1)) {
                    matchHorizontal(row, column, colorLeft)
                    matched = true
                }
                */
            }
        }
        
        return matched
    }
    
    private fun matchVertical(r: Int, c: Int, color: Gem.Color) {
        for (i in r downTo 0) {
            val gem = get(i, c)
            
            if (gem?.color == color) {
                removeGem(gem)
                
                gem.affectGame(i, c, this)
            }
            else {
                break
            }
        }
        
        for (i in r + 1 until boardSize) {
            val gem = get(i, c)
            
            if (gem?.color == color) {
                removeGem(gem)
                
                gem.affectGame(i, c, this)
            }
            else {
                break
            }
        }
    }
    
    private fun matchHorizontal(row: Int, column: Int, color: Gem.Color) {
        for (i in column downTo 0) {
            val gem = get(row, i)
            
            if (gem?.color == color) {
                removeGem(gem)
                
                gem.affectGame(row, i, this)
            }
            else {
                break
            }
        }
        
        for (i in column + 1 until boardSize) {
            val gem = get(row, i)
            
            if (gem?.color == color) {
                removeGem(gem)
                
                gem.affectGame(row, i, this)
            }
            else {
                break
            }
        }
    }
    
    private fun removeGem(gem: Gem?) {
        gem?.remove()
        
        explosions += Explosion(gem?.x ?: 0.0, gem?.y ?: 0.0)
    }
    
    private fun removeGems(): Boolean {
        var removed = false
        
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                val gem = get(r, c)
                
                if (gem != null && gem.removed) {
                    scoredGems.add(gem)
                    
                    removed = true
                }
            }
        }
        
        return removed
    }
    
    override fun render(view: View, renderer: Renderer) {
        renderer.color = Colors.black
        renderer.fill(view.bounds.rectangle)
        
        renderer.push()
        renderer.clip = boardBounds.rectangle
        
        renderer.color = Color(255, 255, 255, 63)
        
        for (row in 0 until boardSize) {
            for (column in 0 until boardSize) {
                if ((row + column) % 2 == 0) continue
                
                renderer.fillRect((column * Gem.SIZE) + boardBounds.x.toInt(), (row * Gem.SIZE) + boardBounds.y.toInt(), Gem.SIZE, Gem.SIZE)
            }
        }
        
        gems.forEach { it.render(view, renderer) }
        
        explosions.forEach { it.render(view, renderer) }
        
        renderer.pop()
        
        if (state === GameState.SELECT_SECOND) {
            renderer.drawAnimation(Resources.select, gemFirst.center - 32.0)
        }
        
        renderer.color = Colors.white
        renderer.drawRoundRect(boardBounds, 8, 8)
    }
    
    override fun halt(view: View) {
    }
    
    fun isRemoved(row: Int, column: Int) =
        grid["$row,$column"]?.removed
    
    fun removeAt(row: Int, column: Int) {
        removeGem(grid["$row,$column"])
    }
    
    fun removeIf(predicate: (Gem) -> Boolean) {
        for (gem in gems) {
            if (gem.intersects(boardBounds) && predicate(gem)) {
                removeGem(gem)
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
        VALIDATE,
        MATCH,
        RETURN,
        FALL
    }
}
