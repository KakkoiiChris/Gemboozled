package kakkoiichris.gemboozled.ui.menu

import kakkoiichris.gemboozled.Resources
import kakkoiichris.gemboozled.game.Game.Companion.BORDER
import kakkoiichris.hypergame.input.Button
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.ContextRenderable
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.util.math.tween
import kakkoiichris.hypergame.view.View
import java.awt.Color
import java.awt.Font

class Menu(private val box: Box) : Renderable {
    private val layers = mutableListOf<Layer>()

    private var lastLayer: Layer? = null

    private val itemFont = Font(Resources.font, Font.PLAIN, BORDER * 2)

    fun push(layer: Layer) {
        val itemHeight = 60.0
        val totalHeight = itemHeight * layer.size
        val itemSpace = (box.height - totalHeight) / (layer.size - 1)

        if (layers.isEmpty()) {
            var y = 0.0

            for (item in layer) {
                item.box.setBounds(0.0, -itemHeight, box.width, itemHeight)

                item.targetHere = y

                y += itemSpace + itemHeight
            }

            layers.push(layer)

            return
        }

        val top = layer.header ?: TODO("BROKEN HEADER")

        var up = true

        for (item in layers.peek()) {
            item.away = true
            item.enabled = false

            item.targetAway = if (up) {
                -itemHeight
            }
            else {
                box.bottom
            }

            if (item === top) {
                item.visible = false
                up = false
            }
        }

        var y = 0.0

        for (item in layer) {
            item.away = false
            item.enabled = true

            item.box.setBounds(top.box)

            item.targetHere = y

            y += itemSpace + itemHeight
        }

        layers.push(layer)
    }

    fun pop() {
        layers.peek().forEach {
            it.away = true
            it.enabled = false
        }

        lastLayer = layers.pop()

        layers.peek().forEach {
            it.away = false
            it.enabled = true
            it.visible = true
        }
    }

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        input.translate(box.position)

        lastLayer?.update(view, this, manager, time, input)
        layers.peek().forEach { it.update(view, this, manager, time, input) }
        layers.peekUp()?.forEach { it.update(view, this, manager, time, input) }

        if (lastLayer?.isHome() == true) {
            lastLayer = null
        }

        input.translate(-box.position)
    }

    override fun render(view: View, renderer: Renderer): Unit = with(renderer) {
        font = itemFont

        withState {
            clip = box.rectangle
            translate(box.position)

            lastLayer?.render(view, this@Menu, renderer)

            layers.peek().reversed().forEach {
                if (it.visible) {
                    it.render(view, this@Menu, renderer)
                }
            }

            layers.peekUp()?.reversed()?.forEach {
                if (it.visible) {
                    it.render(view, this@Menu, renderer)
                }
            }
        }
    }
}

fun <T> MutableList<T>.push(t: T) {
    addFirst(t)
}

fun <T> MutableList<T>.pop(): T {
    return removeFirst()
}

fun <T> MutableList<T>.peek(): T {
    return get(0)
}

fun <T> MutableList<T>.peekUp(): T? {
    return getOrNull(1)
}

class Layer(val header: SubMenu?) : ContextRenderable<Menu>, MutableList<Item> by mutableListOf() {
    override fun update(view: View, context: Menu, manager: StateManager, time: Time, input: Input) {
        forEach { it.update(view, context, manager, time, input) }
    }

    override fun render(view: View, context: Menu, renderer: Renderer) {
        forEach { it.render(view, context, renderer) }
    }

    fun isHome() =
        all { it.isHome() }
}

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
                context.push(layer)
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

        val point = Vector(box.x + 5, box.centerY)

        addVertex(point)
        addVertex(point + Vector(5.0, -5.0))
        addVertex(point + Vector(5.0, 5.0))

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
                context.pop()
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

        val point = Vector(box.x + 5, box.centerY)

        addVertex(point)
        addVertex(point + Vector(5.0, -5.0))
        addVertex(point + Vector(5.0, 5.0))

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