package com.example.imilipocket.auth

import android.content.Context
import com.example.imilipocket.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null
)

class SupabaseAuthService(context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .build()
    private val gson = Gson()
    private val sessionManager = AuthSessionManager(context)
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY

    suspend fun signUp(email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        try {
            if (!isConfigValid()) {
                return@withContext AuthResponse(false, "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY")
            }

            val payload = JsonObject().apply {
                addProperty("email", email)
                addProperty("password", password)
            }

            val response = postWithRetry("/auth/v1/signup", payload.toString())
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext AuthResponse(
                    success = false,
                    message = extractErrorMessage(body, "Sign up failed")
                )
            }

            val json = gson.fromJson(body, JsonObject::class.java)
            val session = json.getAsJsonObject("session")
            val accessToken = session?.get("access_token")?.asString
            val refreshToken = session?.get("refresh_token")?.asString
            val userId = json.getAsJsonObject("user")?.get("id")?.asString

            if (!accessToken.isNullOrBlank()) {
                sessionManager.saveSession(accessToken, refreshToken, userId)
                return@withContext AuthResponse(true, "Sign up successful", accessToken, refreshToken, userId)
            }

            AuthResponse(
                success = true,
                message = "Account created. Please verify your email, then sign in.",
                userId = userId
            )
        } catch (e: IOException) {
            AuthResponse(false, "Network error (${e.javaClass.simpleName}). Check emulator internet and try again")
        } catch (e: Exception) {
            AuthResponse(false, "Sign up failed: ${e.message ?: "unexpected error"}")
        }
    }

    suspend fun signIn(email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        try {
            if (!isConfigValid()) {
                return@withContext AuthResponse(false, "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY")
            }

            val payload = JsonObject().apply {
                addProperty("email", email)
                addProperty("password", password)
            }

            val response = postWithRetry("/auth/v1/token?grant_type=password", payload.toString())
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext AuthResponse(
                    success = false,
                    message = extractErrorMessage(body, "Sign in failed")
                )
            }

            val json = gson.fromJson(body, JsonObject::class.java)
            val accessToken = json.get("access_token")?.asString
            val refreshToken = json.get("refresh_token")?.asString
            val userId = json.getAsJsonObject("user")?.get("id")?.asString

            if (accessToken.isNullOrBlank()) {
                return@withContext AuthResponse(false, "Sign in failed: no access token returned")
            }

            sessionManager.saveSession(accessToken, refreshToken, userId)
            AuthResponse(true, "Sign in successful", accessToken, refreshToken, userId)
        } catch (e: IOException) {
            AuthResponse(false, "Network error (${e.javaClass.simpleName}). Check emulator internet and try again")
        } catch (e: Exception) {
            AuthResponse(false, "Sign in failed: ${e.message ?: "unexpected error"}")
        }
    }

    fun isSignedIn(): Boolean = sessionManager.isSignedIn()

    fun signOut() {
        sessionManager.clearSession()
    }

    private fun post(path: String, body: String): okhttp3.Response {
        val requestBody = body.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$supabaseUrl$path")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        return client.newCall(request).execute()
    }

    @Throws(IOException::class)
    private fun postWithRetry(path: String, body: String): okhttp3.Response {
        var lastError: IOException? = null
        repeat(3) { attempt ->
            try {
                return post(path, body)
            } catch (e: IOException) {
                lastError = e
                if (attempt < 2) {
                    Thread.sleep(700L)
                }
            }
        }

        throw lastError ?: IOException("Unknown network error")
    }

    private fun isConfigValid(): Boolean {
        return supabaseUrl.isNotBlank() && anonKey.isNotBlank()
    }

    private fun extractErrorMessage(body: String, fallback: String): String {
        return runCatching {
            val json = gson.fromJson(body, JsonObject::class.java)
            val errorCode = json.get("error_code")?.asString.orEmpty()
            val rawMessage = json.get("msg")?.asString
                ?: json.get("message")?.asString
                ?: json.get("error_description")?.asString
                ?: fallback

            when (errorCode) {
                "email_address_invalid" -> "Invalid email address. Use a real email format like name@gmail.com"
                "over_email_send_rate_limit" -> "Too many signup attempts. Please wait a few minutes and try again"
                "email_not_confirmed" -> "Please verify your email first, then sign in"
                "invalid_credentials" -> "Invalid email or password"
                else -> rawMessage
            }
        }.getOrElse { fallback }
    }
}
