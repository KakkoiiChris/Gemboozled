package kakkoiichris.gemboozled.game

import kakkoiichris.gemboozled.Resources
import kakkoiichris.gemboozled.game.particles.Explosion
import kakkoiichris.gemboozled.game.particles.Particle
import kakkoiichris.gemboozled.game.particles.Points
import kakkoiichris.gemboozled.game.particles.X
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
import kakkoiichris.hypergame.util.math.tween
import kakkoiichris.hypergame.view.View
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import kotlin.math.max
import kotlin.math.pow

// Christian Alexander, 12/28/2022
class Game(val gameMode: GameMode) : State {
    private val titleBox =
        Box(BORDER.toDouble(), BORDER.toDouble(), (Gem.SIZE * gameMode.boardSize).toDouble(), BORDER * 2.0)
    private val boardBox = Box(
        BORDER.toDouble(),
        titleBox.bottom + BORDER,
        (Gem.SIZE * gameMode.boardSize).toDouble(),
        (Gem.SIZE * gameMode.boardSize).toDouble()
    )
    private val statsBox =
        Box(BORDER.toDouble(), boardBox.bottom + BORDER, (Gem.SIZE * gameMode.boardSize).toDouble(), BORDER * 4.0)

    private val timeBox: Box
    private val scoreBox: Box
    private val comboBox: Box

    private val gems = mutableListOf<Gem>()

    private lateinit var gemFirst: Gem
    private lateinit var gemSecond: Gem

    private var gemHover: Gem? = null

    private val pathFirst = Path(Path.Equation.BACK_OUT, Path.Equation.BACK_OUT, Vector(), Vector(), 30.0)
    private val pathSecond = Path(Path.Equation.BACK_OUT, Path.Equation.BACK_OUT, Vector(), Vector(), 30.0)

    private val grid = mutableMapOf<String, Gem>()

    private val scoredGems = mutableListOf<Gem>()

    private val particles = mutableListOf<Particle>()

    private val selecting get() = state in listOf(GameState.SELECT_FIRST, GameState.SELECT_SECOND)

    var gameTime = gameMode.time
    private var totalTime = 0.0
    private var timePaused = true

    private var score = 0
    private var displayedScore = 0.0

    private var combo = 0
    private var lastCombo = 0
    private var maxCombo = 0

    private var state = GameState.START
    private var waitTime = 0.0


    private val clearBlack = Color(0, 0, 0, 191)
    private val clearWhite = Color(255, 255, 255, 63)
    private var hue = 0.0

    override val name = ID

    init {
        fillOffBoard(gameMode::getRandomStartGem)

        constrainOffBoard()

        val (a, b, c) = statsBox.divide(3, 1).map { it.resized(BORDER * -2.0) }

        timeBox = a
        scoreBox = b
        comboBox = c
    }

    override fun swapTo(view: View, passed: List<Any>) {
    }

    override fun swapFrom(view: View) {
    }

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        if (particles.isNotEmpty()) {
            particles.forEach { it.update(view, manager, time, input) }

            particles.filter(Particle::removed).forEach { particles.remove(it) }
        }

        Resources.select.update(time)

        hue += time.delta * 0.005

        if (!timePaused) {
            gameTime -= time.seconds
            totalTime += time.seconds
        }

        if (selecting && gameTime <= 0) {
            state = GameState.GAME_OVER
        }

        displayedScore = displayedScore.tween(score.toDouble(), 0.1, 0.5)

        when (state) {
            GameState.START         -> updateStart(view, manager, time, input)

            GameState.SELECT_FIRST  -> updateSelectFirst(input)

            GameState.SELECT_SECOND -> updateSelectSecond(input)

            GameState.SELECT_WAIT   -> updateSelectWait(time)

            GameState.NO_SWAP       -> updateNoSwap()

            GameState.SWAP          -> updateSwap(time)

            GameState.VALIDATE      -> updateValidate()

            GameState.MATCH         -> updateMatch()

            GameState.RETURN        -> updateReturn(time)

            GameState.FALL          -> updateFall(view, manager, time, input)

            GameState.FALL_WAIT     -> updateFallWait(time)

            GameState.GAME_OVER     -> updateGameOver(input)
        }
    }

    private fun updateStart(view: View, manager: StateManager, time: Time, input: Input) {
        gemFall(view, manager, time, input)

        if (gems.none { it.falling }) {
            timePaused = false

            state = GameState.SELECT_FIRST
        }
    }

    private fun updateSelectFirst(input: Input) {
        for (gem in gems) {
            if (input.mouse in boardBox && input.mouse in gem) {
                gemHover?.hovering = false

                gemHover = gem

                gemHover?.hovering = true

                break
            }
        }

        if (!input.buttonDown(Button.LEFT)) return

        gemFirst = gemHover ?: return

        state = GameState.SELECT_SECOND
    }

    private fun updateSelectSecond(input: Input) {
        for (gem in gems) {
            if (input.mouse in boardBox && input.mouse in gem) {
                gemHover?.hovering = false

                gemHover = gem

                gemHover?.hovering = true

                break
            }
        }

        if (!input.buttonDown(Button.LEFT)) return

        gemSecond = gemHover ?: return

        gemHover?.hovering = false
        gemHover = null

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

        gemHover = null

        state = GameState.SELECT_WAIT
    }

    private fun updateSelectWait(time: Time) {
        waitTime += time.seconds

        if (waitTime < 0.3) return

        state = GameState.SWAP
    }

    private fun updateNoSwap() {
        particles.add(X(gemFirst.center))
        particles.add(X(gemSecond.center))

        state = GameState.SELECT_FIRST
    }

    private fun updateSwap(time: Time) {
        pathFirst.update(time.delta)
        pathSecond.update(time.delta)

        gemFirst.position = pathFirst.getPoint()
        gemSecond.position = pathSecond.getPoint()

        if (!(pathFirst.hasElapsed || pathSecond.hasElapsed)) return

        state = GameState.VALIDATE
    }

    private fun updateValidate() {
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

    private fun updateMatch() {
        generateGrid()

        if (!matchGems()) {
            lastCombo = combo

            maxCombo = max(combo, maxCombo)

            combo = 0

            state = GameState.SELECT_FIRST
        }

        if (clearRemovedGems()) {
            combo++

            for (gem in scoredGems) {
                val points = gem.type.score * (2.0.pow(combo - 1)).toInt()

                score += points

                particles += Points(gem.position, points)
            }

            scoredGems.clear()

            gems.removeIf(Gem::removed)

            generateGrid()

            for (gem in gems.sortedByDescending { it.y }) {
                val (row, column) = getCoords(gem)

                if (row == gameMode.boardSize - 1) continue

                val other = get(row + 1, column)

                if (other == null || other.falling) {
                    gem.falling = true
                }
            }

            fillOffBoard(gameMode::getRandomGem)

            state = GameState.FALL
        }
    }

    private fun updateReturn(time: Time) {
        pathFirst.update(time.delta)
        pathSecond.update(time.delta)

        gemFirst.position = pathSecond.getPoint()
        gemSecond.position = pathFirst.getPoint()

        if (!(pathFirst.hasElapsed || pathSecond.hasElapsed)) return

        state = GameState.SELECT_FIRST
    }

    private fun updateFall(view: View, manager: StateManager, time: Time, input: Input) {
        gemFall(view, manager, time, input)

        if (gems.none { it.falling }) {
            waitTime = 0.0

            state = GameState.FALL_WAIT
        }
    }

    private fun updateFallWait(time: Time) {
        waitTime += time.seconds

        if (waitTime < 0.3) return

        state = GameState.MATCH
    }

    private fun updateGameOver(input: Input) {
        if (!input.keyDown(Key.ESCAPE)) return

        gems.clear()

        fillOffBoard(gameMode::getRandomStartGem)

        constrainOffBoard()

        score = 0

        gameTime = gameMode.time
        totalTime = 0.0
        timePaused = true

        combo = 0
        maxCombo = 0

        state = GameState.START
    }

    override fun render(view: View, renderer: Renderer) {
        for (y in 0..(view.height / Resources.background.height)) {
            for (x in 0..(view.width / Resources.background.width)) {
                renderer.drawImage(
                    Resources.background,
                    x * Resources.background.width,
                    y * Resources.background.height
                )
            }
        }

        renderer.color = clearBlack

        renderer.fillRoundRect(titleBox, 8, 8)
        renderer.fillRoundRect(statsBox, 8, 8)

        renderer.push()
        renderer.clip = boardBox.rectangle

        for (row in 0 until gameMode.boardSize) {
            for (column in 0 until gameMode.boardSize) {
                renderer.color = if ((row + column) % 2 == 0) clearBlack else clearWhite

                renderer.fillRect(
                    (column * Gem.SIZE) + boardBox.x.toInt(),
                    (row * Gem.SIZE) + boardBox.y.toInt(),
                    Gem.SIZE,
                    Gem.SIZE
                )
            }
        }

        gems.forEach { it.render(view, renderer) }

        renderer.pop()

        particles.forEach { it.render(view, renderer) }

        if (state === GameState.SELECT_SECOND) {
            renderer.drawAnimation(Resources.select, gemFirst.center - 32.0)
        }

        if (state === GameState.SELECT_WAIT) {
            renderer.drawAnimation(Resources.select, gemFirst.center - 32.0)

            renderer.drawAnimation(Resources.select, gemSecond.center - 32.0)
        }

        renderer.color = Color(Color.HSBtoRGB(hue.toFloat(), 0.5F, 1F))
        renderer.stroke = BasicStroke(2F)

        renderer.drawRoundRect(titleBox, 8, 8)
        renderer.drawRoundRect(boardBox, 8, 8)
        renderer.drawRoundRect(statsBox, 8, 8)

        renderer.color = Colors.white
        renderer.font = Font(Resources.font, Font.PLAIN, BORDER * 3 / 2)

        renderer.drawString("Gemboozled ${gameMode.modeName}!", titleBox)

        renderer.font = Font(Resources.font, Font.PLAIN, BORDER)

        val minutes = max(gameTime / 60, 0.0).toInt()
        val seconds = max(gameTime % 60, 0.0).toInt()
        val microseconds = (max((gameTime % 60) % 1, 0.0) * 100).toInt()

        val timeString = "%d:%02d:%02d".format(minutes, seconds, microseconds)

        renderer.drawString("TIME", timeBox, xAlign = 0.0)
        renderer.drawString(timeString, timeBox, xAlign = 1.0)

        val scoreString = "%,d".format(displayedScore.toInt())

        renderer.drawString("SCORE", scoreBox, xAlign = 0.0)
        renderer.drawString(scoreString, scoreBox, xAlign = 1.0)

        val comboString = "$combo / $lastCombo / $maxCombo"

        renderer.drawString("COMBO", comboBox, xAlign = 0.0)
        renderer.drawString(comboString, comboBox, xAlign = 1.0)

        if (state == GameState.GAME_OVER) {
            renderer.color = clearBlack

            renderer.fillRect(view.bounds)

            renderer.color = Colors.white
            renderer.font = Font(Resources.font, Font.PLAIN, BORDER * 4)

            renderer.drawString("GAME OVER", view.bounds, yAlign = 0.05)
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

    private fun fillOffBoard(getter: (Double, Double) -> Gem) {
        generateGrid()

        for (row in -gameMode.boardSize until 0) {
            for (column in 0 until gameMode.boardSize) {
                if (get(row, column) != null) continue

                val gem = getter(
                    (column * Gem.SIZE).toDouble() + boardBox.x,
                    (row * Gem.SIZE).toDouble() + boardBox.y
                )

                gem.falling = true

                gems.add(gem)
            }
        }
    }

    private fun constrainOffBoard() {
        generateGrid()

        for (row in -gameMode.boardSize until 0) {
            for (column in 0 until gameMode.boardSize) {
                val gem = get(row, column)!!

                val colorCenter = gem.color
                val colorUp = if (row - 1 >= 0) get(row - 1, column)?.color else null
                val colorDown = if (row + 1 < gameMode.boardSize) get(row + 1, column)?.color else null
                val colorLeft = if (column - 1 >= 0) get(row, column - 1)?.color else null
                val colorRight = if (column + 1 < gameMode.boardSize) get(row, column + 1)?.color else null

                if (colorUp == colorCenter && colorCenter == colorDown || colorLeft == colorCenter && colorCenter == colorRight) {
                    var newColor: Gem.Color

                    do {
                        newColor = Gem.Color.random()
                    }
                    while (newColor == colorCenter)

                    val newGem = Gem(gem.x, gem.y, newColor, gem.type)
                    newGem.falling = gem.falling

                    val index = gems.indexOf(gem)

                    gems.removeAt(index)
                    gems.add(index, newGem)

                    set(row, column, newGem)
                }
            }
        }
    }

    private fun gemFall(view: View, manager: StateManager, time: Time, input: Input) {
        val newTime = time / PHYSICS_STEPS

        repeat(PHYSICS_STEPS) {
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

    private fun set(row: Int, column: Int, gem: Gem) {
        grid[getCoordsString(row, column)] = gem
    }

    private fun validateMatch(row: Int, column: Int): Boolean {
        val colorCenter = get(row, column)!!.color

        val colorUp = if (row - 1 >= 0)
            get(row - 1, column)?.color
        else null

        val colorDown = if (row + 1 < gameMode.boardSize)
            get(row + 1, column)?.color
        else null

        val colorLeft = if (column - 1 >= 0)
            get(row, column - 1)?.color
        else null

        val colorRight = if (column + 1 < gameMode.boardSize)
            get(row, column + 1)?.color
        else null

        val color2Up = if (row - 2 >= 0)
            get(row - 2, column)?.color
        else null

        val color2Down = if (row + 2 < gameMode.boardSize)
            get(row + 2, column)?.color
        else null

        val color2Left = if (column - 2 >= 0)
            get(row, column - 2)?.color
        else null

        val color2Right = if (column + 2 < gameMode.boardSize)
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

        for (row in 0 until gameMode.boardSize) {
            for (column in 0 until gameMode.boardSize) {
                val colorCenter = get(row, column)?.color ?: continue

                val colorUp = if (row - 1 >= 0)
                    get(row - 1, column)?.color
                else null

                val colorDown = if (row + 1 < gameMode.boardSize)
                    get(row + 1, column)?.color
                else null

                val colorLeft = if (column - 1 >= 0)
                    get(row, column - 1)?.color
                else null

                val colorRight = if (column + 1 < gameMode.boardSize)
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

                gem.affectGame(this, i, c)
            }
            else {
                break
            }
        }

        for (i in r + 1 until gameMode.boardSize) {
            val gem = get(i, c)

            if (gem?.color == color) {
                removeGem(gem)

                gem.affectGame(this, i, c)
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

                gem.affectGame(this, row, i)
            }
            else {
                break
            }
        }

        for (i in column + 1 until gameMode.boardSize) {
            val gem = get(row, i)

            if (gem?.color == color) {
                removeGem(gem)

                gem.affectGame(this, row, i)
            }
            else {
                break
            }
        }
    }

    private fun removeGem(gem: Gem?) {
        if (gem == null) return

        gem.remove()

        particles += Explosion(gem.center)
    }

    private fun clearRemovedGems(): Boolean {
        var removed = false

        for (r in 0 until gameMode.boardSize) {
            for (c in 0 until gameMode.boardSize) {
                val gem = get(r, c)

                if (gem != null && gem.removed) {
                    scoredGems.add(gem)

                    removed = true
                }
            }
        }

        return removed
    }

    companion object {
        const val ID = "game"
        const val BORDER = 25
        const val PHYSICS_STEPS = 8
    }

    enum class GameState {
        START,
        SELECT_FIRST,
        SELECT_SECOND,
        SELECT_WAIT,
        NO_SWAP,
        SWAP,
        VALIDATE,
        MATCH,
        RETURN,
        FALL,
        FALL_WAIT,
        GAME_OVER
    }
}
