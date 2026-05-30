package eu.monniot.speed.data

// Display unit system. Internal storage stays SI (m/s, m, m/s²); this only governs
// the display boundary (§4.9, §5 "Units boundary").
enum class Units {
    METRIC,
    IMPERIAL;

    companion object {
        fun fromStorage(value: String?): Units =
            entries.firstOrNull { it.name == value } ?: METRIC
    }
}
