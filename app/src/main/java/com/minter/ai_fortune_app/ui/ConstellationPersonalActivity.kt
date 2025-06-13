package com.minter.ai_fortune_app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.minter.ai_fortune_app.R
import com.minter.ai_fortune_app.data.model.EmotionType
import com.minter.ai_fortune_app.utils.SharedPreferencesUtils

/**
 * 개인별 감정 별자리 화면
 * 
 * 주요 기능:
 * 1. 사용자의 감정에 따른 개인 별자리 맵 표시
 * 2. 북두칠성 7개 별을 감정별로 색칠
 * 3. 별자리 수집 상태 표시
 */
class ConstellationPersonalActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ConstellationPersonalActivity"
    }

    // UI 요소들
    private lateinit var constellationMap: RelativeLayout
    private lateinit var constellationLines: ImageView
    private lateinit var btnNext: View

    //  모달 관련 UI 요소들 추가
    private lateinit var layoutStarModal: RelativeLayout
    private lateinit var ivStarImage: ImageView
    private lateinit var tvStarTitle: TextView
    private lateinit var tvStarInfo: TextView
    private lateinit var tvStarDescription: TextView
    private lateinit var btnModalClose: ImageView

    // 별 이미지뷰들 (북두칠성 7개)
    private val starViews: MutableList<ImageView> = mutableListOf()

    // 데이터
    private var userName: String = ""
    private var analyzedEmotion: EmotionType? = null
    private var emotionDisplayName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_constellation_personal)

        Log.d(TAG, "ConstellationPersonalActivity 시작")

        handleIntentData()
        initViews()
        setupButtonListeners()
        setupConstellationMap()
    }

    /**
     * Intent로 전달받은 데이터 처리
     */
    private fun handleIntentData() {
        try {
            userName = intent.getStringExtra("userName") ?: "사용자"
            emotionDisplayName = intent.getStringExtra("emotionDisplayName") ?: "Happy"

            val emotionName = intent.getStringExtra("analyzedEmotion") ?: "HAPPY"
            
            // 먼저 Intent에서 감정 정보를 시도
            analyzedEmotion = try {
                EmotionType.valueOf(emotionName.uppercase())
            } catch (e: Exception) {
                Log.w(TAG, "Intent에서 감정 파싱 실패: $emotionName")
                null
            }

            // Intent에서 감정 정보를 가져오지 못했다면 SharedPreferences에서 시도
            if (analyzedEmotion == null) {
                val savedEmotion = SharedPreferencesUtils.getTodayConstellationEmotion(this)
                analyzedEmotion = if (savedEmotion != null) {
                    try {
                        EmotionType.valueOf(savedEmotion.uppercase())
                    } catch (e: Exception) {
                        Log.w(TAG, "SharedPreferences에서 감정 파싱 실패: $savedEmotion")
                        EmotionType.HAPPY
                    }
                } else {
                    Log.w(TAG, "저장된 감정 정보 없음 - 기본값 사용")
                    EmotionType.HAPPY
                }
            }

            // 감정 표시명도 업데이트
            if (emotionDisplayName == "Happy" && analyzedEmotion != EmotionType.HAPPY) {
                emotionDisplayName = analyzedEmotion?.displayName ?: "Happy"
            }

            Log.d(TAG, "데이터 처리 완료 - 사용자: $userName, 감정: ${analyzedEmotion?.name}, 표시명: $emotionDisplayName")

        } catch (e: Exception) {
            Log.e(TAG, "Intent 데이터 처리 실패: ${e.message}")
            userName = "사용자"
            analyzedEmotion = EmotionType.HAPPY
            emotionDisplayName = "Happy"
        }
    }

    /**
     * UI 요소들 초기화
     */
    private fun initViews() {
        try {
            constellationMap = findViewById(R.id.constellation_map)
            constellationLines = findViewById(R.id.constellation_lines)
            btnNext = findViewById(R.id.btn_next)

            // 모달 관련 UI 요소들 초기화
            layoutStarModal = findViewById(R.id.layout_star_modal)
            ivStarImage = findViewById(R.id.iv_star_image)
            tvStarTitle = findViewById(R.id.tv_star_title)
            tvStarInfo = findViewById(R.id.tv_star_info)
            tvStarDescription = findViewById(R.id.tv_star_description)
            btnModalClose = findViewById(R.id.btn_modal_close)

            // 북두칠성 7개 별 찾기
            for (i in 1..7) {
                val starId = resources.getIdentifier("star_$i", "id", packageName)
                val starView = findViewById<ImageView>(starId)
                starViews.add(starView)
            }

            // 모달 초기 상태 설정 (숨김)
            layoutStarModal.visibility = View.GONE
            layoutStarModal.alpha = 0f

            Log.d(TAG, "UI 초기화 완료 - 별 개수: ${starViews.size}, 모달 추가됨")

        } catch (e: Exception) {
            Log.e(TAG, "UI 초기화 실패: ${e.message}")
        }
    }

    /**
     * 버튼 클릭 이벤트 설정
     */
    private fun setupButtonListeners() {
        try {
            btnNext.setOnClickListener {
                Log.d(TAG, "다음 버튼 클릭")
                proceedToNext()
            }

            // 모달 관련 이벤트 설정
            btnModalClose.setOnClickListener {
                Log.d(TAG, "모달 닫기 버튼 클릭")
                hideStarModal()
            }

            // 모달 배경 클릭 시 닫기
            layoutStarModal.setOnClickListener {
                Log.d(TAG, "모달 배경 클릭")
                hideStarModal()
            }

            // 모달 콘텐츠 클릭 시에는 닫히지 않도록 설정
            val modalContent = findViewById<View>(R.id.constellation_modal)
            modalContent.setOnClickListener {
                // 아무 동작 안 함 (이벤트 전파 차단)
            }

            // 각 별 클릭 이벤트 (별 세부 정보 표시용)
            starViews.forEachIndexed { index, starView ->
                starView.setOnClickListener {
                    Log.d(TAG, "별 ${index + 1} 클릭")
                    showStarInfo(index + 1)
                }
            }

            Log.d(TAG, "버튼 이벤트 설정 완료 (모달 포함)")

        } catch (e: Exception) {
            Log.e(TAG, "버튼 이벤트 설정 실패: ${e.message}")
        }
    }

    /**
     * 별자리 맵 설정
     * 모든 수집된 별자리를 순서대로 표시
     */
    private fun setupConstellationMap() {
        try {
            Log.d(TAG, "별자리 맵 설정 시작")

            // 모든 수집된 별자리를 로드하여 표시
            loadAllCollectedStars()

            // 별자리 연결선 표시
            showConstellationLines()

            // 다음 버튼 텍스트 설정
            updateNextButtonText()

            Log.d(TAG, "별자리 맵 설정 완료")

        } catch (e: Exception) {
            Log.e(TAG, "별자리 맵 설정 실패: ${e.message}")
        }
    }

    /**
     * 모든 수집된 별자리를 순서대로 로드하여 표시
     */
    private fun loadAllCollectedStars() {
        try {
            Log.d(TAG, "수집된 별자리 로드 시작")

            // 모든 수집된 별자리 컬렉션 가져오기
            val constellationCollection = SharedPreferencesUtils.getAllConstellationCollection(this)
            
            Log.d(TAG, "별자리 컬렉션 조회 완료 - 총 ${constellationCollection.size}개")

            // 모든 별을 기본 상태로 초기화
            resetAllStars()

            // 수집된 별자리들을 순서대로 활성화
            constellationCollection.forEachIndexed { index, (date, emotionType) ->
                if (index < starViews.size) { // 최대 7개 (북두칠성)
                    activateStarAtPosition(index, emotionType, date)
                    Log.d(TAG, "별 ${index + 1} 활성화 - 날짜: $date, 감정: $emotionType")
                }
            }

            // 컬렉션 정보 로그 출력
            if (constellationCollection.isNotEmpty()) {
                Log.d(TAG, "✨ 활성화된 별자리 목록:")
                constellationCollection.forEachIndexed { index, (date, emotion) ->
                    val starName = getStarName(index + 1)
                    Log.d(TAG, "  ${index + 1}. $starName - $date ($emotion)")
                }
            } else {
                Log.d(TAG, "수집된 별자리 없음")
            }

        } catch (e: Exception) {
            Log.e(TAG, "수집된 별자리 로드 실패: ${e.message}")
        }
    }

    /**
     * 모든 별을 기본 상태로 초기화
     */
    private fun resetAllStars() {
        try {
            starViews.forEach { starView ->
                starView.setImageResource(R.drawable.iv_star_happy) // 기본 별 이미지 (노란색)
                starView.alpha = 0.2f // 매우 반투명으로 설정
                starView.tag = null
            }
            
            Log.d(TAG, "모든 별 초기화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "별 초기화 실패: ${e.message}")
        }
    }

    /**
     * 특정 위치의 별을 감정에 따라 활성화
     * @param position 별의 위치 (0-6)
     * @param emotionType 감정 타입
     * @param date 수집 날짜
     */
    private fun activateStarAtPosition(position: Int, emotionType: String, date: String) {
        try {
            if (position >= starViews.size) {
                Log.w(TAG, "잘못된 별 위치: $position")
                return
            }

            val starView = starViews[position]
            val emotion = try {
                EmotionType.valueOf(emotionType.uppercase())
            } catch (e: Exception) {
                Log.w(TAG, "알 수 없는 감정 타입: $emotionType")
                EmotionType.HAPPY
            }

            // 감정에 따른 별 이미지 설정
            val starImageRes = getStarImageResource(emotion)
            starView.setImageResource(starImageRes)
            starView.alpha = 1.0f // 완전 불투명
            starView.tag = "${emotionType}|${date}" // 감정과 날짜 정보 저장

            // 애니메이션 효과 (수집 순서대로 딜레이)
            starView.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(300)
                .withEndAction {
                    starView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start()
                }
                .setStartDelay((position * 200).toLong()) // 0.2초씩 딜레이
                .start()

            Log.d(TAG, "별 ${position + 1} 활성화 완료 - 감정: $emotionType, 날짜: $date")

        } catch (e: Exception) {
            Log.e(TAG, "별 활성화 실패 - 위치: $position, 감정: $emotionType: ${e.message}")
        }
    }

    /**
     * 감정에 따른 별 이미지 리소스 반환
     */
    private fun getStarImageResource(emotion: EmotionType?): Int {
        return when (emotion) {
            EmotionType.HAPPY -> R.drawable.iv_star_happy
            EmotionType.ANGRY -> R.drawable.iv_star_angry
            EmotionType.SAD -> R.drawable.iv_star_sad
            EmotionType.TIMID -> R.drawable.iv_star_timid
            EmotionType.GRUMPY -> R.drawable.iv_star_grumpy
            else -> R.drawable.iv_star_happy
        }
    }

    /**
     * 별자리 연결선 표시
     */
    private fun showConstellationLines() {
        try {
            constellationLines.alpha = 0.4f
            Log.d(TAG, "별자리 연결선 표시")

        } catch (e: Exception) {
            Log.e(TAG, "별자리 연결선 표시 실패: ${e.message}")
        }
    }

    /**
     * 다음 버튼 텍스트 업데이트
     */
    private fun updateNextButtonText() {
        try {
            val btnText = btnNext.findViewById<TextView>(R.id.tv_btn_text)
            btnText?.text = "이전으로"

            Log.d(TAG, "버튼 텍스트 업데이트 완료")

        } catch (e: Exception) {
            Log.e(TAG, "버튼 텍스트 업데이트 실패: ${e.message}")
        }
    }

    /**
     * 별 정보 표시 (별 클릭 시) - 커스텀 모달 사용
     */
    private fun showStarInfo(starNumber: Int) {
        try {
            val starIndex = starNumber - 1
            if (starIndex >= starViews.size) {
                return
            }

            val starView = starViews[starIndex]
            val starTag = starView.tag as? String
            val starName = getStarName(starNumber)

            // 별 이미지 설정
            val currentStarImage = if (starTag != null) {
                // 수집된 별자리인 경우
                val parts = starTag.split("|")
                if (parts.isNotEmpty()) {
                    val emotionType = parts[0]
                    val emotion = try {
                        EmotionType.valueOf(emotionType.uppercase())
                    } catch (e: Exception) {
                        EmotionType.HAPPY
                    }
                    getStarImageResource(emotion)
                } else {
                    R.drawable.iv_star_happy
                }
            } else {
                // 미수집 별인 경우 반투명한 기본 이미지
                R.drawable.iv_star_happy
            }

            // 모달 내용 설정
            ivStarImage.setImageResource(currentStarImage)
            if (starTag == null) {
                ivStarImage.alpha = 0.3f // 미수집 별은 반투명
            } else {
                ivStarImage.alpha = 1.0f // 수집된 별은 완전 불투명
            }

            // 제목 설정
            val titlePrefix = if (starTag != null) "✨" else "⭐"
            tvStarTitle.text = "$titlePrefix $starName"

            // 상세 정보 설정
            if (starTag != null) {
                // 수집된 별자리인 경우
                val parts = starTag.split("|")
                if (parts.size >= 2) {
                    val emotionType = parts[0]
                    val date = parts[1]
                    
                    val emotionDisplayName = try {
                        EmotionType.valueOf(emotionType.uppercase()).displayName
                    } catch (e: Exception) {
                        emotionType
                    }
                    
                    tvStarInfo.text = "수집 날짜: $date\n감정: $emotionDisplayName"
                    tvStarDescription.text = "이 별은 당신의 소중한 감정이 담긴 별입니다! 🌟"
                } else {
                    tvStarInfo.text = "수집된 별자리"
                    tvStarDescription.text = "당신의 감정이 별이 되었습니다! 🌟"
                }
            } else {
                // 아직 수집되지 않은 별인 경우
                tvStarInfo.text = "아직 수집되지 않음"
                tvStarDescription.text = "새로운 감정의 별을 수집해보세요! ✨"
            }

            // 모달 표시
            showStarModal()
            
            Log.d(TAG, "별 정보 모달 표시: $starName (${starTag ?: "미수집"})")

        } catch (e: Exception) {
            Log.e(TAG, "별 정보 표시 실패: ${e.message}")
        }
    }

    /**
     * 별자리 정보 모달 표시
     */
    private fun showStarModal() {
        try {
            layoutStarModal.visibility = View.VISIBLE
            layoutStarModal.alpha = 0f
            
            // 페이드인 애니메이션
            layoutStarModal.animate()
                .alpha(1f)
                .setDuration(300)
                .start()

            Log.d(TAG, "별자리 모달 표시됨")

        } catch (e: Exception) {
            Log.e(TAG, "별자리 모달 표시 실패: ${e.message}")
        }
    }

    /**
     * 별자리 정보 모달 숨기기
     */
    private fun hideStarModal() {
        try {
            // 페이드아웃 애니메이션
            layoutStarModal.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    layoutStarModal.visibility = View.GONE
                }
                .start()

            Log.d(TAG, "별자리 모달 숨겨짐")

        } catch (e: Exception) {
            Log.e(TAG, "별자리 모달 숨기기 실패: ${e.message}")
        }
    }

    /**
     * 별 번호에 따른 별 이름 반환 (북두칠성)
     */
    private fun getStarName(starNumber: Int): String {
        return when (starNumber) {
            1 -> "두베"
            2 -> "메라크"
            3 -> "페크다"
            4 -> "메그레즈"
            5 -> "알리오트"
            6 -> "미자르"
            7 -> "베네트나시"
            else -> "별"
        }
    }

    /**
     * 다음 화면으로 이동
     */
    private fun proceedToNext() {
        try {
            Log.d(TAG, "별자리 컬렉션 화면으로 이동")

            val intent = Intent(this, ConstellationActivity::class.java).apply {
                putExtra("userName", userName)
                putExtra("analyzedEmotion", analyzedEmotion?.name)
                putExtra("emotionDisplayName", emotionDisplayName)
                putExtra("fromPersonalMap", true) // 개인 맵에서 왔음을 표시
            }

            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Log.e(TAG, "다음 화면 이동 실패: ${e.message}")
        }
    }

    /**
     * 뒤로가기 처리 - 모달이 열려있으면 모달 닫기
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        try {
            if (layoutStarModal.visibility == View.VISIBLE) {
                // 모달이 열려있으면 모달 닫기
                Log.d(TAG, "뒤로가기 - 모달 닫기")
                hideStarModal()
            } else {
                // 모달이 닫혀있으면 다음 화면으로 이동
                Log.d(TAG, "뒤로가기 - 다음 화면으로 이동")
                proceedToNext()
            }
        } catch (e: Exception) {
            Log.e(TAG, "뒤로가기 처리 실패: ${e.message}")
            // 실패 시 기본 동작
            proceedToNext()
        }
        // super.onBackPressed() // 의도적으로 기본 동작을 막음
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ConstellationPersonalActivity 종료")
    }
}