package kakkoiichris.gemboozled

import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
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
class Explosion(x: Double, y: Double) : Box(x, y, SIZE, SIZE), Renderable {
    private val animation = Resources.explosion.copy()
    
    var removed = false; private set
    
    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        animation.update(time)
        
        if (animation.elapsed) {
            removed = true
        }
    }
    
    override fun render(view: View, renderer: Renderer) {
        renderer.drawAnimation(animation, this)
    }
    
    companion object {
        const val SIZE = 51.0
    }
}