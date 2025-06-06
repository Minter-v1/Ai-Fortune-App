package com.minter.ai_fortune_app.data.model

import com.minter.ai_fortune_app.utils.DateUtils
import java.util.UUID

enum class MissionStatus {
    RECOMMENDED,    // 미션 추천됨
    ACCEPTED,       // OK 버튼 누름
    COMPLETED,      // 미션 완료 버튼 누름
    REWARD_RECEIVED // 별자리 리워드 받음
}

data class LocationInfo(
    val address: String = "현재 위치",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

data class Mission(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val location: String,
    val createdDate: String = DateUtils.getCurrentDate(),
    val status: MissionStatus = MissionStatus.RECOMMENDED,
    val completedAt: Long? = null,
    val rewardConstellationId: String? = null
) {
    // 미션 수락 가능한지
    val canAccept: Boolean
        get() = status == MissionStatus.RECOMMENDED

    // 미션 완료 여부 (중복 제거)
    val isCompleted: Boolean
        get() = status == MissionStatus.COMPLETED || status == MissionStatus.REWARD_RECEIVED

    // 오늘 미션인지 확인
    val isToday: Boolean
        get() = DateUtils.isToday(createdDate)

    // 상태 변경 (함수명 명확하게)
    fun nextStatus(): Mission {
        val nextStatus = when (status) {
            MissionStatus.RECOMMENDED -> MissionStatus.ACCEPTED
            MissionStatus.ACCEPTED -> MissionStatus.COMPLETED
            MissionStatus.COMPLETED -> MissionStatus.REWARD_RECEIVED
            MissionStatus.REWARD_RECEIVED -> MissionStatus.REWARD_RECEIVED
        }

        return copy(
            status = nextStatus,
            completedAt = if (nextStatus == MissionStatus.COMPLETED) System.currentTimeMillis() else completedAt
        )
    }
}