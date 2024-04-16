package kakkoiichris.gemboozled

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
    private var direction = Vector(y = -0.2)

    private var deltaAngle = 0.0

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        position += direction * time.delta
        position.y %= image.height

        direction.x = sin(deltaAngle) * 0.5

        deltaAngle += PI / 128
    }

    override fun render(view: View, renderer: Renderer) {
        val (ox, oy) = position

        for (y in 0..(view.height / Resources.background.height) + 1) {
            for (x in -1..(view.width / Resources.background.width)) {
                renderer.drawImage(
                    image,
                    ((x * image.width) + ox).toInt(),
                    ((y * image.height) + oy).toInt()
                )
            }
        }
    }
}