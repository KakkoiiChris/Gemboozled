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
import kotlin.random.Random

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
class Points(position: Vector, points: Int) : Particle(position) {
    private val text = "+$points"

    private val targetY = position.y - Gem.SIZE / 2.0

    private var decay = false

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        position.y = position.y.tween(targetY, 0.1, 0.5)

        if (position.y == targetY) {
            decay = true
        }

        if (decay && Random.nextDouble() > 0.8) {
            removed = true
        }
    }

    override fun render(view: View, renderer: Renderer) {
        renderer.setXORMode(Color.white)

        renderer.font = font

        renderer.drawString(text, position)

        renderer.setPaintMode()
    }

    companion object {
        val font = Font(Resources.font, Font.PLAIN, 24)
    }
}