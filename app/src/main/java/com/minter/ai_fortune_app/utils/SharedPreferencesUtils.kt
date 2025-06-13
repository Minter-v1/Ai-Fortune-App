package com.minter.ai_fortune_app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.minter.ai_fortune_app.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * SharedPreferences를 사용한 간단한 데이터 저장소
 * 하루 1회 제한 관리 및 앱 설정 저장
 */
object SharedPreferencesUtils {

    private const val TAG = "SharedPreferencesUtils"

    // SharedPreferences 파일 이름
    private const val PREF_NAME = "ai_fortune_prefs"

    // 키 상수들
    private const val KEY_TODAY_SAJU_DATE = "today_saju_date"
    private const val KEY_TODAY_SAJU_ID = "today_saju_id"
    private const val KEY_TODAY_CHAT_COUNT = "today_chat_count"
    private const val KEY_TODAY_CHAT_DATE = "today_chat_date"
    private const val KEY_TODAY_MISSION_DATE = "today_mission_date"
    private const val KEY_TODAY_MISSION_ID = "today_mission_id"
    private const val KEY_TODAY_CONSTELLATION_DATE = "today_constellation_date"
    private const val KEY_TODAY_CONSTELLATION_EMOTION = "today_constellation_emotion"
    private const val KEY_TODAY_SAJU_RESULT = "today_saju_result"
    
    // 감정 분석 관련 키 추가
    private const val KEY_TODAY_EMOTION_DATE = "today_emotion_date"
    private const val KEY_TODAY_EMOTION_TYPE = "today_emotion_type"
    private const val KEY_TODAY_EMOTION_DISPLAY_NAME = "today_emotion_display_name"

    // ⭐ 사용자 정보 관련 키 추가
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_BIRTH_DATE = "user_birth_date"
    private const val KEY_USER_INFO_DATE = "user_info_date"

    // ⭐ 미션 상세 정보 키 추가
    private const val KEY_TODAY_MISSION_TITLE = "today_mission_title"
    private const val KEY_TODAY_MISSION_DESCRIPTION = "today_mission_description"
    private const val KEY_TODAY_MISSION_LOCATION = "today_mission_location"

    // ⭐ 영구 별자리 컬렉션 저장용 키 추가
    private const val KEY_CONSTELLATION_COLLECTION = "constellation_collection"

    /**
     * SharedPreferences 인스턴스 가져오기
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // ================================
    // 사주 관련 함수들
    // ================================

    /**
     * 오늘 사주를 생성했는지 확인
     * @param context 앱 컨텍스트
     * @return true: 오늘 이미 생성함, false: 아직 생성 안함
     */
    fun hasTodaySaju(context: Context): Boolean {
        return try {
            val prefs = getPrefs(context)
            val savedDate = prefs.getString(KEY_TODAY_SAJU_DATE, "") ?: ""
            val currentDate = DateUtils.getCurrentDate()

            val hasToday = savedDate == currentDate
            Log.d(TAG, "사주 생성 확인 - 저장된 날짜: $savedDate, 오늘: $currentDate, 결과: $hasToday")
            hasToday

        } catch (e: Exception) {
            Log.e(TAG, "사주 생성 확인 실패: ${e.message}")
            false
        }
    }

    /**
     * 오늘의 사주 생성 기록
     * @param context 앱 컨텍스트
     * @param sajuId 생성된 사주 ID
     */
    fun saveTodaySaju(context: Context, sajuId: String) {
        try {
            val prefs = getPrefs(context)
            val currentDate = DateUtils.getCurrentDate()

            prefs.edit().apply {
                putString(KEY_TODAY_SAJU_DATE, currentDate)
                putString(KEY_TODAY_SAJU_ID, sajuId)
                apply()
            }

            Log.d(TAG, "사주 생성 기록 저장 - 날짜: $currentDate, ID: $sajuId")

        } catch (e: Exception) {
            Log.e(TAG, "사주 생성 기록 저장 실패: ${e.message}")
        }
    }

    /**
     * 오늘의 사주 ID 가져오기
     */
    fun getTodaySajuId(context: Context): String? {
        return try {
            val prefs = getPrefs(context)
            prefs.getString(KEY_TODAY_SAJU_ID, null)
        } catch (e: Exception) {
            Log.e(TAG, "사주 ID 가져오기 실패: ${e.message}")
            null
        }
    }

    /**
     * 오늘의 사주 결과 저장
     * @param context 앱 컨텍스트
     * @param sajuResult 사주 결과
     */
    fun saveTodaySajuResult(context: Context, sajuResult: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(KEY_TODAY_SAJU_RESULT, sajuResult)
        editor.putString(KEY_TODAY_SAJU_DATE, DateUtils.getCurrentDate())
        editor.apply()
    }

    /**
     * 오늘의 사주 결과 가져오기
     * @param context 앱 컨텍스트
     * @return 오늘의 사주 결과, null일 경우 사주를 생성하지 않음
     */
    fun getTodaySajuResult(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_TODAY_SAJU_DATE, "")
        
        // 오늘 날짜의 사주 결과만 반환
        return if (savedDate == DateUtils.getCurrentDate()) {
            prefs.getString(KEY_TODAY_SAJU_RESULT, null)
        } else {
            null
        }
    }

    // ================================
    // 채팅 관련 함수들
    // ================================

    /**
     * 오늘의 채팅 횟수 가져오기
     * @return 0~3 사이의 값
     */
    fun getTodayChatCount(context: Context): Int {
        return try {
            val prefs = getPrefs(context)
            val savedDate = prefs.getString(KEY_TODAY_CHAT_DATE, "") ?: ""
            val currentDate = DateUtils.getCurrentDate()

            if (savedDate == currentDate) {
                prefs.getInt(KEY_TODAY_CHAT_COUNT, 0)
            } else {
                0 // 다른 날이면 0으로 초기화
            }

        } catch (e: Exception) {
            Log.e(TAG, "채팅 횟수 가져오기 실패: ${e.message}")
            0
        }
    }

    /**
     * 채팅 횟수 증가
     */
    fun incrementChatCount(context: Context) {
        try {
            val prefs = getPrefs(context)
            val currentDate = DateUtils.getCurrentDate()
            val currentCount = getTodayChatCount(context)
            val newCount = (currentCount + 1).coerceAtMost(3) // 최대 3회

            prefs.edit().apply {
                putString(KEY_TODAY_CHAT_DATE, currentDate)
                putInt(KEY_TODAY_CHAT_COUNT, newCount)
                apply()
            }

            Log.d(TAG, "채팅 횟수 증가 - $currentCount -> $newCount")

        } catch (e: Exception) {
            Log.e(TAG, "채팅 횟수 증가 실패: ${e.message}")
        }
    }

    /**
     * 오늘 채팅을 완료했는지 확인 (3회 모두 사용)
     */
    fun isChatCompleted(context: Context): Boolean {
        return getTodayChatCount(context) >= 3
    }

    // ================================
    // 미션 관련 함수들
    // ================================

    /**
     * 오늘 미션을 완료했는지 확인
     */
    fun hasTodayMission(context: Context): Boolean {
        return try {
            val prefs = getPrefs(context)
            val savedDate = prefs.getString(KEY_TODAY_MISSION_DATE, "") ?: ""
            val currentDate = DateUtils.getCurrentDate()

            savedDate == currentDate

        } catch (e: Exception) {
            Log.e(TAG, "미션 완료 확인 실패: ${e.message}")
            false
        }
    }

    /**
     * 오늘의 미션 완료 기록 (상세 정보 포함)
     */
    fun saveTodayMission(context: Context, missionId: String, missionTitle: String = "", missionDescription: String = "", missionLocation: String = "") {
        try {
            val prefs = getPrefs(context)
            val currentDate = DateUtils.getCurrentDate()

            prefs.edit().apply {
                putString(KEY_TODAY_MISSION_DATE, currentDate)
                putString(KEY_TODAY_MISSION_ID, missionId)
                if (missionTitle.isNotEmpty()) {
                    putString(KEY_TODAY_MISSION_TITLE, missionTitle)
                }
                if (missionDescription.isNotEmpty()) {
                    putString(KEY_TODAY_MISSION_DESCRIPTION, missionDescription)
                }
                if (missionLocation.isNotEmpty()) {
                    putString(KEY_TODAY_MISSION_LOCATION, missionLocation)
                }
                apply()
            }

            Log.d(TAG, "미션 완료 기록 저장 - 날짜: $currentDate, ID: $missionId, 제목: $missionTitle")

        } catch (e: Exception) {
            Log.e(TAG, "미션 완료 기록 저장 실패: ${e.message}")
        }
    }

    // 기존 saveTodayMission 오버로드 (호환성 유지)
    fun saveTodayMission(context: Context, missionId: String) {
        saveTodayMission(context, missionId, "", "", "")
    }

    /**
     * 오늘의 미션 정보 가져오기
     * @return Triple<missionTitle, missionDescription, missionLocation> 또는 null
     */
    fun getTodayMissionInfo(context: Context): Triple<String, String, String>? {
        return try {
            val prefs = getPrefs(context)
            val savedDate = prefs.getString(KEY_TODAY_MISSION_DATE, "") ?: ""
            val currentDate = DateUtils.getCurrentDate()

            if (savedDate == currentDate) {
                val missionTitle = prefs.getString(KEY_TODAY_MISSION_TITLE, null) ?: "오늘의 미션"
                val missionDescription = prefs.getString(KEY_TODAY_MISSION_DESCRIPTION, null) ?: "미션을 수행해보세요!"
                val missionLocation = prefs.getString(KEY_TODAY_MISSION_LOCATION, null) ?: "현재 위치"
                
                Log.d(TAG, "저장된 미션 정보 조회 - 제목: $missionTitle, 설명: $missionDescription, 위치: $missionLocation")
                Triple(missionTitle, missionDescription, missionLocation)
            } else {
                Log.d(TAG, "오늘 저장된 미션 정보 없음")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "미션 정보 조회 실패: ${e.message}")
            null
        }
    }

    // ================================
    // 별자리 관련 함수들
    // ================================

    /**
     * 오늘 별자리를 수집했는지 확인
     */
    fun hasTodayConstellation(context: Context): Boolean {
        return try {
            val prefs = getPrefs(context)
            val savedDate = prefs.getString(KEY_TODAY_CONSTELLATION_DATE, "") ?: ""
            val currentDate = DateUtils.getCurrentDate()

            savedDate == currentDate

        } catch (e: Exception) {
            Log.e(TAG, "별자리 수집 확인 실패: ${e.message}")
            false
        }
    }

    /**
     * 오늘의 별자리 수집 기록 (감정 정보 포함) + 영구 컬렉션에 추가
     * @param context 앱 컨텍스트
     * @param emotionType 수집된 감정 타입
     */
    fun saveTodayConstellation(context: Context, emotionType: String?) {
        try {
            val prefs = getPrefs(context)
            val currentDate = DateUtils.getCurrentDate()

            // 기존 방식: 오늘 수집 여부 저장
            prefs.edit().apply {
                putString(KEY_TODAY_CONSTELLATION_DATE, currentDate)
                if (emotionType != null) {
                    putString(KEY_TODAY_CONSTELLATION_EMOTION, emotionType)
                }
                apply()
            }

            // ⭐ 새로운 방식: 영구 컬렉션에 추가
            if (emotionType != null) {
                addToConstellationCollection(context, currentDate, emotionType)
            }

            Log.d(TAG, "별자리 수집 기록 저장 - 날짜: $currentDate, 감정: $emotionType")

        } catch (e: Exception) {
            Log.e(TAG, "별자리 수집 기록 저장 실패: ${e.message}")
        }
    }

    /**
     * 영구 별자리 컬렉션에 새로운 별자리 추가
     * @param context 앱 컨텍스트
     * @param date 수집 날짜
     * @param emotionType 감정 타입
     */
    private fun addToConstellationCollection(context: Context, date: String, emotionType: String) {
        try {
            val prefs = getPrefs(context)
            
            // 기존 컬렉션 불러오기
            val existingCollection = prefs.getString(KEY_CONSTELLATION_COLLECTION, "[]") ?: "[]"
            
            // JSON 배열로 파싱 (간단한 문자열 처리)
            val collectionList = parseConstellationCollection(existingCollection)
            
            // 이미 같은 날짜에 수집한 별자리가 있는지 확인
            val existingIndex = collectionList.indexOfFirst { it.first == date }
            
            if (existingIndex >= 0) {
                // 같은 날짜가 있으면 감정 타입만 업데이트
                collectionList[existingIndex] = Pair(date, emotionType)
                Log.d(TAG, "별자리 컬렉션 업데이트 - 날짜: $date, 감정: $emotionType")
            } else {
                // 새로운 날짜이면 추가
                collectionList.add(Pair(date, emotionType))
                Log.d(TAG, "별자리 컬렉션 추가 - 날짜: $date, 감정: $emotionType")
            }
            
            // 컬렉션을 JSON 문자열로 변환하여 저장
            val updatedCollection = formatConstellationCollection(collectionList)
            
            prefs.edit().apply {
                putString(KEY_CONSTELLATION_COLLECTION, updatedCollection)
                apply()
            }

            Log.d(TAG, "영구 별자리 컬렉션 저장 완료 - 총 ${collectionList.size}개")

        } catch (e: Exception) {
            Log.e(TAG, "영구 별자리 컬렉션 저장 실패: ${e.message}")
        }
    }

    /**
     * 모든 수집된 별자리 컬렉션 가져오기
     * @param context 앱 컨텍스트
     * @return List<Pair<날짜, 감정타입>> - 수집 순서대로 정렬
     */
    fun getAllConstellationCollection(context: Context): List<Pair<String, String>> {
        return try {
            val prefs = getPrefs(context)
            val collectionJson = prefs.getString(KEY_CONSTELLATION_COLLECTION, "[]") ?: "[]"
            
            val collection = parseConstellationCollection(collectionJson)
            
            Log.d(TAG, "별자리 컬렉션 조회 완료 - 총 ${collection.size}개")
            
            // 날짜순으로 정렬하여 반환
            collection.sortedBy { it.first }

        } catch (e: Exception) {
            Log.e(TAG, "별자리 컬렉션 조회 실패: ${e.message}")
            emptyList()
        }
    }

    /**
     * 별자리 컬렉션 JSON 문자열을 파싱
     * 형식: [{"date":"2024-01-01","emotion":"HAPPY"},{"date":"2024-01-02","emotion":"ANGRY"}]
     */
    private fun parseConstellationCollection(json: String): MutableList<Pair<String, String>> {
        return try {
            val result = mutableListOf<Pair<String, String>>()
            
            if (json == "[]" || json.isBlank()) {
                return result
            }
            
            // 간단한 JSON 파싱 (정규식 사용)
            val itemPattern = """"date":"([^"]+)","emotion":"([^"]+)"""".toRegex()
            val matches = itemPattern.findAll(json)
            
            for (match in matches) {
                val date = match.groupValues[1]
                val emotion = match.groupValues[2]
                result.add(Pair(date, emotion))
            }
            
            Log.d(TAG, "별자리 컬렉션 파싱 완료 - ${result.size}개 항목")
            result

        } catch (e: Exception) {
            Log.e(TAG, "별자리 컬렉션 파싱 실패: ${e.message}")
            mutableListOf()
        }
    }

    /**
     * 별자리 컬렉션을 JSON 문자열로 변환
     */
    private fun formatConstellationCollection(collection: List<Pair<String, String>>): String {
        return try {
            if (collection.isEmpty()) {
                "[]"
            } else {
                val items = collection.joinToString(",") { (date, emotion) ->
                    """{"date":"$date","emotion":"$emotion"}"""
                }
                "[$items]"
            }

        } catch (e: Exception) {
            Log.e(TAG, "별자리 컬렉션 포맷팅 실패: ${e.message}")
            "[]"
        }
    }

    /**
     * 별자리 컬렉션에서 현재 수집된 별의 개수 반환
     * @param context 앱 컨텍스트
     * @return 수집된 별의 개수 (0-7)
     */
    fun getConstellationCollectionSize(context: Context): Int {
        return try {
            val collection = getAllConstellationCollection(context)
            val size = collection.size.coerceAtMost(7) // 최대 7개 (북두칠성)
            
            Log.d(TAG, "별자리 컬렉션 크기: $size")
            size

        } catch (e: Exception) {
            Log.e(TAG, "별자리 컬렉션 크기 조회 실패: ${e.message}")
            0
        }
    }

    /**
     * 오늘 수집한 별자리의 감정 타입 가져오기
     * @param context 앱 컨텍스트
     * @return 감정 타입 문자열 (HAPPY, ANGRY, SAD, TIMID, GRUMPY) 또는 null
     */
    fun getTodayConstellationEmotion(context: Context): String? {
        return try {
            val prefs = getPrefs(context)
            val savedDate = prefs.getString(KEY_TODAY_CONSTELLATION_DATE, "") ?: ""
            val currentDate = DateUtils.getCurrentDate()

            if (savedDate == currentDate) {
                val emotion = prefs.getString(KEY_TODAY_CONSTELLATION_EMOTION, null)
                Log.d(TAG, "별자리 감정 정보 조회 - 날짜: $currentDate, 감정: $emotion")
                emotion
            } else {
                Log.d(TAG, "오늘 수집한 별자리 없음 - 저장된 날짜: $savedDate, 현재 날짜: $currentDate")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "별자리 감정 정보 조회 실패: ${e.message}")
            null
        }
    }

    // ================================
    // 감정 분석 관련 함수들 (새로 추가)
    // ================================

    /**
     * 오늘 감정 분석을 했는지 확인
     */
    fun hasTodayEmotionAnalysis(context: Context): Boolean {
        return try {
            val prefs = getPrefs(context)
            val savedDate = prefs.getString(KEY_TODAY_EMOTION_DATE, "") ?: ""
            val currentDate = DateUtils.getCurrentDate()

            savedDate == currentDate

        } catch (e: Exception) {
            Log.e(TAG, "감정 분석 확인 실패: ${e.message}")
            false
        }
    }

    /**
     * 오늘의 감정 분석 결과 저장
     */
    fun saveTodayEmotionAnalysis(context: Context, emotionType: String, emotionDisplayName: String) {
        try {
            val prefs = getPrefs(context)
            val currentDate = DateUtils.getCurrentDate()

            prefs.edit().apply {
                putString(KEY_TODAY_EMOTION_DATE, currentDate)
                putString(KEY_TODAY_EMOTION_TYPE, emotionType)
                putString(KEY_TODAY_EMOTION_DISPLAY_NAME, emotionDisplayName)
                apply()
            }

            Log.d(TAG, "감정 분석 결과 저장 - 날짜: $currentDate, 감정: $emotionType, 표시명: $emotionDisplayName")

        } catch (e: Exception) {
            Log.e(TAG, "감정 분석 결과 저장 실패: ${e.message}")
        }
    }

    /**
     * 오늘의 감정 분석 결과 가져오기
     * @return Pair<emotionType, emotionDisplayName> 또는 null
     */
    fun getTodayEmotionAnalysis(context: Context): Pair<String, String>? {
        return try {
            val prefs = getPrefs(context)
            val savedDate = prefs.getString(KEY_TODAY_EMOTION_DATE, "") ?: ""
            val currentDate = DateUtils.getCurrentDate()

            if (savedDate == currentDate) {
                val emotionType = prefs.getString(KEY_TODAY_EMOTION_TYPE, null)
                val emotionDisplayName = prefs.getString(KEY_TODAY_EMOTION_DISPLAY_NAME, null)
                
                if (emotionType != null && emotionDisplayName != null) {
                    Log.d(TAG, "저장된 감정 분석 결과 조회 - 감정: $emotionType, 표시명: $emotionDisplayName")
                    Pair(emotionType, emotionDisplayName)
                } else {
                    null
                }
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "감정 분석 결과 조회 실패: ${e.message}")
            null
        }
    }

    // ================================
    // 사용자 정보 관련 함수들 (새로 추가)
    // ================================

    /**
     * 사용자 정보 저장
     */
    fun saveUserInfo(context: Context, userName: String, userBirthDate: String) {
        try {
            val prefs = getPrefs(context)
            val currentDate = DateUtils.getCurrentDate()

            prefs.edit().apply {
                putString(KEY_USER_NAME, userName)
                putString(KEY_USER_BIRTH_DATE, userBirthDate)
                putString(KEY_USER_INFO_DATE, currentDate)
                apply()
            }

            Log.d(TAG, "사용자 정보 저장 - 이름: $userName, 생년월일: $userBirthDate, 날짜: $currentDate")

        } catch (e: Exception) {
            Log.e(TAG, "사용자 정보 저장 실패: ${e.message}")
        }
    }

    /**
     * 사용자 정보 가져오기
     * @return Pair<userName, userBirthDate> 또는 null
     */
    fun getUserInfo(context: Context): Pair<String, String>? {
        return try {
            val prefs = getPrefs(context)
            val savedDate = prefs.getString(KEY_USER_INFO_DATE, "") ?: ""
            val currentDate = DateUtils.getCurrentDate()

            if (savedDate == currentDate) {
                val userName = prefs.getString(KEY_USER_NAME, null)
                val userBirthDate = prefs.getString(KEY_USER_BIRTH_DATE, null)
                
                if (userName != null && userBirthDate != null) {
                    Log.d(TAG, "저장된 사용자 정보 조회 - 이름: $userName, 생년월일: $userBirthDate")
                    Pair(userName, userBirthDate)
                } else {
                    Log.d(TAG, "사용자 정보 불완전")
                    null
                }
            } else {
                Log.d(TAG, "오늘 저장된 사용자 정보 없음 - 저장된 날짜: $savedDate, 현재 날짜: $currentDate")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "사용자 정보 조회 실패: ${e.message}")
            null
        }
    }

    // ================================
    // 전체 상태 관리
    // ================================

    /**
     * 오늘의 모든 단계를 완료했는지 확인
     * 사주 생성 + 채팅 완료 + 미션 완료 + 별자리 수집
     */
    fun isAllStepsCompleted(context: Context): Boolean {
        return hasTodaySaju(context) &&
                isChatCompleted(context) &&
                hasTodayMission(context) &&
                hasTodayConstellation(context)
    }

    /**
     * 하루 제한 초기화 (자정에 호출)
     * 실제로는 날짜 체크로 자동 초기화되므로 필요시에만 사용
     */
    fun resetDailyLimits(context: Context) {
        try {
            val prefs = getPrefs(context)
            prefs.edit().clear().apply()
            Log.d(TAG, "일일 제한 초기화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "일일 제한 초기화 실패: ${e.message}")
        }
    }

    /**
     * 디버깅용: 모든 저장된 값 출력 (개선된 버전)
     */
    fun debugPrintAll(context: Context) {
        try {
            val prefs = getPrefs(context)
            val currentDate = DateUtils.getCurrentDate()
            
            Log.d(TAG, "=== SharedPreferences 디버그 ===")
            Log.d(TAG, "현재 날짜: $currentDate")
            Log.d(TAG, "사주 날짜: ${prefs.getString(KEY_TODAY_SAJU_DATE, "없음")}")
            Log.d(TAG, "사주 ID: ${prefs.getString(KEY_TODAY_SAJU_ID, "없음")}")
            Log.d(TAG, "채팅 날짜: ${prefs.getString(KEY_TODAY_CHAT_DATE, "없음")}")
            Log.d(TAG, "채팅 횟수: ${prefs.getInt(KEY_TODAY_CHAT_COUNT, 0)}")
            Log.d(TAG, "미션 날짜: ${prefs.getString(KEY_TODAY_MISSION_DATE, "없음")}")
            Log.d(TAG, "미션 ID: ${prefs.getString(KEY_TODAY_MISSION_ID, "없음")}")
            Log.d(TAG, "미션 제목: ${prefs.getString(KEY_TODAY_MISSION_TITLE, "없음")}")
            Log.d(TAG, "미션 설명: ${prefs.getString(KEY_TODAY_MISSION_DESCRIPTION, "없음")}")
            Log.d(TAG, "미션 위치: ${prefs.getString(KEY_TODAY_MISSION_LOCATION, "없음")}")
            Log.d(TAG, "별자리 날짜: ${prefs.getString(KEY_TODAY_CONSTELLATION_DATE, "없음")}")
            Log.d(TAG, "별자리 감정: ${prefs.getString(KEY_TODAY_CONSTELLATION_EMOTION, "없음")}")
            Log.d(TAG, "감정분석 날짜: ${prefs.getString(KEY_TODAY_EMOTION_DATE, "없음")}")
            Log.d(TAG, "감정분석 타입: ${prefs.getString(KEY_TODAY_EMOTION_TYPE, "없음")}")
            Log.d(TAG, "감정분석 표시명: ${prefs.getString(KEY_TODAY_EMOTION_DISPLAY_NAME, "없음")}")
            Log.d(TAG, "사용자 이름: ${prefs.getString(KEY_USER_NAME, "없음")}")
            Log.d(TAG, "사용자 생년월일: ${prefs.getString(KEY_USER_BIRTH_DATE, "없음")}")
            Log.d(TAG, "--- 계산된 값들 ---")
            Log.d(TAG, "hasTodaySaju: ${hasTodaySaju(context)}")
            Log.d(TAG, "hasTodayMission: ${hasTodayMission(context)}")
            Log.d(TAG, "hasTodayConstellation: ${hasTodayConstellation(context)}")
            Log.d(TAG, "hasTodayEmotionAnalysis: ${hasTodayEmotionAnalysis(context)}")
            Log.d(TAG, "==============================")

        } catch (e: Exception) {
            Log.e(TAG, "디버그 출력 실패: ${e.message}")
        }
    }
}