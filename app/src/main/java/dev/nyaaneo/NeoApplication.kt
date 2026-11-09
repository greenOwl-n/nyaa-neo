package dev.nyaaneo

import android.app.Application
import dev.nyaaneo.data.NyaaRepository
import dev.nyaaneo.data.UserPreferences

class NeoApplication : Application() {
    val repository by lazy { NyaaRepository() }
    val preferences by lazy { UserPreferences(this) }
}
