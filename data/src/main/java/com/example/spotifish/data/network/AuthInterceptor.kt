package com.example.spotifish.data.network

import com.example.spotifish.data.auth.AuthRepository
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation

/**
 * Attaches `Authorization: Bearer <jwt>` to outgoing requests, and transparently
 * refreshes the session when a request comes back 401.
 *
 * Endpoints annotated with [NoAuth] (currently the auth endpoints themselves) are
 * passed through untouched so the bootstrap and refresh flows don't recurse.
 *
 * The 401 retry policy is one-shot per call: if the refresh succeeds, the original
 * request is rebuilt with the new token and replayed. If it fails, the original 401
 * propagates and the UI is expected to bounce the user back to sign-in.
 *
 * [AuthRepository] is injected lazily because it depends on the same [SpotifishApi]
 * that this interceptor sits in front of, which would be a circular dependency
 * otherwise.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authRepository: Lazy<AuthRepository>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Skip endpoints flagged @NoAuth (sign-in, refresh).
        val noAuth = original.tag(Invocation::class.java)
            ?.method()
            ?.isAnnotationPresent(NoAuth::class.java) == true
        if (noAuth) return chain.proceed(original)

        val session = authRepository.get().current()
            ?: return chain.proceed(original)

        val firstAttempt = chain.proceed(
            original.newBuilder()
                .header(HEADER_AUTH, "Bearer ${session.accessToken}")
                .build(),
        )

        if (firstAttempt.code != 401) return firstAttempt

        // Token rejected — try to refresh once.
        firstAttempt.close()
        val refreshed = runBlocking { authRepository.get().refresh() }
            ?: return chain.proceed(original)

        return chain.proceed(
            original.newBuilder()
                .header(HEADER_AUTH, "Bearer ${refreshed.accessToken}")
                .build(),
        )
    }

    private companion object {
        private const val HEADER_AUTH = "Authorization"
    }
}
