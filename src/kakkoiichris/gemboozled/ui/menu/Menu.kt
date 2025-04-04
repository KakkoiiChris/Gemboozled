package kakkoiichris.gemboozled.ui.menu

import kakkoiichris.gemboozled.Resources
import kakkoiichris.gemboozled.game.Game.Companion.BORDER
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.ContextRenderable
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.view.View
import java.awt.Font

class Menu(private val box: Box) : Renderable {
    private val layers = mutableListOf<Layer>()

    private var lastLayer: Layer? = null

    private val itemFont = Font(Resources.font, Font.PLAIN, BORDER * 2)

    fun pushLayer(layer: Layer) {
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

        val top = layer.header!!

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

    fun popLayer() {
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

    private fun <T> MutableList<T>.push(t: T) {
        addFirst(t)
    }

    private fun <T> MutableList<T>.pop(): T {
        return removeFirst()
    }

    private fun <T> MutableList<T>.peek(): T {
        return get(0)
    }

    private fun <T> MutableList<T>.peekUp(): T? {
        return getOrNull(1)
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