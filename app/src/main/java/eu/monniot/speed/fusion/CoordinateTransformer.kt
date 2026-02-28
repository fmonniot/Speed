package eu.monniot.speed.fusion

import kotlin.math.sqrt

/**
 * Pure Kotlin implementation of coordinate transformations.
 * Decoupled from Android's SensorManager so it can be used in JVM unit tests.
 */
object CoordinateTransformer {

    /** (Taken from Android's SensorManager.getRotationMatrixFromVector as is to be able to test on the JVM)
     *
     * Helper function to convert a rotation vector to a rotation matrix.
     * Given a rotation vector (presumably from a ROTATION_VECTOR sensor), returns a
     * 9  or 16 element rotation matrix in the array R.  R must have length 9 or 16.
     * If R.length == 9, the following matrix is returned:
     * <pre>
     * /  R[ 0]   R[ 1]   R[ 2]   \
     * |  R[ 3]   R[ 4]   R[ 5]   |
     * \  R[ 6]   R[ 7]   R[ 8]   /
    </pre> *
     * If R.length == 16, the following matrix is returned:
     * <pre>
     * /  R[ 0]   R[ 1]   R[ 2]   0  \
     * |  R[ 4]   R[ 5]   R[ 6]   0  |
     * |  R[ 8]   R[ 9]   R[10]   0  |
     * \  0       0       0       1  /
    </pre> *
     * @param rotationVector the rotation vector to convert
     * @param R an array of floats in which to store the rotation matrix
     */
    fun getRotationMatrixFromVector(R: FloatArray, rotationVector: FloatArray) {
        var q0: Float
        val q1 = rotationVector[0]
        val q2 = rotationVector[1]
        val q3 = rotationVector[2]

        if (rotationVector.size >= 4) {
            q0 = rotationVector[3]
        } else {
            q0 = 1 - q1 * q1 - q2 * q2 - q3 * q3
            q0 = if (q0 > 0) sqrt(q0.toDouble()).toFloat() else 0f
        }

        val sq_q1 = 2 * q1 * q1
        val sq_q2 = 2 * q2 * q2
        val sq_q3 = 2 * q3 * q3
        val q1_q2 = 2 * q1 * q2
        val q3_q0 = 2 * q3 * q0
        val q1_q3 = 2 * q1 * q3
        val q2_q0 = 2 * q2 * q0
        val q2_q3 = 2 * q2 * q3
        val q1_q0 = 2 * q1 * q0

        if (R.size == 9) {
            R[0] = 1 - sq_q2 - sq_q3
            R[1] = q1_q2 - q3_q0
            R[2] = q1_q3 + q2_q0

            R[3] = q1_q2 + q3_q0
            R[4] = 1 - sq_q1 - sq_q3
            R[5] = q2_q3 - q1_q0

            R[6] = q1_q3 - q2_q0
            R[7] = q2_q3 + q1_q0
            R[8] = 1 - sq_q1 - sq_q2
        } else if (R.size == 16) {
            R[0] = 1 - sq_q2 - sq_q3
            R[1] = q1_q2 - q3_q0
            R[2] = q1_q3 + q2_q0
            R[3] = 0.0f

            R[4] = q1_q2 + q3_q0
            R[5] = 1 - sq_q1 - sq_q3
            R[6] = q2_q3 - q1_q0
            R[7] = 0.0f

            R[8] = q1_q3 - q2_q0
            R[9] = q2_q3 + q1_q0
            R[10] = 1 - sq_q1 - sq_q2
            R[11] = 0.0f

            R[14] = 0.0f
            R[13] = R[14]
            R[12] = R[13]
            R[15] = 1.0f
        }
    }


    /**
     * Rotates a 3D vector using the provided 3x3 rotation matrix.
     */
    fun transform(vector: FloatArray, matrix: FloatArray): FloatArray {
        val out = FloatArray(3)
        out[0] = matrix[0] * vector[0] + matrix[1] * vector[1] + matrix[2] * vector[2]
        out[1] = matrix[3] * vector[0] + matrix[4] * vector[1] + matrix[5] * vector[2]
        out[2] = matrix[6] * vector[0] + matrix[7] * vector[1] + matrix[8] * vector[2]
        return out
    }
}
