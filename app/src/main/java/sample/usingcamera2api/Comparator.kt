package sample.usingcamera2api

import android.util.Size

/**
 * Created by insight on 17/07/16.
 */
class CompareSizesByArea : Comparator<Size> {
    override fun compare(lhs: Size?, rhs: Size?): Int =
            Math.signum((lhs!!.width * lhs!!.height - rhs!!.width * rhs!!.height).toDouble()).toInt()

}