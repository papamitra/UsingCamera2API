package sample.usingcamera2api

import android.graphics.Bitmap
import android.media.Image
import android.media.ImageReader
import android.util.Log
import android.util.Size

/**
 * Created by insight on 17/07/29.
 */

class Detector(val previewSize: Size) : ImageReader.OnImageAvailableListener {
    private var computing = false;

    init {
        System.loadLibrary("tensorflow-demo")
    }

    external fun convertYUV420ToARGB8888(yBytes: ByteArray?, uBytes: ByteArray?, vBytes: ByteArray?,
                                         rgbArray: IntArray?, width: Int, height: Int,
                                         yRowStride: Int, uvRowStride: Int, uvPixelStride: Int)

    override fun onImageAvailable(reader: ImageReader?) {
        Log.d("CameraDetection", "call image avalilablelistener")
        if (reader == null) {
            Log.d("CameraDetection", "reader is null")

        } else {
            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
                if (image == null) {
                    Log.d("CameraDetection", "image is null")
                } else {
                    if (computing) {
                        image.close()
                        return
                    }
                    compute(image)
                }
            } catch(e: Exception) {
                if (image != null) {
                    image.close()
                }
                Log.e("CameraDetection", "")
            }
        }
    }

    val yuvBytes: Array<ByteArray?> = arrayOfNulls(3)
    val rgbBytes: IntArray = IntArray(previewSize.width * previewSize.height)

    private val rgbFrameBitmap: Bitmap by lazy {
        Bitmap.createBitmap(previewSize.width, previewSize.height, Bitmap.Config.ARGB_8888)
    }

    private fun compute(image: Image) {

        val planes = image.planes
        fillBytes(planes, yuvBytes)

        val yRowStride = planes[0].rowStride
        val uvRowStride = planes[1].rowStride
        val uvPixelStride = planes[1].pixelStride

        convertYUV420ToARGB8888(yuvBytes[0], yuvBytes[1], yuvBytes[2], rgbBytes,
                previewSize.width, previewSize.height,
                yRowStride, uvRowStride, uvPixelStride)
        image.close()

        rgbFrameBitmap.setPixels(rgbBytes, 0, previewSize.width, 0, 0, previewSize.width, previewSize.height)
        
    }

    companion object {
        private fun fillBytes(planes: Array<Image.Plane>, yuvBytes: Array<ByteArray?>) {
            for(i in 0..planes.size-1) {
                val buffer = planes[i].buffer
                if (yuvBytes[i] == null){
                    yuvBytes[i] = ByteArray(buffer.capacity())
                }
                buffer.get(yuvBytes[i])
            }
        }
    }
    }

}