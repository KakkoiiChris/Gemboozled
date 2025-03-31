package kakkoiichris.gemboozled

import kakkoiichris.gemboozled.game.Game.Companion.BORDER
import kakkoiichris.gemboozled.game.GameMode
import kakkoiichris.gemboozled.ui.TextBox
import kakkoiichris.gemboozled.ui.menu.Button
import kakkoiichris.gemboozled.ui.menu.Layer
import kakkoiichris.gemboozled.ui.menu.Menu
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.State
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.view.View
import java.awt.Font

class Results(
    val gameMode: GameMode,
    val score: Int,
    val time: Double,
    val combos: Int,
    val maxCombo: Int
) : State {
    private lateinit var header: TextBox

    private lateinit var menu: Menu

    private lateinit var statBoxes:List<Box>

    override fun swapTo(view: View) {
        val headerBox = view.bounds.resized(-BORDER * 2.0).copy(height = BORDER * 2.0)
        val headerFont = Font(Resources.font, Font.PLAIN, BORDER * 3 / 2)

        header = TextBox(headerBox, "GAME OVER", headerFont)

        val infoBox = view.bounds.resized(-BORDER * 8.0)
        val change = headerBox.height + (BORDER * 2)
        infoBox.top += change
        infoBox.height -= change

        val (resultBox, menuBox) = infoBox.divide(2, 1)

        statBoxes = resultBox.divide(5, 1)

        menu = Menu(menuBox)

        val items = listOf(
            Button("Replay") { _, manager, _, _ -> manager.pop() },
            Button("Main Menu") { _, manager, _, _ -> manager.pop(); manager.pop() }
        )

        val topLayer = Layer(null)
        topLayer += items

        menu.push(topLayer)
    }

    override fun swapFrom(view: View) {
    }

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        header.update(view, manager, time, input)

        menu.update(view, manager, time, input)
    }

    override fun render(view: View, renderer: Renderer) {
        header.render(view, renderer)

        renderer.drawString(gameMode.name, statBoxes[0])
        renderer.drawString("SCORE", statBoxes[1], xAlign = 0.0)
        renderer.drawString(score.toString(), statBoxes[1], xAlign = 1.0)
        renderer.drawString("TIME", statBoxes[2], xAlign = 0.0)
        renderer.drawString(time.toString(), statBoxes[2], xAlign = 1.0)
        renderer.drawString("COMBOS", statBoxes[3], xAlign = 0.0)
        renderer.drawString(combos.toString(), statBoxes[3], xAlign = 1.0)
        renderer.drawString("MAX COMBO", statBoxes[4], xAlign = 0.0)
        renderer.drawString(maxCombo.toString(), statBoxes[4], xAlign = 1.0)

        menu.render(view, renderer)
    }

    override fun halt(view: View) {
    }
}