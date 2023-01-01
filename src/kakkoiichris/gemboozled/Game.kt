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
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.util.math.easing.Path
import kakkoiichris.hypergame.view.View
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import kotlin.math.max
import kotlin.math.pow

// Christian Alexander, 12/28/2022
class Game(val boardSize: Int) : State {
    var gameTime = 120.0
    var totalTime = 0.0
    
    private val titleBox = Box(BORDER.toDouble(), BORDER.toDouble(), (Gem.SIZE * boardSize).toDouble(), BORDER * 2.0)
    
    private val boardBox = Box(BORDER.toDouble(), titleBox.bottom + BORDER, (Gem.SIZE * boardSize).toDouble(), (Gem.SIZE * boardSize).toDouble())
    
    private val statsBox = Box(BORDER.toDouble(), boardBox.bottom + BORDER, (Gem.SIZE * boardSize).toDouble(), BORDER * 4.0)
    
    private val timeBox: Box
    private val scoreBox: Box
    private val comboBox: Box
    
    private val gems = mutableListOf<Gem>()
    
    private lateinit var gemFirst: Gem
    private lateinit var gemSecond: Gem
    
    private val pathFirst = Path(Path.Equation.BACK_OUT, Path.Equation.BACK_OUT, Vector(), Vector(), 30.0)
    private val pathSecond = Path(Path.Equation.BACK_OUT, Path.Equation.BACK_OUT, Vector(), Vector(), 30.0)
    
    private val grid = mutableMapOf<String, Gem>()
    
    private val scoredGems = mutableListOf<Gem>()
    
    private val selecting get() = state in listOf(GameState.SELECT_FIRST, GameState.SELECT_SECOND)
    
    private var score = 0
    private var combo = 0
    private var maxCombo = 0
    
    private var state = GameState.FALL
    
    private val explosions = mutableListOf<Explosion>()
    
    private var waitTime = 0.0
    
    private var hue = 0.0
    
    override val name = ID
    
    init {
        fillOffBoard()
        
        val (a, b, c) = statsBox.divide(3, 1)
        
        timeBox = a
        scoreBox = b
        comboBox = c
    }
    
    private fun fillOffBoard() {
        for (row in 0 until boardSize) {
            for (column in 0 until boardSize) {
                val gem = Gem.random(
                    (column * Gem.SIZE).toDouble() + boardBox.x,
                    (row * Gem.SIZE).toDouble() + boardBox.y - boardBox.height
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
        
        Resources.select.update(time)
        
        hue += time.delta * 0.005
        
        gameTime -= time.seconds
        totalTime += time.seconds
        
        if (selecting && gameTime <= 0) {
            state = GameState.GAME_OVER
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
                if (!input.buttonDown(Button.LEFT)) return
                
                for (gem in gems) {
                    if (input.mouse !in gem) continue
                    
                    gemSecond = gem
                    
                    val (ra, ca) = getCoords(gemFirst)
                    val (rb, cb) = getCoords(gemSecond)
                    
                    if (!(gemFirst.type.allowMove(ra, ca, rb, cb) || gemSecond.type.allowMove(ra, ca, rb, cb))) {
                        state = GameState.NO_SWAP
                        
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
                    
                    waitTime = 0.0
                    
                    state = GameState.SELECT_WAIT
                    
                    return
                }
            }
            
            GameState.SELECT_WAIT   -> {
                waitTime += time.seconds
                
                if (waitTime < 0.2) return
                
                state = GameState.SWAP
            }
            
            GameState.NO_SWAP       -> {
                state = GameState.SELECT_FIRST
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
                    maxCombo = max(combo, maxCombo)
                    
                    combo = 0
                    
                    state = GameState.SELECT_FIRST
                }
                
                if (removeGems()) {
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
                                (column * Gem.SIZE).toDouble() + boardBox.x,
                                (row * Gem.SIZE).toDouble() + boardBox.y - boardBox.height
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
                        
                        if (thisGem.bottom > boardBox.bottom) {
                            thisGem.halt()
                            
                            thisGem.bottom = boardBox.bottom
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
            
            GameState.GAME_OVER     -> {
                if (!input.keyDown(Key.ESCAPE)) return
                
                gems.clear()
                
                fillOffBoard()
                
                score = 0
                
                gameTime = 120.0
                totalTime = 0.0
                
                combo = 0
                maxCombo = 0
                
                state = GameState.FALL
            }
        }
    }
    
    private fun generateGrid() {
        grid.clear()
        
        grid.putAll(gems.associateBy(::getCoordsString))
    }
    
    private fun getCoords(gem: Gem) =
        (gem.y - boardBox.y).toInt() / Gem.SIZE to (gem.x - boardBox.x).toInt() / Gem.SIZE
    
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
        
        renderer.color = Color(255, 255, 255, 63)
        
        renderer.fillRoundRect(titleBox, 8, 8)
        renderer.fillRoundRect(timeBox, 8, 8)
        renderer.fillRoundRect(comboBox, 8, 8)
        
        renderer.push()
        renderer.clip = boardBox.rectangle
        
        for (row in 0 until boardSize) {
            for (column in 0 until boardSize) {
                if ((row + column) % 2 == 0) continue
                
                renderer.fillRect((column * Gem.SIZE) + boardBox.x.toInt(), (row * Gem.SIZE) + boardBox.y.toInt(), Gem.SIZE, Gem.SIZE)
            }
        }
        
        gems.forEach { it.render(view, renderer) }
        
        explosions.forEach { it.render(view, renderer) }
        
        renderer.pop()
        
        if (state === GameState.SELECT_FIRST || state === GameState.SELECT_SECOND) {
            //renderer.drawImage(Resources.crosshair, view.input.mouse - 32.0)
        }
        
        if (state === GameState.SELECT_SECOND) {
            renderer.drawAnimation(Resources.select, gemFirst.center - 32.0)
        }
        
        if (state === GameState.SELECT_WAIT) {
            renderer.drawAnimation(Resources.select, gemFirst.center - 32.0)
            
            renderer.drawAnimation(Resources.select, gemSecond.center - 32.0)
        }
        
        renderer.color = Colors.white
        renderer.stroke = BasicStroke(2F)
        
        renderer.drawRoundRect(titleBox, 8, 8)
        renderer.drawRoundRect(boardBox, 8, 8)
        renderer.drawRoundRect(statsBox, 8, 8)
        
        renderer.font = Font(Resources.font, Font.PLAIN, BORDER * 3 / 2)
        
        renderer.drawString("Gemboozled!", titleBox)
        
        renderer.font = Font(Resources.font, Font.PLAIN, BORDER)
        
        val minutes = max(gameTime / 60, 0.0).toInt()
        val seconds = max(gameTime % 60, 0.0).toInt()
        val microseconds = (max((gameTime % 60) % 1, 0.0) * 100).toInt()
        
        val timeString = String.format("Time: %d:%02d:%02d", minutes, seconds, microseconds)
        
        renderer.drawString("TIME", timeBox, xAlign = 0.1)
        renderer.drawString(timeString, timeBox, xAlign = 0.9)
        
        renderer.drawString("SCORE", scoreBox, xAlign = 0.1)
        renderer.drawString("Score: $score", scoreBox, xAlign = 0.9)
        
        renderer.drawString("COMBO", comboBox, xAlign = 0.1)
        renderer.drawString("Combo: $combo / $maxCombo", comboBox, xAlign = 0.9)
        
        if (state == GameState.GAME_OVER) {
            renderer.color = Color(0, 0, 0, 191)
            
            renderer.fillRect(view.bounds)
            
            renderer.color = Colors.white
            renderer.font = Font(Resources.font, Font.PLAIN, BORDER * 4)
            
            renderer.drawString("GAME OVER", view.bounds)
        }
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
            if (gem.intersects(boardBox) && predicate(gem)) {
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
        SELECT_WAIT,
        NO_SWAP,
        SWAP,
        VALIDATE,
        MATCH,
        RETURN,
        FALL,
        GAME_OVER
    }
}
