package com.formuloo.core.network

import com.formuloo.core.auth.TokenRepository
import com.formuloo.core.network.dto.auth.RefreshRequest
import com.formuloo.core.network.dto.auth.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiConfig {
    val BASE_URL: String = "http://$devBackendHost"
}

fun createHttpClient(tokenRepository: TokenRepository): HttpClient = HttpClient(platformEngineFactory()) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            },
        )
    }

    install(Logging) {
        level = LogLevel.INFO
    }

    install(Auth) {
        bearer {
            loadTokens {
                val accessToken = tokenRepository.getAccessToken()
                val refreshToken = tokenRepository.getRefreshToken()
                if (accessToken != null && refreshToken != null) {
                    BearerTokens(accessToken, refreshToken)
                } else {
                    null
                }
            }
            refreshTokens {
                // `client` ici n'a pas le plugin Auth installé (fourni par Ktor pour
                // éviter une boucle infinie de refresh). On l'utilise pour appeler
                // /auth/token/refresh/ directement et persister les nouveaux tokens.
                val refreshToken = oldTokens?.refreshToken ?: return@refreshTokens null
                try {
                    val response = client.post("${ApiConfig.BASE_URL}/api/v1/auth/token/refresh/") {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(refresh = refreshToken))
                    }
                    if (!response.status.isSuccess()) {
                        tokenRepository.clear()
                        return@refreshTokens null
                    }
                    val newTokens = response.body<TokenResponse>()
                    val newRefreshToken = newTokens.refresh ?: refreshToken
                    tokenRepository.saveTokens(newTokens.access, newRefreshToken)
                    BearerTokens(newTokens.access, newRefreshToken)
                } catch (e: Exception) {
                    tokenRepository.clear()
                    null
                }
            }
        }
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
    }

    defaultRequest {
        url(ApiConfig.BASE_URL)
    }
}
