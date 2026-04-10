package com.example.musicapp.core

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class DriveAuthSessionStore @Inject constructor() {
    private val mutex = Mutex()
    private var accountEmail: String? = null
    private var accessToken: String? = null

    suspend fun update(email: String, token: String) {
        mutex.withLock {
            accountEmail = email
            accessToken = token
        }
    }

    suspend fun tokenFor(email: String?): String? {
        return mutex.withLock {
            if (email != null && email == accountEmail) accessToken else accessToken
        }
    }

    suspend fun clear() {
        mutex.withLock {
            accountEmail = null
            accessToken = null
        }
    }
}
