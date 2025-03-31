package kakkoiichris.gemboozled

import kakkoiichris.gemboozled.game.Game
import kakkoiichris.gemboozled.game.Gem
import kakkoiichris.gemboozled.ui.Background
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.view.Display
import kakkoiichris.hypergame.view.View

/**
 * Gemboozled
 *
 * Copyright (C) 2022, KakkoiiChris
 *
 * File:    Main.kt
 *
 * Created: Wednesday, December 28, 2022, 19:54:54
 *
 * @author Christian Bryce Alexander
 */
fun main() {
    Resources

    val width = (Game.BORDER * 2) + (Gem.SIZE * 10)
    val height = (Game.BORDER * 2) + (Gem.SIZE * 10) + Game.BORDER * 8

    val display = Display(width, height, title = "Gemboozled", icon = Resources.icon)

    display.preRenderable = Backdrop

    display.manager.push(Title)

    display.open()
}

private object Backdrop : Renderable {
    private val background = Background(Resources.background)

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        background.update(view, manager, time, input)
    }

    override fun render(view: View, renderer: Renderer) {
        background.render(view, renderer)
    }
}