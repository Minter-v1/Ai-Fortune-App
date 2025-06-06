package com.minter.ai_fortune_app.api.repository


import android.util.Log
import com.minter.ai_fortune_app.api.manager.OpenAIManager
import com.minter.ai_fortune_app.data.model.*

class OpenAIRepository {

    companion object {
        private const val TAG = "OpenAIRepository"

        @Volatile
        private var INSTANCE: OpenAIRepository? = null

        fun getInstance(): OpenAIRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OpenAIRepository().also { INSTANCE = it }
            }
        }
    }

    private val openAIManager = OpenAIManager.getInstance()

    suspend fun generateSaju(sajuRequest: SajuRequest): String {
        return openAIManager.generateSaju(sajuRequest).getOrElse {
            Log.w(TAG, "사주 생성 API 실패, Fallback 사용: ${it.message}")
            getFallbackSaju(sajuRequest.category)
        }
    }

    suspend fun generateChatResponse(userMessage: String, conversationCount: Int): String {
        return openAIManager.generateChatResponse(userMessage, conversationCount).getOrElse {
            Log.w(TAG, "채팅 API 실패, Fallback 사용: ${it.message}")
            getFallbackChatResponse()
        }
    }

    suspend fun generateMission(locationInfo: LocationInfo): Pair<String, String> {
        return openAIManager.generateMission(locationInfo).getOrElse {
            Log.w(TAG, "미션 생성 API 실패, Fallback 사용: ${it.message}")
            getFallbackMission(locationInfo.address)
        }
    }

    suspend fun analyzeEmotion(userMessages: List<String>): EmotionType {
        return openAIManager.analyzeEmotion(userMessages).getOrElse {
            Log.w(TAG, "감정 분석 API 실패, Fallback 사용: ${it.message}")
            getFallbackEmotion()
        }
    }

    suspend fun checkApiStatus(): Boolean {
        return openAIManager.checkApiStatus().getOrElse {
            Log.w(TAG, "API 상태 확인 실패: ${it.message}")
            false
        }
    }

    private fun getFallbackSaju(category: SajuCategory): String {
        return when (category) {
            SajuCategory.DAILY -> "오늘은 새로운 시작의 날입니다. 긍정적인 마음으로 하루를 시작해보세요. 작은 변화가 큰 행운을 가져다줄 것입니다."
            SajuCategory.LOVE -> "좋은 인연이 기다리고 있습니다. 자신감을 가지고 마음을 열어보세요. 진정한 사랑은 자연스럽게 찾아올 것입니다."
            SajuCategory.STUDY -> "집중력이 높아지는 시기입니다. 계획을 세우고 꾸준히 노력해보세요. 노력한 만큼 좋은 결과가 따를 것입니다."
            SajuCategory.CAREER -> "새로운 기회가 다가오고 있습니다. 적극적으로 도전해보세요. 당신의 능력을 발휘할 때가 왔습니다."
            SajuCategory.HEALTH -> "몸과 마음의 균형을 맞추는 것이 중요합니다. 충분한 휴식을 취하고 규칙적인 생활을 해보세요."
        }
    }

    private fun getFallbackChatResponse(): String {
        val responses = listOf(
            "응답을 받지 못했어요 😅 다시 이야기해주세요!",
            "지금은 좀 바빠서 대답이 늦어지고 있어요 💦",
            "잠깐, 생각 중이에요... 다시 말해줄래요? 🤔",
            "어? 뭔가 문제가 생긴 것 같아요. 다시 시도해주세요! 😊"
        )
        return responses.random()
    }

    private fun getFallbackMission(location: String): Pair<String, String> {
        val missions = listOf(
            Pair("오늘의 산책 미션", "주변을 천천히 산책하며 좋은 기운을 받아보세요! ✨"),
            Pair("힐링 타임", "근처 카페에서 따뜻한 차 한 잔과 함께 여유를 즐겨보세요 ☕"),
            Pair("자연과 함께", "가까운 공원이나 녹지에서 깊게 숨을 쉬어보세요 🌿"),
            Pair("감사 인사", "오늘 만나는 사람들에게 따뜻한 인사를 건네보세요 😊"),
            Pair("작은 친절", "주변 사람에게 작은 도움이나 친절을 베풀어보세요 💝")
        )
        return missions.random()
    }

    private fun getFallbackEmotion(): EmotionType {
        return EmotionType.HAPPY
    }
}