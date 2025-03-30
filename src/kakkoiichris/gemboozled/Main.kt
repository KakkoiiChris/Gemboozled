package kakkoiichris.gemboozled

import kakkoiichris.gemboozled.game.Game
import kakkoiichris.gemboozled.game.Gem
import kakkoiichris.hypergame.view.Display

/**
 * Gemboozled
 *
 * Copyright (C) 2022, KakkoiiChris
 *
 * File:    Main.kt
 *
 * Created: Wednesday, December 28, 2022, 19:54:54
 *
 * @author Christian Bryce Alexander
 */
fun main() {
    Resources

    val width = (Game.BORDER * 2) + (Gem.SIZE * 10)
    val height = (Game.BORDER * 2) + (Gem.SIZE * 10) + Game.BORDER * 8

    val display = Display(width, height, title = "Gemboozled", icon = Resources.icon)

    display.manager.push(Title)

    display.open()
}