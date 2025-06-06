package com.minter.ai_fortune_app.data.model

import com.minter.ai_fortune_app.utils.DateUtils
import com.minter.ai_fortune_app.utils.ValidationUtils
import java.util.UUID

data class UserInfo(
    val name: String,
    val birthDate: String // YYYY-MM-DD 형식으로 통일
) {
    // 이름 유효성 검증
    val isNameValid: Boolean
        get() = ValidationUtils.validateName(name)

    // 생년월일 유효성 검증 (오타 수정)
    val isBirthDateValid: Boolean
        get() = ValidationUtils.validateBirthDate(birthDate)

    // 전체 유효성 검사
    val isValid: Boolean
        get() = isNameValid && isBirthDateValid
}

// 사주 카테고리 enum (JOB → CAREER 수정)
enum class SajuCategory(val displayName: String) {
    DAILY("오늘의 사주"),
    LOVE("연애"),
    STUDY("학업"),
    CAREER("직업"),
    HEALTH("건강")
}

data class SajuRequest(
    val userInfo: UserInfo,
    val category: SajuCategory
)

data class SajuResult(
    val id: String = UUID.randomUUID().toString(),
    val userInfo: UserInfo,
    val category: SajuCategory,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val createdDate: String = DateUtils.getCurrentDate()
) {
    // 오늘 생성된 사주인지 확인
    val isToday: Boolean
        get() = DateUtils.isToday(createdDate)
}

data class DailySajuAccess(
    val date: String = DateUtils.getCurrentDate(),
    val sajuId: String? = null,
    val generatedAt: Long? = null
) {
    // 오늘 생성 여부
    val hasGeneratedToday: Boolean
        get() = sajuId != null && DateUtils.isToday(date)

    // 오늘 생성 가능 여부
    val canGenerateToday: Boolean
        get() = !hasGeneratedToday

    fun recordGeneration(sajuId: String): DailySajuAccess {
        return copy(
            sajuId = sajuId,
            generatedAt = System.currentTimeMillis()
        )
    }
}