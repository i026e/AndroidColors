package xyz.unance.androidcolornaming

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import io.fotoapparat.result.BitmapPhoto
import java.io.File
import java.io.FileOutputStream
import android.media.ThumbnailUtils
import android.support.annotation.ColorInt
import java.lang.Exception
import kotlin.math.*


typealias DoubleCoordinate = Pair<Double, Double>
typealias RGBColor = Triple<Int, Int, Int>


fun MotionEvent.viewPosition(view: View): DoubleCoordinate{
    val location = IntArray(2)
    view.getLocationOnScreen(location)

    val touchX = min(view.width.toDouble(), max(0.0, this.rawX.toDouble() - location[0]))
    val touchY = min(view.height.toDouble(), max(0.0, this.rawY.toDouble() - location[1]))

    return DoubleCoordinate(touchX, touchY)
}


fun MotionEvent.relativePosition(view: View): DoubleCoordinate{
    val touchPos = this.viewPosition(view)

    val relativeX = touchPos.first / view.width
    val relativeY = touchPos.second / view.height

    return DoubleCoordinate(relativeX, relativeY)
}


fun View.getSizePair(): DoubleCoordinate{
    return DoubleCoordinate(this.width.toDouble(), this.height.toDouble())
}

fun BitmapPhoto.getSizePair(): DoubleCoordinate{
    return DoubleCoordinate(this.bitmap.width.toDouble(), this.bitmap.height.toDouble())
}


fun cropCenter(bmp: Bitmap): Bitmap {
    val dimension = Math.min(bmp.width, bmp.height)
    return ThumbnailUtils.extractThumbnail(bmp, dimension, dimension)
}


fun rotate(vector: DoubleCoordinate, center: DoubleCoordinate, angleDegrees: Double): DoubleCoordinate{
    val angleRadians = angleDegrees * Math.PI / 180.0

    val sinRotation = sin(angleRadians)
    val cosRotation = cos(angleRadians)

    val rotatedX = (cosRotation*vector.first) - (sinRotation*vector.second) - (center.first*cosRotation) + (center.second*sinRotation) + center.first
    val rotatedY = (sinRotation*vector.first) + (cosRotation*vector.second) - (center.first*sinRotation) - (center.second*cosRotation) + center.second

    return Pair(rotatedX, rotatedY)

}


fun rotate(vector: DoubleCoordinate, angleDegrees: Double): DoubleCoordinate{
    val angleRadians = angleDegrees * Math.PI / 180.0

    val sinRotation = sin(angleRadians)
    val cosRotation = cos(angleRadians)

    val rotatedX = (cosRotation*vector.first) - (sinRotation*vector.second)
    val rotatedY = (sinRotation*vector.first) + (cosRotation*vector.second)

    return DoubleCoordinate(rotatedX, rotatedY)

}

operator fun DoubleCoordinate.plus(other: DoubleCoordinate): DoubleCoordinate{
    return DoubleCoordinate(this.first + other.first, this.second + other.second)
}


operator fun DoubleCoordinate.minus(other: DoubleCoordinate): DoubleCoordinate{
    return DoubleCoordinate(this.first - other.first, this.second - other.second)
}


fun DoubleCoordinate.half(): DoubleCoordinate{
    return Pair(this.first/2.0, this.second/2.0)
}


fun toRGB(value: Int): RGBColor{
    return RGBColor(Color.red(value), Color.green(value), Color.blue(value))
}


fun getScale(bitmapPhoto: BitmapPhoto, view: View): Double{
    val rotatedBitmapSize = rotate(
        bitmapPhoto.getSizePair(),
        -bitmapPhoto.rotationDegrees.toDouble()
    )

    return min(abs(rotatedBitmapSize.first)/view.width , abs(rotatedBitmapSize.second)/view.height)
}


fun getTouchPositionOnImage(bitmapPhoto: BitmapPhoto, view: View, motionEvent: MotionEvent): Pair<Int, Int> {
    // position of coursor relative to center of view
    val relativeTouch = motionEvent.viewPosition(view) - view.getSizePair().half()
    // rotate around center, which is (0,0)
    val rotatedRelativeTouch = rotate(relativeTouch, bitmapPhoto.rotationDegrees.toDouble())

    // get scale of bitmap to view
    val scaleCoef = getScale(bitmapPhoto, view)

    // find center of bitmap
    val bitmapCenter = bitmapPhoto.getSizePair().half()

    // find position of touch in bitmap
    val bitmapX = (rotatedRelativeTouch.first * scaleCoef + bitmapCenter.first).toInt()
    val bitmapY = (rotatedRelativeTouch.second * scaleCoef + bitmapCenter.second).toInt()

    return Pair(bitmapX, bitmapY)

}


fun getAverageColor(bitmapPhoto: BitmapPhoto, patchCenter: Pair<Int, Int>, patchSide: Int, debug: Boolean = false): RGBColor{

    val startX = max(0, patchCenter.first - patchSide)
    val endX = min(bitmapPhoto.bitmap.width, patchCenter.first + patchSide)
    val width = endX - startX

    val startY = max(0, patchCenter.second - patchSide)
    val endY = min(bitmapPhoto.bitmap.height, patchCenter.second + patchSide)
    val height = endY - startY

    // load pixels into array
    val pixels = IntArray(width*height)
    bitmapPhoto.bitmap.getPixels(pixels, 0, width, startX, startY, width, height)

    val rComponent = pixels.map { color -> Color.red(color) }.average().toInt()
    val gComponent = pixels.map { color -> Color.green(color) }.average().toInt()
    val bComponent = pixels.map { color -> Color.blue(color) }.average().toInt()


    //
    if (debug) {
        val debugBitmap = bitmapPhoto.bitmap.copy(bitmapPhoto.bitmap.config, true)
        //val debugColor = Color.parseColor("#ffffffff")
        val debugColor = Color.rgb(rComponent, gComponent, bComponent)

        val newPixels = IntArray(width*height){debugColor}

        debugBitmap.setPixels(newPixels, 0, width, startX, startY, width, height)

        try {
            saveTempFile(debugBitmap!!)
        } catch (e: Exception){
            println(e)
        }
    }

    return RGBColor(rComponent, gComponent, bComponent)
}


fun saveTempFile(bitmap: Bitmap) {
    val file = File(Environment.getExternalStorageDirectory().path, "test.png")

    if (file.exists()) {
        file.delete()
    }

    val outStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 85, outStream)
    outStream.close()
}