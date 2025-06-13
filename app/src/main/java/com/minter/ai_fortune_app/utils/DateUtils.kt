package com.minter.ai_fortune_app.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // 디버그용 날짜 오프셋 (일 단위)
    private var debugDayOffset: Int = 0

    /**
     * 현재 날짜를 YYYY-MM-DD 형식으로 반환
     * 디버그 모드에서는 오프셋이 적용
     */
    fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        
        // 디버그 오프셋 적용
        if (debugDayOffset != 0) {
            calendar.add(Calendar.DAY_OF_MONTH, debugDayOffset)
            Log.d("DateUtils", "🔧 디버그 모드: 날짜 오프셋 +$debugDayOffset 일 적용")
        }
        
        return dateFormat.format(calendar.time)
    }

    /**
     * 현재 날짜시간을 반환 (디버그 오프셋 적용)
     */
    fun getCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        
        // 디버그 오프셋 적용
        if (debugDayOffset != 0) {
            calendar.add(Calendar.DAY_OF_MONTH, debugDayOffset)
        }
        
        return dateTimeFormat.format(calendar.time)
    }

    /**
     * 디버그용: 날짜 오프셋 설정
     * @param days 현재 날짜에서 더할 일수 (음수 가능)
     */
    fun setDebugDayOffset(days: Int) {
        debugDayOffset = days
        Log.d("DateUtils", "🔧 디버그 날짜 오프셋 설정: +$days 일")
        Log.d("DateUtils", "현재 반환 날짜: ${getCurrentDate()}")
    }

    /**
     * 디버그 모드 해제
     */
    fun clearDebugMode() {
        debugDayOffset = 0
        Log.d("DateUtils", "🔧 디버그 모드 해제")
        Log.d("DateUtils", "현재 반환 날짜: ${getCurrentDate()}")
    }

    /**
     * 현재 디버그 오프셋 값 반환
     */
    fun getDebugDayOffset(): Int = debugDayOffset

    /**
     * 디버그 모드 활성화 여부 확인
     */
    fun isDebugModeActive(): Boolean = debugDayOffset != 0

    fun isToday(dateString: String): Boolean = dateString == getCurrentDate()

    fun formatDate(timestamp: Long): String {
        // 일반적인 timestamp 포맷팅은 디버그 영향 받지 않음
        return dateFormat.format(Date(timestamp))
    }

    // 추가 필요한 함수들
    fun isToday(timestamp: Long): Boolean {
        val date = formatDate(timestamp)
        return date == getCurrentDate()
    }

    fun formatDateTime(timestamp: Long): String {
        // 일반적인 timestamp 포맷팅은 디버그 영향 받지 않음
        return dateTimeFormat.format(Date(timestamp))
    }

    fun isSameDate(date1: String, date2: String): Boolean = date1 == date2

    fun isSameDate(timestamp1: Long, timestamp2: Long): Boolean {
        return formatDate(timestamp1) == formatDate(timestamp2)
    }
}