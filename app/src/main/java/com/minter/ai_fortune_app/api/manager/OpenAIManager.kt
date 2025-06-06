package com.minter.ai_fortune_app.api.manager

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import android.util.Log
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException
import com.minter.ai_fortune_app.BuildConfig
import com.minter.ai_fortune_app.api.service.OpenAIApiService
import com.minter.ai_fortune_app.data.model.*

class OpenAIManager private constructor() {

    companion object {
        private const val TAG = "OpenAIManager"
        private const val BASE_URL = "https://api.openai.com/"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_BASE = 1000L
        private const val RATE_LIMIT_DELAY = 2000L

        @Volatile
        private var INSTANCE: OpenAIManager? = null

        fun getInstance(): OpenAIManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OpenAIManager().also {
                    INSTANCE = it
                    Log.d(TAG, "OpenAIManager 인스턴스 생성")
                }
            }
        }
    }

    private val apiKey = BuildConfig.MOCOM_API_KEY

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(OpenAIApiService::class.java)

    suspend fun generateSaju(sajuRequest: SajuRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "사주 생성 시작")

            val prompt = PromptTemplate.createSajuPrompt(sajuRequest.userInfo, sajuRequest.category)

            val request = OpenAIRequest(
                model = "gpt-4o",
                messages = listOf(
                    Message("system", "당신은 따뜻하고 긍정적인 전문 사주 상담사입니다."),
                    Message("user", prompt)
                ),
                max_tokens = 800,
                temperature = 0.8
            )

            val response = executeWithRetry("사주 생성") {
                apiService.createChatCompletion("Bearer $apiKey", request)
            }

            if (response.isSuccessful) {
                val openAIResponse = response.body()
                if (openAIResponse != null) {
                    val content = OpenAIResponseParser.extractSajuContent(openAIResponse)

                    if (content.length >= 100) {
                        Log.d(TAG, "사주 생성 성공")
                        Result.success(content)
                    } else {
                        Result.failure(Exception("생성된 사주가 너무 짧습니다."))
                    }
                } else {
                    Result.failure(Exception("서버 응답을 처리할 수 없습니다."))
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "API 인증에 실패했습니다."
                    429 -> "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
                    500, 502, 503 -> "서버에 일시적인 문제가 발생했습니다."
                    else -> "사주 생성에 실패했습니다."
                }
                Result.failure(Exception(errorMessage))
            }

        } catch (e: Exception) {
            Log.e(TAG, "사주 생성 중 예외 발생", e)
            Result.failure(handleNetworkException(e, "사주 생성"))
        }
    }

    suspend fun generateChatResponse(
        userMessage: String,
        conversationCount: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "채팅 응답 생성 시작")

            val prompt = PromptTemplate.createChatPrompt(userMessage, conversationCount)

            val request = OpenAIRequest(
                model = "gpt-4o",
                messages = listOf(
                    Message("system", "당신은 뾰롱이라는 귀여운 귀신 캐릭터입니다."),
                    Message("user", prompt)
                ),
                max_tokens = 300,
                temperature = 0.9
            )

            val response = executeWithRetry("채팅 응답") {
                apiService.createChatCompletion("Bearer $apiKey", request)
            }

            if (response.isSuccessful) {
                val openAIResponse = response.body()
                if (openAIResponse != null) {
                    val content = OpenAIResponseParser.extractChatContent(openAIResponse)

                    if (content.length in 10..200) {
                        Log.d(TAG, "채팅 응답 생성 성공")
                        Result.success(content)
                    } else {
                        Result.failure(Exception("적절한 응답을 생성하지 못했습니다."))
                    }
                } else {
                    Result.failure(Exception("서버 응답을 처리할 수 없습니다."))
                }
            } else {
                val errorMessage = when (response.code()) {
                    429 -> "잠시만 기다려줘! 너무 빨리 말하는 것 같아 😅"
                    else -> "응답을 받지 못했어요. 다시 말해줄래? 🥺"
                }
                Result.failure(Exception(errorMessage))
            }

        } catch (e: Exception) {
            Log.e(TAG, "채팅 응답 생성 중 예외 발생", e)
            Result.failure(handleNetworkException(e, "채팅"))
        }
    }

    suspend fun generateMission(locationInfo: LocationInfo): Result<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "미션 생성 시작")

                val prompt = PromptTemplate.createMissionPrompt(locationInfo)

                val request = OpenAIRequest(
                    model = "gpt-4o",
                    messages = listOf(
                        Message("system", "당신은 위치 기반 웰빙 활동 추천 전문가입니다."),
                        Message("user", prompt)
                    ),
                    max_tokens = 400,
                    temperature = 0.7
                )

                val response = executeWithRetry("미션 생성") {
                    apiService.createChatCompletion("Bearer $apiKey", request)
                }

                if (response.isSuccessful) {
                    val openAIResponse = response.body()
                    if (openAIResponse != null) {
                        val (title, description) = OpenAIResponseParser.extractMissionContent(openAIResponse)

                        if (title.isNotBlank() && description.isNotBlank()) {
                            Log.d(TAG, "미션 생성 성공")
                            Result.success(Pair(title, description))
                        } else {
                            Result.failure(Exception("적절한 미션을 생성하지 못했습니다."))
                        }
                    } else {
                        Result.failure(Exception("서버 응답을 처리할 수 없습니다."))
                    }
                } else {
                    Result.failure(Exception("미션 생성에 실패했습니다."))
                }

            } catch (e: Exception) {
                Log.e(TAG, "미션 생성 중 예외 발생", e)
                Result.failure(handleNetworkException(e, "미션 생성"))
            }
        }

    suspend fun analyzeEmotion(userMessages: List<String>): Result<EmotionType> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "감정 분석 시작")

                val prompt = PromptTemplate.createEmotionAnalysisPrompt(userMessages)

                val request = OpenAIRequest(
                    model = "gpt-4o",
                    messages = listOf(
                        Message("system", "당신은 텍스트 감정 분석 전문가입니다."),
                        Message("user", prompt)
                    ),
                    max_tokens = 50,
                    temperature = 0.3
                )

                val response = executeWithRetry("감정 분석") {
                    apiService.createChatCompletion("Bearer $apiKey", request)
                }

                if (response.isSuccessful) {
                    val openAIResponse = response.body()
                    if (openAIResponse != null) {
                        val emotion = OpenAIResponseParser.extractEmotionType(openAIResponse)
                        Log.d(TAG, "감정 분석 성공")
                        Result.success(emotion)
                    } else {
                        Result.failure(Exception("감정 분석 응답을 처리할 수 없습니다."))
                    }
                } else {
                    Result.failure(Exception("감정 분석에 실패했습니다."))
                }

            } catch (e: Exception) {
                Log.e(TAG, "감정 분석 중 예외 발생", e)
                Result.failure(handleNetworkException(e, "감정 분석"))
            }
        }

    private suspend fun <T> executeWithRetry(
        operationName: String,
        operation: suspend () -> Response<T>
    ): Response<T> {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                val result = operation()

                if (result.isSuccessful || !shouldRetry(result.code())) {
                    return result
                }

                if (attempt < MAX_RETRIES - 1) {
                    val delayTime = calculateRetryDelay(attempt, result.code())
                    delay(delayTime)
                }

            } catch (e: Exception) {
                lastException = e

                if (attempt == MAX_RETRIES - 1) {
                    throw e
                }

                if (isRetryableException(e)) {
                    val delayTime = calculateRetryDelay(attempt, null)
                    delay(delayTime)
                } else {
                    throw e
                }
            }
        }

        throw lastException ?: Exception("$operationName 최대 재시도 횟수 초과")
    }

    private fun shouldRetry(statusCode: Int): Boolean {
        return when (statusCode) {
            429 -> true
            500, 502, 503, 504 -> true
            else -> false
        }
    }

    private fun isRetryableException(exception: Exception): Boolean {
        return when (exception) {
            is IOException -> true
            is SocketTimeoutException -> true
            is UnknownHostException -> true
            else -> false
        }
    }

    private fun calculateRetryDelay(attempt: Int, statusCode: Int?): Long {
        val baseDelay = RETRY_DELAY_BASE * (attempt + 1)
        val additionalDelay = if (statusCode == 429) RATE_LIMIT_DELAY else 0L
        return baseDelay + additionalDelay
    }

    private fun handleNetworkException(exception: Exception, operation: String): Exception {
        val userMessage = when (exception) {
            is UnknownHostException -> "인터넷 연결을 확인해주세요."
            is SocketTimeoutException -> "서버 응답이 지연되고 있습니다."
            is IOException -> "네트워크 오류가 발생했습니다."
            else -> "$operation 중 오류가 발생했습니다."
        }

        return Exception(userMessage, exception)
    }

    suspend fun checkApiStatus(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val testRequest = OpenAIRequest(
                messages = listOf(Message("user", "test")),
                max_tokens = 5,
                temperature = 0.0
            )

            val response = apiService.createChatCompletion("Bearer $apiKey", testRequest)
            Result.success(response.isSuccessful)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}