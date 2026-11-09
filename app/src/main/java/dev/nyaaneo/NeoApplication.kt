package dev.nyaaneo

import android.app.Application
import dev.nyaaneo.data.NyaaRepository
import dev.nyaaneo.data.UserPreferences
import dev.nyaaneo.data.AnimeArtworkRepository
import java.io.File

class NeoApplication : Application() {
    val repository by lazy { NyaaRepository() }
    val preferences by lazy { UserPreferences(this) }
    val artwork by lazy { AnimeArtworkRepository(File(filesDir, "catalog/cover-cache-v3.json")) }
}
