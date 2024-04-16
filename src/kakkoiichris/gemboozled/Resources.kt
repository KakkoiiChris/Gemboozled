package kakkoiichris.gemboozled

import kakkoiichris.gemboozled.game.Gem
import kakkoiichris.hypergame.media.Animation
import kakkoiichris.hypergame.media.Sprite
import kakkoiichris.hypergame.media.SpriteSheet
import kakkoiichris.hypergame.util.filesystem.ResourceManager
import java.awt.Color

// Christian Alexander, 12/29/2022
object Resources {
    val icon: Sprite

    val background: Sprite

    val gems: SpriteSheet

    val select: Animation

    val x: Sprite

    val explosion: Animation

    val font: String

    val clearBlack = Color(0, 0, 0, 191)
    val clearWhite = Color(255, 255, 255, 63)

    init {
        val manager = ResourceManager("/resources")

        val images = manager.getFolder("img")
        val fonts = manager.getFolder("fnt")
        //val sounds = manager.getFolder("sfx")

        icon = images.getSprite("icon")

        background = images.getSprite("arcade_carpet_512")

        val gemsSprite = images.getSprite("gemsFlat")

        gems = SpriteSheet(gemsSprite, Gem.SIZE, Gem.SIZE)

        val selectSprite = images.getSprite("select")
        val selectSheet = SpriteSheet(selectSprite, 64, 64)

        select = Animation(selectSheet.sprites, 0.05, Animation.Style.LOOP)

        x = images.getSprite("x")

        val explosionSprite = images.getSprite("explode")
        val explosionSheet = SpriteSheet(explosionSprite, 64, 64)

        explosion = Animation(explosionSheet.sprites, 0.025, Animation.Style.ONCE)

        font = fonts.getFont("charybdis")
    }
}