package com.minter.ai_fortune_app.data.model

import java.util.*
import com.minter.ai_fortune_app.utils.DateUtils

data class OpenAIRequest(
    val model: String = "gpt-4o",//기본값
    val messages: List<Message>, // messages: AI에게 보낼 메시지들의 리스트
    val max_tokens: Int = 500, // TODO: 토큰 수 조정 필요
    val temperature: Double = 0.7   // TODO: AI의 창의성 조절(0.0~1.0, 높을수록 창의적)
)

/**
 * 메시지 구조
 * AI와 대화할 때 주고받는 메시지 하나하나를 표현
 * 예: "안녕하세요"라는 사용자 메시지 → Message(role="user", content="안녕하세요")
 */
data class Message(
    val role: String, //System, user, assistant
    val content: String //메세지 내용
)

/**
 * OpenAI API 응답 DTO
 * 서버에서 받아오는 응답 데이터 구조
 * JSON 형태로 오는 데이터를 Kotlin 객체로 변환하는 역할
 */
data class OpenAIResponse(
    val choices: List<Choice>
)

/**
 * Choice 구조
 * AI가 생성한 답변 하나하나를 표현
 * choices 배열 안에 들어가는 개별 항목
 */

data class Choice (
    val message: Message
)

//MARK: - 프롬프트 템플릿
object PromptTemplate {

    //MARK: - 사주 생성 프롬프트
    fun createSajuPrompt(userInfo: UserInfo, category: SajuCategory) : String {
        //return = 함수 실행 결과로 내보낼 값
        return """
        당신은 친근하고 긍정적인 AI 사주 심리상담가 입니다.
        
        사용자 정보:
        - 이름: ${userInfo.name}
        - 생년월일: ${userInfo.birthDate}
        - 관심 사주 분야: ${category.displayName} //qa: enum의 dp Name이 이해가 잘 안됨
        
        다음 조건으로 ${category.displayName}에 관한 사주를 작성해주세요:
        
        1. 300-500자 이내의 긍정적이고 희망찬 내용
        2. 구체적이고 실용적인 조언 포함
        3. ${category.displayName} 분야에 특화된 사주
        4. 친근하고 따뜻한 말투 사용
        
        사주 내용만 답변해주세요.
        """.trimIndent()
    }

    //MARK: - AI 채팅 프롬프트
    fun createChatPrompt(userMessage: String, conversationCount: Int): String {
        //qa: 내부에서 주석 어떻게 쓰지
        return """
        당신은 "고스티니"라는 귀여운 귀신 캐릭터입니다. 
        
        성격: 친근하고 도움을 주고 싶어하는 귀신
        말투: 반말, 친근함, 적절한 이모티콘 사용
        답변길이: 100-150자 이내
        
        사용자 메세지: "$userMessage"
        현재 대화 횟수: "$conversationCount/3" 
        
        
        ${ if (conversationCount >= 3) {
            "이제 대화가 끝났다는 것을 자연스럽게 알려주고 행운의 액션을 추천해주세요"
            } else {
                "사용자의 이야기를 듣고 공감해주며 실용적인 조언을 해주세요."
            }
        }
        """.trimIndent()
    }

    //MARK: - AI 미션 생성 프롬프트
    fun createMissionPrompt(location: LocationInfo): String {
        return """
        사용자의 현재 위치를 바탕으로 오늘의 행운 미션을 추천해주세요.
        
        위치 : ${location.address}
        
        1. 실제로 실행 가능한 미션
        2. 우울감 및 긍정적인 감정 개선에 도움이 될 만한 행동일 것
        3. 3시간 이내 완료 가능한 미션일 것
        4. 해당 위치 주변에서 할 수 있는 것(ex. 주변 공원 및 하천 런닝 등)
        
        형식: "제목|설명" 으로 답변해주세요.
        예시: "화산천 런닝| 오늘 열심히 달려보는거 어때요?"
        """.trimIndent()
    }

    //MARK: - 감정 분석 프롬프트
    fun createEmotionAnalysisPrompt(userMessages: List<String>): String {
        val messagesText = userMessages.joinToString ("\n"){ "- $it" }

        return """
        다음 사용자의 채팅 메세지들을 분석해서 종합적인 감정 상태를 파악해주세요.
        
        사용자 메세지들:
        $messagesText
        
        다음 감정 중 하나만 선택해서 답변해주세요:
        HAPPY, ANGRY, SAD, TIMID, GRUMPY
        
        감정명만 답변해주세요. (예: HAPPY)
        
        """.trimIndent()
    }
    }


//MARK: - 응답 파싱
object OpenAIResponseParser {
    //MARK: - 사주 내용 추출
    fun extractSajuContent(response: OpenAIResponse): String {
        return response.choices.firstOrNull()?.message?.content
            ?: "사주를 생성하는데 문제가 발생했습니다."
    }

    //MARK: - 채팅 내용 추출, extractSajuContent와 기능은 같지만 에러 메세지가 다름
    fun extractChatContent(response: OpenAIResponse): String {
        return response.choices.firstOrNull()?.message?.content
            ?: "응답을 받지 못했어요. 다시 시도해주세요!"
    }

    //MARK: - 미션 내용 추출
    fun extractMissionContent(response: OpenAIResponse): Pair<String, String> {
        val content = extractSajuContent(response)

        val parts = content.split("|")

        return if (parts.size >= 2) {
            Pair(parts[0].trim(), parts[1].trim())
        }  else {
            //형식 맞지 않는 경 (|)
            Pair("오늘의 행운 미션 !", content)
        }
    }

    fun extractEmotionType(response: OpenAIResponse): EmotionType {
        val content = extractSajuContent(response).trim().uppercase()

        return when (content) {
            "HAPPY" -> EmotionType.HAPPY
            "ANGRY" -> EmotionType.ANGRY
            "SAD" -> EmotionType.SAD
            "TIMID" -> EmotionType.TIMID
            "GRUMPY" -> EmotionType.GRUMPY
            else -> EmotionType.HAPPY // 기본값
        }
    }
}
