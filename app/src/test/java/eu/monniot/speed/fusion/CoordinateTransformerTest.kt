package eu.monniot.speed.fusion

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class CoordinateTransformerTest {

    @Test
    fun `Identity Rotation - vector remains unchanged`() {
        // Identity rotation vector [0, 0, 0, 1] (no rotation)
        val rotationVector = floatArrayOf(0f, 0f, 0f, 1f)
        val matrix = FloatArray(9)
        CoordinateTransformer.getRotationMatrixFromVector(matrix, rotationVector)

        val input = floatArrayOf(1f, 2f, 3f)
        val output = CoordinateTransformer.transform(input, matrix)

        assertEquals(1f, output[0], 0.0001f)
        assertEquals(2f, output[1], 0.0001f)
        assertEquals(3f, output[2], 0.0001f)
    }

    @Test
    fun `90 degree Z-axis rotation`() {
        // Rotation of 90 degrees around Z axis (Up)
        // q = [0, 0, sin(45), cos(45)] = [0, 0, 0.7071, 0.7071]
        val s = sqrt(0.5f)
        val rotationVector = floatArrayOf(0f, 0f, s, s)
        val matrix = FloatArray(9)
        CoordinateTransformer.getRotationMatrixFromVector(matrix, rotationVector)

        // Input: X=1 (East), Y=0 (North), Z=0 (Up)
        val input = floatArrayOf(1f, 0f, 0f)
        val output = CoordinateTransformer.transform(input, matrix)

        // After 90 deg rotation around Z, East becomes North
        // Wait, check coordinate system. Usually X=East, Y=North.
        // Rotation matrix for 90 deg around Z:
        // [ cos -sin 0 ]   [ 0 -1 0 ]
        // [ sin  cos 0 ] = [ 1  0 0 ]
        // [  0    0  1 ]   [ 0  0 1 ]
        // X_new = -Y_old, Y_new = X_old.
        
        assertEquals(0f, output[0], 0.0001f) // X_new
        assertEquals(1f, output[1], 0.0001f) // Y_new
        assertEquals(0f, output[2], 0.0001f) // Z_new
    }
}
