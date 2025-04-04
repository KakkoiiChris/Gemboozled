package kakkoiichris.gemboozled

import kotlin.math.max

fun Double.withCommas() =
    "%,d".format(toInt())

fun Int.withCommas() =
    "%,d".format(this)

fun Double.asTime():String{
    val minutes = max(this / 60, 0.0).toInt()
    val seconds = max(this % 60, 0.0).toInt()
    val microseconds = (max((this % 60) % 1, 0.0) * 100).toInt()

    return "%d:%02d:%02d".format(minutes, seconds, microseconds)
}