package com.example.musicapp.data.network

/**
 * Marker annotation for endpoints that must NOT have an Authorization header
 * attached by [AuthInterceptor]. Used for the public auth endpoints
 * (`/v1/auth/google`, `/v1/auth/refresh`) that bootstrap the session itself.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class NoAuth
