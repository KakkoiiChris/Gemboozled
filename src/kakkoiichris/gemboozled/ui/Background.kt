package kakkoiichris.gemboozled.ui

import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.view.View
import java.awt.image.BufferedImage
import kotlin.math.PI
import kotlin.math.sin

class Background(private val image: BufferedImage) : Renderable {
    private var position = Vector()
    private var direction = Vector(y = -0.5)

    private var deltaAngle = 0.0

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        position += direction * time.delta
        position.y %= image.height

        direction.x = sin(deltaAngle) * 0.5

        deltaAngle += PI / 128
    }

    override fun render(view: View, renderer: Renderer) = renderer.withState {
        translate(view.bounds.center)

        rotate(sin(deltaAngle * 0.25) * 0.25)

        translate(position)

        for (ty in -2..2) {
            for (tx in -2..<2) {
                drawImage(image, tx * image.width, ty * image.height)
            }
        }
    }
}