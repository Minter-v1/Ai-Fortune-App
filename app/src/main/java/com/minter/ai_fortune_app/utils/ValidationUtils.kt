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

    /**
     * 생년월일 자동 포맷팅 함수
     * 다양한 형식의 입력을 YYYY-MM-DD 형식으로 변환
     * 
     * @param input 사용자 입력 (예: "010817", "20010817", "2001-08-17")
     * @return 포맷된 날짜 문자열 또는 원본 (변환 불가 시)
     */
    fun formatBirthDate(input: String): String {
        // 공백 제거 및 특수문자 제거
        val cleanInput = input.replace(Regex("[^0-9]"), "")
        
        return when (cleanInput.length) {
            6 -> {
                // YYMMDD 형식 (예: 010817 → 2001-08-17)
                try {
                    val year = cleanInput.substring(0, 2).toInt()
                    val month = cleanInput.substring(2, 4)
                    val day = cleanInput.substring(4, 6)
                    
                    // 2자리 연도를 4자리로 변환 (00-30은 20xx, 31-99는 19xx)
                    val fullYear = if (year <= 30) 2000 + year else 1900 + year
                    
                    // 월과 일 유효성 검사
                    if (month.toInt() in 1..12 && day.toInt() in 1..31) {
                        "$fullYear-$month-$day"
                    } else {
                        input // 유효하지 않으면 원본 반환
                    }
                } catch (e: Exception) {
                    input
                }
            }
            8 -> {
                // YYYYMMDD 형식 (예: 20010817 → 2001-08-17)
                try {
                    val year = cleanInput.substring(0, 4)
                    val month = cleanInput.substring(4, 6)
                    val day = cleanInput.substring(6, 8)
                    
                    // 월과 일 유효성 검사
                    if (month.toInt() in 1..12 && day.toInt() in 1..31) {
                        "$year-$month-$day"
                    } else {
                        input // 유효하지 않으면 원본 반환
                    }
                } catch (e: Exception) {
                    input
                }
            }
            else -> {
                // 다른 길이면 원본 반환
                input
            }
        }
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