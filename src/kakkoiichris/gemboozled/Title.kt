package kakkoiichris.gemboozled

import kakkoiichris.gemboozled.game.Game
import kakkoiichris.gemboozled.game.Game.Companion.BORDER
import kakkoiichris.gemboozled.game.GameMode
import kakkoiichris.gemboozled.ui.*
import kakkoiichris.gemboozled.ui.menu.Button
import kakkoiichris.gemboozled.ui.menu.Layer
import kakkoiichris.gemboozled.ui.menu.Menu
import kakkoiichris.gemboozled.ui.menu.SubMenu
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.input.Key
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.State
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.view.View
import java.awt.Font

object Title : State {
    private val background = Background(Resources.background)

    private lateinit var title: TextBox

    private lateinit var menu: Menu

    override fun swapTo(view: View) {
        val titleBox = view.bounds.resized(-BORDER * 2.0).copy(height = BORDER * 5.0)
        val titleFont = Font(Resources.font, Font.PLAIN, BORDER * 4)

        title = TextBox(titleBox, "Gemboozled!", titleFont)

        val menuBox = view.bounds.resized(-BORDER * 8.0)
        val change = titleBox.height + (BORDER * 2)
        menuBox.top += change
        menuBox.height -= change

        menu = Menu(menuBox)

        val startSubItems = GameMode.Builtin.entries.map {
            Button(it.modeName) { _, manager, _, _ -> manager.push(Game(it)) }
        }

        val startMenu = SubMenu("Start", startSubItems)


        val items = listOf(
            startMenu,
            Button("Options") { view, manager, time, input -> },
            Button("Credits") { view, manager, time, input -> },
            Button("Quit") { view, manager, time, input -> view.close() }
        )

        val topLayer = Layer(null)
        topLayer += items

        menu.push(topLayer)
    }

    override fun swapFrom(view: View) {
    }

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        background.update(view, manager, time, input)

        title.update(view, manager, time, input)

        menu.update(view, manager, time, input)

        if (input.keyDown(Key.SPACE)) {
            manager.push(Game(GameMode.Builtin.CHAOS))
        }
    }

    override fun render(view: View, renderer: Renderer) {
        background.render(view, renderer)

        title.render(view, renderer)

        menu.render(view, renderer)
        /*
                val last = renderer.composite

                renderer.composite = BlurComposite(2)

                renderer.fillRect(view.width / 4, view.height / 4, view.width / 2, view.height / 2)

                renderer.composite = last
                */
    }

    override fun halt(view: View) {
    }
}