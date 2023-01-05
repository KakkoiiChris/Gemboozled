package kakkoiichris.gemboozled.game.particles

import kakkoiichris.gemboozled.Resources
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.view.View
import java.awt.AlphaComposite

/**
 * Gemboozled
 *
 * Copyright (C) 2023, KakkoiiChris
 *
 * File:    X.kt
 *
 * Created: Sunday, January 01, 2023, 15:36:56
 *
 * @author Christian Bryce Alexander
 */
class X(position: Vector) : Particle(position) {
    private var alpha = 1.0
    
    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        alpha -= time.delta * 0.05
        
        if (alpha <= 0.0) {
            removed = true
        }
    }
    
    override fun render(view: View, renderer: Renderer) {
        renderer.push()
        
        renderer.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.toFloat())
        
        renderer.drawImage(Resources.x, position - (Resources.x.size / 2.0))
        
        renderer.pop()
    }
}