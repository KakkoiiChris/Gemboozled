package kakkoiichris.gemboozled

import kakkoiichris.gemboozled.game.Game
import kakkoiichris.gemboozled.game.Game.Companion.BORDER
import kakkoiichris.gemboozled.game.GameMode
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.input.Key
import kakkoiichris.hypergame.media.BlurComposite
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.State
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.view.View
import java.awt.Font

object Menu : State {
    private val background = Background(Resources.background)

    private lateinit var title: TextBox

    override fun swapTo(view: View) {
        val titleBox = view.bounds.resized(-BORDER * 2.0).copy(height = BORDER * 5.0)
        val titleFont = Font(Resources.font, Font.PLAIN, BORDER * 4)

        title = TextBox(titleBox, "Gemboozled!", titleFont)
    }

    override fun swapFrom(view: View) {
    }

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        background.update(view, manager, time, input)

        title.update(view, manager, time, input)

        if (input.keyDown(Key.SPACE)) {
            manager.push(Game(GameMode.Builtin.TIME_TRIAL))
        }
    }

    override fun render(view: View, renderer: Renderer) {
        background.render(view, renderer)

        title.render(view, renderer)

        val last = renderer.composite

        renderer.composite = BlurComposite(2)

        renderer.fillRect(view.width / 4, view.height / 4, view.width / 2, view.height / 2)

        renderer.composite = last
    }

    override fun halt(view: View) {
    }
}