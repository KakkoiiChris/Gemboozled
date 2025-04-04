package kakkoiichris.gemboozled.ui.menu

import kakkoiichris.hypergame.input.Button
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.ContextRenderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.util.math.tween
import kakkoiichris.hypergame.view.View
import java.awt.Color

sealed class Item : ContextRenderable<Menu> {
    var box = Box()
    var targetHere = 0.0
    var targetAway = 0.0

    var away = false
    var enabled = true
    var visible = true

    override fun update(view: View, context: Menu, manager: StateManager, time: Time, input: Input) {
        val target = if (away) targetAway else targetHere

        box.y = box.y.tween(target, time.delta * 0.2, 0.01)
    }

    fun isHome() =
        away && box.y == targetAway
}

class Button(val text: String, private val action: (View, StateManager, Time, Input) -> Unit) : Item() {
    private var hover = false
    private var press = false

    override fun update(view: View, context: Menu, manager: StateManager, time: Time, input: Input) {
        super.update(view, context, manager, time, input)

        if (enabled) {
            hover = input.mouse in box

            press = hover && input.buttonHeld(Button.LEFT)

            if (hover && input.buttonUp(Button.LEFT)) {
                action(view, manager, time, input)
            }
        }
    }

    override fun render(view: View, context: Menu, renderer: Renderer): Unit = with(renderer) {
        color = when {
            press -> Color(127, 127, 127)
            hover -> Color(255, 255, 255)
            else  -> Color(191, 191, 191)
        }
        fillRoundRect(box, 5, 5)

        color = Color.BLACK
        drawString(text, box)
    }
}

class SubMenu(val text: String, subItems: List<Item>) : Item() {
    val layer = Layer(this)

    private var hover = false
    private var press = false

    init {
        layer += Header.of(this)
        layer += subItems
    }

    override fun update(view: View, context: Menu, manager: StateManager, time: Time, input: Input) {
        super.update(view, context, manager, time, input)

        layer.forEach { it.targetAway = targetHere }

        if (enabled) {
            hover = input.mouse in box

            press = hover && input.buttonHeld(Button.LEFT)

            if (hover && input.buttonUp(Button.LEFT)) {
                context.pushLayer(layer)
            }
        }
    }

    override fun render(view: View, context: Menu, renderer: Renderer): Unit = with(renderer) {
        color = when {
            press -> Color(127, 127, 127)
            hover -> Color(255, 255, 255)
            else  -> Color(191, 191, 191)
        }
        fillRoundRect(box, 5, 5)

        color = Color.BLACK

        drawString(text, box)

        val point = Vector(box.x + 8, box.centerY)

        addVertex(point)
        addVertex(point + Vector(x = 16.0))
        addVertex(point + Vector(8.0, -8.0))

        fillVertices()
        clearVertices()
    }
}

class Header(val text: String) : Item() {
    private var hover = false
    private var press = false

    private var hue = 0.0

    override fun update(view: View, context: Menu, manager: StateManager, time: Time, input: Input) {
        super.update(view, context, manager, time, input)

        if (enabled) {
            hover = input.mouse in box

            press = hover && input.buttonHeld(Button.LEFT)

            if (hover && input.buttonUp(Button.LEFT)) {
                context.popLayer()
            }
        }
        hue += time.delta * 0.005
    }

    override fun render(view: View, context: Menu, renderer: Renderer): Unit = with(renderer) {
        val hsb = Color.getHSBColor(hue.toFloat(), 1F, 0.5F)

        color = when {
            press -> hsb.darker()
            hover -> hsb.brighter()
            else  -> hsb
        }
        fillRoundRect(box, 5, 5)

        color = Color.WHITE

        drawString(text, box)

        val point = Vector(box.x + 8, box.centerY)

        addVertex(point)
        addVertex(point + Vector(x = 16.0))
        addVertex(point + Vector(8.0, 8.0))

        fillVertices()
        clearVertices()
    }

    companion object {
        fun of(subMenu: SubMenu): Header {
            val header = Header(subMenu.text)

            header.box.setBounds(subMenu.box)

            return header
        }
    }
}