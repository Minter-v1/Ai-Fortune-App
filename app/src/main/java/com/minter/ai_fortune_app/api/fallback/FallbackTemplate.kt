package com.minter.ai_fortune_app.api.fallback

import com.minter.ai_fortune_app.data.model.SajuCategory
import com.minter.ai_fortune_app.data.model.EmotionType
import kotlin.collections.*
import kotlin.random.Random

object FallbackTemplate {

    fun getSajuFallback(category: SajuCategory): String {
        return when (category) {
            SajuCategory.DAILY ->
                "오늘은 새로운 시작의 날입니다. 긍정적인 마음으로 하루를 시작해보세요. " +
                        "작은 변화가 큰 행운을 가져다줄 것입니다. " +
                        "주변 사람들과의 소통을 늘리고, 감사한 마음을 잊지 마세요. " +
                        "오늘 하루가 당신에게 특별한 의미가 될 것입니다."

            SajuCategory.LOVE ->
                "좋은 인연이 기다리고 있습니다. 자신감을 가지고 마음을 열어보세요. " +
                        "진정한 사랑은 자연스럽게 찾아올 것입니다. " +
                        "외모보다는 내면의 아름다움을 가꾸는 것이 중요합니다. " +
                        "상대방을 이해하려는 노력이 관계를 더욱 깊게 만들어 줄 것입니다."

            SajuCategory.STUDY ->
                "집중력이 높아지는 시기입니다. 계획을 세우고 꾸준히 노력해보세요. " +
                        "노력한 만큼 좋은 결과가 따를 것입니다. " +
                        "새로운 학습 방법을 시도해보는 것도 도움이 될 것입니다. " +
                        "포기하지 않는 마음가짐이 성공의 열쇠가 될 것입니다."

            SajuCategory.CAREER ->
                "새로운 기회가 다가오고 있습니다. 적극적으로 도전해보세요. " +
                        "당신의 능력을 발휘할 때가 왔습니다. " +
                        "동료들과의 협력이 성공의 중요한 요소가 될 것입니다. " +
                        "창의적인 아이디어로 주변을 놀라게 할 수 있을 것입니다."

            SajuCategory.HEALTH ->
                "몸과 마음의 균형을 맞추는 것이 중요합니다. " +
                        "충분한 휴식을 취하고 규칙적인 생활을 해보세요. " +
                        "적당한 운동과 건강한 식습관이 활력을 가져다 줄 것입니다. " +
                        "스트레스 관리에 신경 쓰고, 긍정적인 마음가짐을 유지하세요."
        }
    }

    fun getChatFallback(): String {
        val responses = listOf(
            "응답을 받지 못했어요 😅 다시 이야기해주세요!",
            "지금은 좀 바빠서 대답이 늦어지고 있어요 💦",
            "잠깐, 생각 중이에요... 다시 말해줄래요? 🤔",
            "어? 뭔가 문제가 생긴 것 같아요. 다시 시도해주세요! 😊",
            "인터넷이 좀 느린 것 같아요... 다시 한 번 말해주세요! 🌐",
            "뾰롱이가 잠깐 딴 생각을 했나봐요 😴 다시 말해주실래요?",
            "아직 답변을 준비 중이에요! 조금만 기다려주세요 ⏰",
            "뭔가 복잡한 문제네요... 다시 간단하게 말해주실래요? 🤷‍♀️"
        )
        return responses.random()
    }

    fun getMissionFallback(location: String): Pair<String, String> {
        val missions = listOf(
            Pair("오늘의 산책 미션", "주변을 천천히 산책하며 좋은 기운을 받아보세요! 스마트폰은 잠시 내려두고 주변 풍경을 감상해보세요 ✨"),
            Pair("힐링 카페 타임", "근처 카페에서 따뜻한 차 한 잔과 함께 여유를 즐겨보세요. 좋아하는 음악을 들으며 마음의 평화를 찾아보세요 ☕"),
            Pair("자연과 함께하기", "가까운 공원이나 녹지에서 깊게 숨을 쉬어보세요. 나무들과 꽃들을 보며 자연의 에너지를 느껴보세요 🌿"),
            Pair("감사 인사 미션", "오늘 만나는 사람들에게 따뜻한 인사를 건네보세요. 작은 미소 하나가 하루를 밝게 만들어 줄 거예요 😊"),
            Pair("작은 친절 실천", "주변 사람에게 작은 도움이나 친절을 베풀어보세요. 엘리베이터 버튼을 눌러주거나 문을 잡아주는 것만으로도 충분해요 💝"),
            Pair("맛있는 간식 찾기", "근처에서 평소 먹어보지 않은 새로운 간식을 찾아보세요. 새로운 맛의 발견이 작은 행복을 가져다 줄 거예요 🍰"),
            Pair("사진 촬영 미션", "오늘의 아름다운 순간을 사진으로 남겨보세요. 하늘, 꽃, 혹은 특별한 장면을 찾아 기록해보세요 📸"),
            Pair("독서 타임", "근처 서점이나 도서관에서 관심 있는 책을 찾아보세요. 몇 페이지만 읽어도 새로운 영감을 얻을 수 있을 거예요 📚"),
            Pair("음악 감상하기", "좋아하는 음악을 들으며 주변을 둘러보세요. 음악과 함께하는 시간이 마음에 위로가 될 거예요 🎵"),
            Pair("스트레칭 타임", "잠시 시간을 내어 몸을 쭉쭉 늘려보세요. 간단한 스트레칭으로 몸과 마음을 가볍게 만들어보세요 🤸‍♀️")
        )
        return missions.random()
    }

    fun getEmotionFallback(): EmotionType {
        return EmotionType.HAPPY
    }

    fun getLocationBasedMission(address: String): Pair<String, String> {
        return when {
            address.contains("대전") || address.contains("유성") -> Pair(
                "유성온천 힐링 워크",
                "유성온천 일대를 걸으며 온천의 따뜻한 기운을 느껴보세요! 🌊"
            )
            address.contains("서울") -> Pair(
                "도심 속 쉼터 찾기",
                "바쁜 서울 속에서 작은 쉼터를 찾아 잠시 여유를 즐겨보세요! 🏙️"
            )
            address.contains("부산") -> Pair(
                "바다 바람 맞기",
                "바다 근처로 가서 시원한 바람을 맞으며 마음을 정화해보세요! 🌊"
            )
            address.contains("제주") -> Pair(
                "제주 자연 감상하기",
                "제주의 아름다운 자연을 감상하며 힐링 시간을 가져보세요! 🌺"
            )
            else -> getMissionFallback(address)
        }
    }

    fun getEmergencyResponse(): String {
        return "죄송해요, 지금 뾰롱이가 잠시 쉬고 있어요 😴 조금 후에 다시 말걸어주실래요?"
    }

    fun getNetworkErrorResponse(): String {
        return "인터넷 연결이 불안정한 것 같아요 📶 네트워크를 확인하고 다시 시도해주세요!"
    }

    fun getServerErrorResponse(): String {
        return "서버에 일시적인 문제가 발생했어요 🔧 잠시 후 다시 시도해주세요!"
    }
}