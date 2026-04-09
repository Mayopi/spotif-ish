package com.example.musicapp.data.store

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class JsonFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()

    suspend fun read(fileName: String, fallback: String): String = mutex.withLock {
        val file = file(fileName)
        if (!file.exists()) {
            file.writeText(fallback)
            return@withLock fallback
        }
        file.readText().ifBlank { fallback }
    }

    suspend fun write(fileName: String, content: String) = mutex.withLock {
        file(fileName).writeText(content)
    }

    private fun file(fileName: String): File = File(context.filesDir, fileName)
}

