package xyz.unance.androidcolornaming

import android.content.Context


fun squareColorDistance(a: RGBColor, b: RGBColor): Long{
    val redMean = (a.first + b.first) / 2

    val redDiff = (a.first - b.first).toLong()
    val greenDiff = (a.second - b.second).toLong()
    val blueDiff = (a.third - b.third).toLong()

    return ((512 + redMean) * redDiff * redDiff shr 8) + 4 * greenDiff * greenDiff + ((767 - redMean) * blueDiff * blueDiff shr 8)

}


class ColorMatcher(context: Context) {

    val colorIds = context.resources.getStringArray(R.array.colorIds)
    val colors = context.resources.obtainTypedArray(R.array.colors)

    val colorMap = colorIds.withIndex().map { element -> Pair(element.value, toRGB(colors.getColor(element.index, 0)))}.toMap()
    val colorNames = colorIds.zip(context.resources.getStringArray(R.array.colorNames)).toMap()


    fun findClosestColor(color: RGBColor): Map.Entry<String, RGBColor>{
        return colorMap.minBy { other -> squareColorDistance(color, other.value) }!!
    }

    fun getClosestColorName(color: RGBColor): Pair<String, String> {
        val closestColor = findClosestColor(color)

        return Pair(closestColor.key, colorNames.getValue(closestColor.key))
    }

}