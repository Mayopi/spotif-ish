package com.example.spotifish.core

/**
 * Lock-free, non-suspending access to the current backend access token.
 *
 * The Media3 data source factory runs on a player thread that can't suspend, so it
 * needs a synchronous getter. The data layer's `SessionStore` already keeps the
 * token in memory; the implementation just exposes it through this interface so the
 * `player` module stays free of any data-layer or auth dependency.
 */
interface PlaybackTokenSource {
    fun currentAccessToken(): String?
}
