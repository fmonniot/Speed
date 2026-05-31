package eu.monniot.speed.data

// App color-scheme preference (§4.9 Dark theme). SYSTEM defers to the OS setting
// (isSystemInDarkTheme); LIGHT/DARK force a scheme regardless of the OS.
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: LIGHT
    }
}
