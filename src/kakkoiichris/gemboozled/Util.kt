package kakkoiichris.gemboozled

import kotlin.math.abs

/**
 * Gemboozled
 *
 * Copyright (C) 2022, KakkoiiChris
 *
 * File:    Util.kt
 *
 * Created: Wednesday, December 28, 2022, 23:52:42
 *
 * @author Christian Bryce Alexander
 */

fun manhattanDistance(ra: Int, ca: Int, rb: Int, cb: Int) =
    abs(ra - rb) + abs(ca - cb)