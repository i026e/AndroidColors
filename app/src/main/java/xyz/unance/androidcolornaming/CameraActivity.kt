package xyz.unance.androidcolornaming

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.configuration.UpdateConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.activity_camera.*
import java.lang.Exception


class CameraActivity : AppCompatActivity() {
    enum class FotoapparatState{
        ON, OFF
    }
    enum class FlashState{
        TORCH, OFF
    }

    val permissions = arrayOf(
        //android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        //android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.CAMERA
    )
    val cameraConfiguration = CameraConfiguration(
        pictureResolution = lowestResolution(),
        previewResolution = highestResolution()
    )

    var colorMatcher: ColorMatcher? = null

    var fotoapparat: Fotoapparat? = null
    var fotoapparatState : FotoapparatState? = null
    var flashState: FlashState? = null


    private fun hasPermissions(): Boolean{

        return permissions
            .map { permission -> ContextCompat.checkSelfPermission(this, permission) }
            .all { granted -> granted == PackageManager.PERMISSION_GRANTED }
    }

    fun requestPermission(){
        ActivityCompat.requestPermissions(this, permissions,0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        recreate()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        createFotoapparat()

        fab_flash.setOnClickListener {
            changeFlashState()
        }

        camera_view.setOnTouchListener { view: View, event: MotionEvent ->
            val capture = event.action == MotionEvent.ACTION_DOWN
            if (capture) {
                captureColor(view, event)
            }
            capture
        }

        // init matcher
        colorMatcher = ColorMatcher(applicationContext)

    }

    override fun onStop() {
        super.onStop()
        fotoapparat?.stop()
        fotoapparatState = FotoapparatState.OFF
    }

    override fun onStart() {
        super.onStart()

        if (!hasPermissions()) {
            requestPermission()
        }

        fotoapparat?.start()
        fotoapparatState = FotoapparatState.ON

    }

    override fun onResume() {
        super.onResume()

        if(hasPermissions() && fotoapparatState == FotoapparatState.OFF){
            val intent = Intent(baseContext, CameraActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun createFotoapparat(){
        val cameraView = findViewById<CameraView>(R.id.camera_view)

        fotoapparat = Fotoapparat(
            context = this,
            view = cameraView,
            scaleType = ScaleType.CenterCrop,
            lensPosition = back(),
            logger = loggers(
                logcat()
            ),
            cameraConfiguration = cameraConfiguration,
            cameraErrorCallback = { error ->
                println("Recorder errors: $error")
            }
        )
    }

    private fun changeFlashState() {
        fotoapparat?.updateConfiguration(
            UpdateConfiguration(
                flashMode = if(flashState == FlashState.TORCH) off() else torch()
            )
        )

        if(flashState == FlashState.TORCH) flashState = FlashState.OFF
        else flashState = FlashState.TORCH
    }

    private fun playSound(soundId: String){
        var sound = resources.getIdentifier(soundId.toLowerCase(), "raw", packageName)
        if (sound == 0) sound = R.raw.unknown

        try {
            val mediaPlayer: MediaPlayer? = MediaPlayer.create(applicationContext, sound)
            mediaPlayer?.start() // no need to call prepare(); create() does that for you

        } catch (e: Exception){
            println(e)
            Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun captureColor(v: View, event: MotionEvent) {

        if (!hasPermissions()) {
            requestPermission()
        } else if (fotoapparat == null) {
            Toast.makeText(applicationContext, R.string.camera_not_available, Toast.LENGTH_SHORT).show()
        } else if (colorMatcher == null) {
            Toast.makeText(applicationContext, R.string.color_matcher_not_ready, Toast.LENGTH_SHORT).show()
        } else {
            val photoResult = fotoapparat!!.takePicture().toBitmap()
            photoResult.whenAvailable {
                    bitmapPhoto ->

                val touchCenter = getTouchPositionOnImage(bitmapPhoto!!, v, event)
                val color = getAverageColor(bitmapPhoto, touchCenter, 10)
                val knownColor = colorMatcher!!.getClosestColorName(color)

                /*val text = bitmapPhoto?.bitmap?.height.toString() + "x" + bitmapPhoto?.bitmap?.width.toString() + " " + bitmapPhoto?.rotationDegrees +
                        " " + v.width.toString() + "x" + v.height.toString() +
                        " " + viewX.toString() + "x" + viewY.toString()*/

                Toast.makeText(applicationContext, knownColor.second, Toast.LENGTH_LONG).show()
                playSound(knownColor.first)

                //bitmapPhoto?.bitmap?.
            }

        }
    }

}
