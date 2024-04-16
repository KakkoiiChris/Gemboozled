package kakkoiichris.gemboozled

import kakkoiichris.gemboozled.game.Game.Companion.BORDER
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Colors
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.view.View
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font

class TextBox(
    val box: Box,
    var text: String,
    val font: Font,
    val align: Vector = Vector(0.5, 0.5)
) : Renderable {
    private var hue = 0.0

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        hue += time.delta * 0.005
    }

    override fun render(view: View, renderer: Renderer) {
        renderer.color = Resources.clearBlack

        renderer.fillRoundRect(box, 8, 8)

        renderer.color = Color(Color.HSBtoRGB(hue.toFloat(), 0.5F, 1F))
        renderer.stroke = BasicStroke(2F)

        renderer.drawRoundRect(box, 8, 8)

        renderer.color = Colors.white
        renderer.font = font

        renderer.drawString(text, box)
    }
}