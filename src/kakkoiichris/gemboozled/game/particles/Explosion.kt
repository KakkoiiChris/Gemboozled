package kakkoiichris.gemboozled.game.particles

import kakkoiichris.gemboozled.Resources
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.view.View

/**
 * Gemboozled
 *
 * Copyright (C) 2022, KakkoiiChris
 *
 * File:    Explosion.kt
 *
 * Created: Saturday, December 31, 2022, 22:01:49
 *
 * @author Christian Bryce Alexander
 */
class Explosion(position: Vector) : Particle(position) {
    private val animation = Resources.explosion.copy()

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        animation.update(time)

        if (animation.elapsed) {
            removed = true
        }
    }

    override fun render(view: View, renderer: Renderer) {
        renderer.drawAnimation(animation, position - (animation.frame.size / 2.0))
    }
}