package eu.monniot.speed.util

object Conversions {
    const val MS_TO_KMH = 3.6f
    const val GRAVITY_MS2 = 9.81f

    fun msToKmh(ms: Float): Float = ms * MS_TO_KMH
    
    fun ms2ToG(ms2: Float): Float = ms2 / GRAVITY_MS2
}
