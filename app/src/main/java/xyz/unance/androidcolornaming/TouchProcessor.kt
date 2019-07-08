package xyz.unance.androidcolornaming

import android.os.Handler
import android.view.MotionEvent
import io.fotoapparat.preview.Frame
import io.fotoapparat.util.FrameProcessor
import io.fotoapparat.view.CameraView
import android.os.Looper




class TouchProcessor: FrameProcessor{
    private val MAIN_THREAD_HANDLER = Handler(Looper.getMainLooper())

    var lastFrame: Frame? = null
    var viewSize: Pair<Int, Int>? = null
    var touchPos: Pair<Int, Int>? = null


    fun touch(view: CameraView, event: MotionEvent){
        val currentFrame = lastFrame



    }


    override fun invoke(frame: Frame) {
        lastFrame = frame



        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}