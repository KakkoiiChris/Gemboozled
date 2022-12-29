package kakkoiichris.gemboozled

import kakkoiichris.hypergame.media.SpriteSheet
import kakkoiichris.hypergame.util.filesystem.ResourceManager

// Christian Alexander, 12/29/2022
object Resources {
    val gems: SpriteSheet
    
    init {
        val manager = ResourceManager("/resources")
        
        val images = manager.getFolder("img")
        //val fonts = manager.getFolder("fnt")
        //val sounds = manager.getFolder("sfx")
        
        val gemsSprite = images.getSprite("gems")
        
        gems = SpriteSheet(gemsSprite, Gem.SIZE, Gem.SIZE)
    }
}