package com.minter.ai_fortune_app.data.model

import com.minter.ai_fortune_app.utils.DateUtils
import java.util.UUID

//감정 타입
enum class EmotionType(val displayName: String) {
    HAPPY("Happy"), //노랑
    ANGRY("Angry"), //빨강
    SAD("Sad"), //파랑
    TIMID("Timid"), //보라
    GRUMPY("Grumpy") //초록
}

//별자리 안의 별의 위치
data class StarPosition(
    val x: Float,
    val y: Float,
    val connections: List<Int> = emptyList()  // 연결된 다른 별들의 인덱스
)


//별자리 안의 개인 별(Star)
data class Star(
    val id: String = UUID.randomUUID().toString(),
    val position: StarPosition, //별자리 내 위치
    val emotion: EmotionType? = null, //수집된 감정(null이면 회색 처리) ->컴포넌트로 색 변경
    val collectedDate: String? = null, //수집 날짜
    val isCollected: Boolean = false //수집여부
) {
    //별 수집
    fun collect(emotion: EmotionType, date: String = DateUtils.getCurrentDate()) : Star {
        return copy(
            emotion = emotion,
            collectedDate = date,
            isCollected = true
        )
    }

    val isCollectedToday: Boolean
        get() = collectedDate == DateUtils.getCurrentDate()
}



//별자리 모델
data class Constellation(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "오리온 자리",
    val stars: List<Star>
) {
    val collectedCount: Int
        get() = stars.count { it.isCollected }

    val totalStars: Int
        get() = stars.size

    val isComplete: Boolean
        get() = totalStars == collectedCount

    val nextStarToCollect: Star?
        get() = stars.firstOrNull { !it.isCollected }

    //오늘 별 수집 여부
    val hasTodayCollection: Boolean
        get() = stars.any{ it.isCollectedToday }

    fun collectNextStar(emotion: EmotionType): Constellation {
        val nextStar = nextStarToCollect ?: return this //수집할 별 찾음
        val updatedStars = stars.map { star ->
            if (star.id == nextStar.id) {
                star.collect(emotion) //감정별 수집
            } else {
                star
            }
        }

        return copy(stars = updatedStars)
    }

    //정적(static) 영역 정의 , 별자리 목록
    companion object {
        //MARK:
        fun createOrionConstellation(): Constellation {
            val orionStars = listOf(
                // TODO: 연결 인덱스 확인 필요
                Star(id = "star_1", position = StarPosition(0.3f, 0.2f, listOf(1, 2))),
                Star(id = "star_2",position = StarPosition(0.7f, 0.2f, listOf(0, 3))),
                Star(id = "star_3",position = StarPosition(0.2f, 0.4f, listOf(0, 4))),
                Star(id = "star_4",position = StarPosition(0.8f, 0.4f, listOf(1, 5))),
                Star(id = "star_5",position = StarPosition(0.4f, 0.6f, listOf(2, 6))),
                Star(id = "star_6",position = StarPosition(0.6f, 0.6f, listOf(3, 6))),
                Star(id = "star_7",position = StarPosition(0.5f, 0.8f, listOf(4, 5)))
            )
            //MARK: - 별자리 리턴
            return Constellation(stars = orionStars)
        }
    }
}







