package com.minter.ai_fortune_app.utils

object ValidationUtils {
    // 생년월일 검증 (YYYY-MM-DD 형식으로 통일)
    fun validateBirthDate(birthDate: String): Boolean {
        return birthDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
    }

    // YYYY.MM.DD 형식도 지원 (기존 데이터 호환성)
    fun validateBirthDateWithDot(birthDate: String): Boolean {
        return birthDate.matches(Regex("\\d{4}\\.\\d{2}\\.\\d{2}"))
    }

    // ID 유효성 검사
    fun isValidId(id: String?): Boolean {
        return !id.isNullOrBlank() && id.length >= 3
    }

    // 이름 검증
    fun validateName(name: String): Boolean {
        return name.isNotBlank() && name.length >= 2 && name.length <= 20
    }

    // 위치 주소 검증
    fun validateAddress(address: String): Boolean {
        return address.isNotBlank() && address.length <= 100
    }
}