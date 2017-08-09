package sample.usingcamera2api

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

import org.tensorflow.contrib.android.TensorFlowInferenceInterface

class MainActivity : AppCompatActivity() {

    private var cameraDevice : CameraDevice? = null
    private lateinit var captureRequestBuilder : CaptureRequest.Builder
    private var cameraCaptureSession : CameraCaptureSession? = null
    private lateinit var previewSize: Size
    private lateinit var previewReader: ImageReader

    private lateinit var captureThread: HandlerThread
    private lateinit var captureHandler: Handler
    private lateinit var previewRequest: CaptureRequest

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureProgressed(session: CameraCaptureSession?, request: CaptureRequest?, partialResult: CaptureResult?) {
            //super.onCaptureProgressed(session, request, partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession?, request: CaptureRequest?, result: TotalCaptureResult?) {
            //super.onCaptureCompleted(session, request, result)
        }
    }

    private val imageListener by lazy { Detector(previewSize) }

    private val stateCallback: CameraDevice.StateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice?) {
                Log.d("CameraDetection", "Camera onOpened")
                cameraDevice = camera
                cameraDevice?.let { cameraDevice ->
                    val texture = textureView.surfaceTexture!!
                    texture.setDefaultBufferSize(previewSize.width, previewSize.height)
                    val surface = Surface(texture)
                    try {
                        captureRequestBuilder = cameraDevice
                                .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                    captureRequestBuilder.addTarget(surface)

                    captureThread = HandlerThread("ImageListener")
                    captureHandler = with(captureThread) {
                        start()
                        Handler(looper)
                    }
                    previewReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 2)
                    previewReader.setOnImageAvailableListener( imageListener, captureHandler)
                    captureRequestBuilder.addTarget(previewReader.surface)

                    try {
                        cameraDevice.createCaptureSession(listOf(surface, previewReader.surface), previewStateCallback, null)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onError(camera: CameraDevice?, error: Int) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDisconnected(p0: CameraDevice?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
            return true
        }

        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
            openCamera()
        }
    }

    private val previewStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession?) {
            Log.d("CameraDetect", "previewStateCallback::onConfigured()")
            startPreview(session!!)
        }

        override fun onConfigureFailed(p0: CameraCaptureSession?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tensorflow = TensorFlowInferenceInterface(assets, "tiny-yolo-voc.pb")

        textureView.surfaceTextureListener = surfaceTextureListener
    }

    override fun onPause() {
        super.onPause()

        cameraDevice?.close()
        cameraDevice = null
    }

    override fun onResume() {
        super.onResume()

        if (textureView.isAvailable) {
            openCamera()
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            previewSize = map.getOutputSizes(SurfaceTexture::class.java)[0]
            manager.openCamera(cameraId, stateCallback, null)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun startPreview(session: CameraCaptureSession) {
        cameraCaptureSession = session.apply {
            //captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            //captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
            //captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            try {
                previewRequest = captureRequestBuilder.build()
                setRepeatingRequest(previewRequest, captureCallback, captureHandler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    private fun getPictureFile() : File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(System.currentTimeMillis())
        val fileName = "PHOTO_" + timeStamp + ".jpg"
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName)
    }

    protected fun takePicture(view: View) {
        Log.d("CameraDetection", "takcPicture")
        cameraDevice?.let { cameraDevice ->
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
                val configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                configurationMap?.let { configurationMap ->
                    val largest = configurationMap.getOutputSizes(ImageFormat.JPEG).maxWith(CompareSizesByArea())!!
                    val reader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG, 1)
                    val outputSurfaces = ArrayList<Surface>()
                    outputSurfaces.add(reader.surface)
                    outputSurfaces.add(Surface(textureView.surfaceTexture))
                    val captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    captureBuilder.addTarget(reader.surface)
                    captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    val readerListener = object : ImageReader.OnImageAvailableListener {
                        override fun onImageAvailable(reader: ImageReader?) {
                            var image: Image? = null
                            try {
                                image = reader!!.acquireLatestImage()
                                val buffer = image.planes[0].buffer
                                val bytes = ByteArray(buffer.capacity())
                                buffer.get(bytes)
                                val output = FileOutputStream(getPictureFile())
                                output.write(bytes)
                                output.close()
                            } catch(e: FileNotFoundException) {
                                e.printStackTrace()
                            } catch(e: IOException) {
                                e.printStackTrace()
                            } finally {
                                image?.close()
                            }
                        }
                    }
                    val thread = HandlerThread("CameraPicture")
                    thread.start()
                    val backgroundHandler = Handler(thread.looper)
                    reader.setOnImageAvailableListener(readerListener, backgroundHandler)
                    val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(session: CameraCaptureSession?, request: CaptureRequest?, result: TotalCaptureResult?) {
                            super.onCaptureCompleted(session, request, result)
                            Toast.makeText(this@MainActivity, "Picture Saved", Toast.LENGTH_SHORT).show()
                            startPreview(session!!)
                        }
                    }

                    cameraDevice.createCaptureSession(outputSurfaces, object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession?) {
                            try {
                                session!!.capture(captureBuilder.build(),
                                        captureCallback, backgroundHandler)
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(p0: CameraCaptureSession?) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }
                    }, backgroundHandler)
                }
            } catch( e: CameraAccessException){
                e.printStackTrace()
            }
        }
    }
}
