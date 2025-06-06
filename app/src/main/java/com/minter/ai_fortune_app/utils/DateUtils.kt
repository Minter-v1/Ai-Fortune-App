package com.minter.ai_fortune_app.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun getCurrentDate(): String = dateFormat.format(Date())

    fun getCurrentDateTime(): String = dateTimeFormat.format(Date())

    fun isToday(dateString: String): Boolean = dateString == getCurrentDate()

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    // 추가 필요한 함수들
    fun isToday(timestamp: Long): Boolean {
        val date = formatDate(timestamp)
        return date == getCurrentDate()
    }

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun isSameDate(date1: String, date2: String): Boolean = date1 == date2

    fun isSameDate(timestamp1: Long, timestamp2: Long): Boolean {
        return formatDate(timestamp1) == formatDate(timestamp2)
    }
}