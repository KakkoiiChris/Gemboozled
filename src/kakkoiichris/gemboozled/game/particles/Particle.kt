package kakkoiichris.gemboozled.game.particles

import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.util.math.Vector

/**
 * Gemboozled
 *
 * Copyright (C) 2023, KakkoiiChris
 *
 * File:    Particle.kt
 *
 * Created: Sunday, January 01, 2023, 12:35:59
 *
 * @author Christian Bryce Alexander
 */
abstract class Particle(var position: Vector) : Renderable {
    var removed = false; protected set
}