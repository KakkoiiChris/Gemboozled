package kakkoiichris.gemboozled.game.particles

import kakkoiichris.gemboozled.Resources
import kakkoiichris.gemboozled.game.Gem
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.util.math.tween
import kakkoiichris.hypergame.view.View
import java.awt.Color
import java.awt.Font

/**
 * Gemboozled
 *
 * Copyright (C) 2023, KakkoiiChris
 *
 * File:    Points.kt
 *
 * Created: Wednesday, January 04, 2023, 21:27:55
 *
 * @author Christian Bryce Alexander
 */
class Points(position: Vector, private val points: Int) : Particle(position) {
    private val targetY = position.y - Gem.SIZE / 2.0

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        position.y = position.y.tween(targetY, 0.1, 0.5)

        if (position.y == targetY) {
            removed = true
        }
    }

    override fun render(view: View, renderer: Renderer) {
        renderer.setXORMode(Color.white)

        renderer.font = font

        renderer.drawString("+$points", position)

        renderer.setPaintMode()
    }

    companion object {
        val font = Font(Resources.font, Font.PLAIN, 24)
    }
}