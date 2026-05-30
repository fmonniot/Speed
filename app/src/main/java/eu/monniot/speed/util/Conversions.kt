package eu.monniot.speed.util

object Conversions {
    const val MS_TO_KMH = 3.6f
    const val GRAVITY_MS2 = 9.81f

    // Imperial conversion factors
    const val MS_TO_MPH = 2.2369363f
    const val KM_TO_MI = 0.621371f
    const val M_TO_FT = 3.2808399f

    fun msToKmh(ms: Float): Float = ms * MS_TO_KMH

    fun ms2ToG(ms2: Float): Float = ms2 / GRAVITY_MS2

    // SI → Imperial display units
    fun msToMph(ms: Float): Float = ms * MS_TO_MPH
    fun kmToMi(km: Float): Float = km * KM_TO_MI
    fun mToFt(m: Float): Float = m * M_TO_FT

    // Imperial → SI (round-trip helpers)
    fun mphToMs(mph: Float): Float = mph / MS_TO_MPH
    fun miToKm(mi: Float): Float = mi / KM_TO_MI
    fun ftToM(ft: Float): Float = ft / M_TO_FT
}
