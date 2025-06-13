package com.minter.ai_fortune_app.ui

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.text.Layout
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.minter.ai_fortune_app.R
import com.minter.ai_fortune_app.api.repository.OpenAIRepository
import com.minter.ai_fortune_app.data.model.EmotionType
import com.minter.ai_fortune_app.data.model.*
import com.minter.ai_fortune_app.utils.DateUtils
import com.minter.ai_fortune_app.utils.SharedPreferencesUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 미션 진행 및 완료를 관리하는 액티비티
 *
 * 주요 기능:
 * 1. 수락된 미션의 상세 정보 표시
 * 2. 미션 완료 버튼 제공
 * 3. 미션 완료 시 감정 분석 진행
 * 4. 별자리 수집 및 ConstellationActivity로 이동
 * 5. 푸시 알림 스케줄링 (오후 9시)
 *
 * 화면 플로우:
 * RecommendActionActivity → AcceptMissionActivity → ConstellationActivity
 */
class AcceptMissionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AcceptMissionActivity"
        private const val MISSION_CHECK_TIME_HOUR = 21
        private const val MISSION_CHECK_TIME_MINUTE = 0
        private const val DEFAULT_MISSION_DURATION_HOURS = 3L
        private const val ANIMATION_DELAY_MS = 3000L
        private const val AUTO_PROCEED_DELAY_MS = 5000L
    }

    // ================================
    // UI 요소 정의
    // ================================
    private lateinit var tvUserName: TextView
    private lateinit var tvMissionTitle: TextView
    private lateinit var tvRemainingTime: TextView
    private lateinit var btnMissionComplete: View
    private lateinit var btnViewConstellation: View
    private lateinit var layoutMissionSuccess: View
    private lateinit var btnRetryMission: View
    private lateinit var ivStarImage: ImageView
    private lateinit var tvStarTitle: TextView
    private lateinit var tvStarInfo: TextView
    private lateinit var tvStarDescription: TextView

    // ================================
    // 데이터 변수들
    // ================================
    private var userName: String = ""
    private var userBirthDate: String = ""
    private var sajuId: String = ""
    private var chatSessionId: String = ""
    private var selectedCategory: SajuCategory = SajuCategory.DAILY
    private var categoryDisplayName: String = ""

    // 미션 관련 데이터
    private var currentMission: Mission? = null
    private var missionId: String = ""
    private var missionTitle: String = ""
    private var missionDescription: String = ""
    private var missionLocation: String = ""
    private var missionStatus: MissionStatus = MissionStatus.ACCEPTED

    // 위치 정보
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private var userAddress: String = ""

    // 채팅에서 받아온 사용자 메시지들 (감정 분석용)
    private var userMessages: Array<String> = emptyArray()

    // ================================
    // 상태 관리 변수들
    // ================================
    private var isMissionCompleted: Boolean = false
    private var isEmotionAnalyzed: Boolean = false
    private var analyzedEmotion: EmotionType? = null
    private var collectedStar: Star? = null
    private var emotionDisplayName: String = ""

    // ================================
    // 타이머 관련
    // ================================
    private var missionCountDownTimer: CountDownTimer? = null

    // ================================
    // API 관련
    // ================================
    private val openAIRepository = OpenAIRepository.getInstance()

    // ================================
    // 액티비티 생명주기 함수들
    // ================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accept_mission)

        handleIntentData()
        initViews()
        setupButtonListeners()
        displayMissionInfo()

        // 조건부 상태 복원
        conditionalStateRestore()

        scheduleMissionCheckNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMissionTimer()
        Log.d(TAG, "AcceptMissionActivity 종료")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()  // 기본 뒤로가기 동작 사용
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // 새로운 Intent 설정
        
        try {
            val fromConstellation = intent?.getBooleanExtra("fromConstellation", false) ?: false
            val fromConstellationPersonal = intent?.getBooleanExtra("fromConstellationPersonal", false) ?: false
            
            Log.d(TAG, "onNewIntent 호출 - fromConstellation: $fromConstellation, fromConstellationPersonal: $fromConstellationPersonal")
            
            if (fromConstellation || fromConstellationPersonal) {
                // Intent 데이터 다시 처리
                handleIntentData()
                // 조건부 상태 복원
                conditionalStateRestore()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "onNewIntent 처리 실패: ${e.message}")
        }
    }

    // ================================
    // 데이터 처리 함수들
    // ================================

    private fun handleIntentData() {
        try {
            // Intent에서 사용자 정보 먼저 시도
            userName = intent.getStringExtra("userName") ?: ""
            userBirthDate = intent.getStringExtra("userBirthDate") ?: ""
            
            // Intent에서 사용자 정보가 없거나 기본값이면 SharedPreferences에서 복원
            if (userName.isEmpty() || userBirthDate.isEmpty() || userBirthDate == "0000-00-00") {
                Log.d(TAG, "Intent에서 사용자 정보 부족 - SharedPreferences에서 복원 시도")
                val savedUserInfo = SharedPreferencesUtils.getUserInfo(this)
                if (savedUserInfo != null) {
                    val (savedName, savedBirthDate) = savedUserInfo
                    if (userName.isEmpty()) userName = savedName
                    if (userBirthDate.isEmpty() || userBirthDate == "0000-00-00") userBirthDate = savedBirthDate
                    Log.d(TAG, "사용자 정보 복원 완료 - 이름: $userName, 생년월일: $userBirthDate")
                } else {
                    Log.w(TAG, "저장된 사용자 정보 없음 - 기본값 사용")
                    if (userName.isEmpty()) userName = "사용자"
                    if (userBirthDate.isEmpty()) userBirthDate = "0000-00-00"
                }
            }

            sajuId = intent.getStringExtra("sajuId") ?: ""
            chatSessionId = intent.getStringExtra("chatSessionId") ?: ""
            categoryDisplayName = intent.getStringExtra("categoryDisplayName") ?: "오늘의 사주"

            val categoryName = intent.getStringExtra("category") ?: "DAILY"
            selectedCategory = try {
                SajuCategory.valueOf(categoryName)
            } catch (e: Exception) {
                Log.w(TAG, "알 수 없는 카테고리: $categoryName")
                SajuCategory.DAILY
            }

            missionId = intent.getStringExtra("missionId") ?: ""
            missionTitle = intent.getStringExtra("missionTitle") ?: "오늘의 미션"
            missionDescription = intent.getStringExtra("missionDescription") ?: "미션을 수행해보세요!"
            missionLocation = intent.getStringExtra("missionLocation") ?: "현재 위치"

            val statusName = intent.getStringExtra("missionStatus") ?: "ACCEPTED"
            missionStatus = try {
                MissionStatus.valueOf(statusName)
            } catch (e: Exception) {
                Log.w(TAG, "알 수 없는 미션 상태: $statusName")
                MissionStatus.ACCEPTED
            }

            userLatitude = intent.getDoubleExtra("userLatitude", 0.0)
            userLongitude = intent.getDoubleExtra("userLongitude", 0.0)
            userAddress = intent.getStringExtra("userAddress") ?: "현재 위치"

            userMessages = intent.getStringArrayExtra("userMessages") ?: emptyArray()

            currentMission = Mission(
                id = missionId,
                title = missionTitle,
                description = missionDescription,
                location = missionLocation,
                status = missionStatus
            )

            // 디버그 로그 추가
            Log.d(TAG, "=== Intent 데이터 확인 ===")
            Log.d(TAG, "사용자 이름: $userName")
            Log.d(TAG, "사용자 생년월일: $userBirthDate")
            Log.d(TAG, "미션 제목: $missionTitle")
            Log.d(TAG, "미션 상태: $missionStatus")
            Log.d(TAG, "==========================")

            Log.d(TAG, "데이터 처리 완료 - 미션: $missionTitle, 상태: $missionStatus")

        } catch (e: Exception) {
            Log.e(TAG, "Intent 데이터 처리 실패: ${e.message}")
            userName = "사용자"
            missionTitle = "오늘의 미션"
            missionDescription = "미션을 수행해보세요!"
            missionStatus = MissionStatus.ACCEPTED // 기본값을 ACCEPTED로 설정
            userBirthDate = "0000-00-00" // 최후의 기본값
        }
    }

    // ================================
    // UI 초기화 함수들
    // ================================

    private fun initViews() {
        try {
            tvUserName = findViewById(R.id.tv_user_name)
            tvMissionTitle = findViewById(R.id.tv_mission_title)
            tvRemainingTime = findViewById(R.id.tv_remaining_time)
            btnMissionComplete = findViewById(R.id.btn_mission_complete)
            btnViewConstellation = findViewById(R.id.btn_view_constellation)
            layoutMissionSuccess = findViewById(R.id.layout_mission_success)
            btnRetryMission = findViewById(R.id.btn_retry_mission)
            ivStarImage = findViewById(R.id.iv_star_image)
            tvStarTitle = findViewById(R.id.tv_star_title)
            tvStarInfo = findViewById(R.id.tv_star_info)
            tvStarDescription = findViewById(R.id.tv_star_description)

            // 모달 숨기기
            layoutMissionSuccess.visibility = View.GONE
            layoutMissionSuccess.alpha = 0f

            // 미션 완료 버튼 텍스트 설정
            var btnMissionCompleteText = btnMissionComplete.findViewById<TextView>(R.id.tv_btn_text)
            btnMissionCompleteText.text = "미션 완료"

            // 하단 버튼 텍스트를 "결과 다시 보기"로 설정
            val btnRetryMissionText = btnRetryMission.findViewById<TextView>(R.id.tv_btn_text)
            btnRetryMissionText?.text = "결과 다시 보기"

            Log.d(TAG, "UI 요소 초기화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "UI 요소 초기화 실패: ${e.message}")
        }
    }

    private fun setupButtonListeners() {
        try {
            btnMissionComplete.setOnClickListener {
                Log.d(TAG, "미션 완료 버튼 클릭")
                onMissionCompleteClicked()
            }

            btnViewConstellation.setOnClickListener {
                Log.d(TAG, "별자리 보기 버튼 클릭")
                onViewConstellationClicked()
            }

            layoutMissionSuccess.setOnClickListener {
                hideMissionSuccessModal()
            }

            btnRetryMission.setOnClickListener {
                Log.d(TAG, "결과 다시 보기 버튼 클릭")
                onResultReviewClicked()
            }

            // 디버그 메뉴 추가 - 사용자 이름을 더블탭하면 디버그 메뉴 열기
            setupDebugMenu()

            Log.d(TAG, "버튼 이벤트 설정 완료")

        } catch (e: Exception) {
            Log.e(TAG, "버튼 이벤트 설정 실패: ${e.message}")
        }
    }

    /**
     * 디버그 메뉴 설정 (더블탭으로 활성화)
     */
    private fun setupDebugMenu() {
        try {
            var lastTapTime = 0L
            val doubleTapThreshold = 500L // 0.5초 내 더블탭

            tvUserName.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                
                if (currentTime - lastTapTime < doubleTapThreshold) {
                    // 더블탭 감지 - 디버그 메뉴 표시
                    showDebugMenu()
                }
                
                lastTapTime = currentTime
            }

            Log.d(TAG, "🔧 디버그 메뉴 설정 완료 (사용자 이름 더블탭으로 활성화)")

        } catch (e: Exception) {
            Log.e(TAG, "디버그 메뉴 설정 실패: ${e.message}")
        }
    }

    /**
     * 디버그 메뉴 다이얼로그 표시
     */
    private fun showDebugMenu() {
        try {
            Log.d(TAG, "🔧 디버그 메뉴 열기")

            val options = arrayOf(
                "내일로 이동 (+1일) → MainActivity",
                "모레로 이동 (+2일) → MainActivity",
                "3일 후로 이동 (+3일) → MainActivity",
                "1주일 후로 이동 (+7일) → MainActivity",
                "어제로 이동 (-1일) → MainActivity",
                "오늘로 돌아가기 (초기화) → MainActivity",
                "현재 설정 확인만"
            )

            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("🔧 디버그 메뉴 - 날짜 시뮬레이션")
            builder.setItems(options) { dialog, which ->
                when (which) {
                    0 -> setDebugDateAndGoToMain(1, "내일")
                    1 -> setDebugDateAndGoToMain(2, "모레")
                    2 -> setDebugDateAndGoToMain(3, "3일 후")
                    3 -> setDebugDateAndGoToMain(7, "1주일 후")
                    4 -> setDebugDateAndGoToMain(-1, "어제")
                    5 -> resetDebugDateAndGoToMain()
                    6 -> showCurrentDebugStatus()
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
            
            val dialog = builder.create()
            dialog.show()

        } catch (e: Exception) {
            Log.e(TAG, "디버그 메뉴 표시 실패: ${e.message}")
        }
    }

    /**
     * 디버그 날짜 설정 후 MainActivity로 이동
     */
    private fun setDebugDateAndGoToMain(offsetDays: Int, description: String) {
        try {
            DateUtils.setDebugDayOffset(offsetDays)
            
            val currentDebugDate = DateUtils.getCurrentDate()
            
            Log.d(TAG, "🔧 디버그 날짜 설정 완료 - $description ($currentDebugDate)")
            
            // MainActivity로 이동하는 다이얼로그 표시
            val message = "날짜가 $description 으로 설정되었습니다\n\n" +
                         "현재 앱 날짜: $currentDebugDate\n" +
                         "오프셋: ${if (offsetDays > 0) "+$offsetDays" else "$offsetDays"}일\n\n" +
                         "새로운 날짜 테스트를 위해 MainActivity로 이동합니다."

            val confirmBuilder = android.app.AlertDialog.Builder(this)
            confirmBuilder.setTitle("테스트 시작")
            confirmBuilder.setMessage(message)
            confirmBuilder.setPositiveButton("MainActivity로 이동") { _, _ ->
                goToMainActivity()
            }
            confirmBuilder.setNegativeButton("현재 화면 유지") { _, _ ->
                // 현재 화면에 그대로 있기
                showMessage("날짜 설정 완료. 현재 화면에서 계속 진행합니다.")
            }
            confirmBuilder.show()

        } catch (e: Exception) {
            Log.e(TAG, "디버그 날짜 설정 실패: ${e.message}")
            showMessage("날짜 설정 중 오류가 발생했습니다.")
        }
    }

    /**
     * 디버그 날짜 초기화 후 MainActivity로 이동
     */
    private fun resetDebugDateAndGoToMain() {
        try {
            DateUtils.clearDebugMode()
            
            val currentDate = DateUtils.getCurrentDate()
            
            Log.d(TAG, "🔧 디버그 날짜 초기화 완료")
            
            // MainActivity로 이동하는 다이얼로그 표시
            val message = "날짜가 오늘로 초기화되었습니다.\n\n" +
                         "현재 앱 날짜: $currentDate\n" +
                         "디버그 모드: 비활성화\n\n" +
                         "정상 날짜로 테스트하기 위해 MainActivity로 이동합니다."

            val confirmBuilder = android.app.AlertDialog.Builder(this)
            confirmBuilder.setTitle("🏠 정상 모드 복귀")
            confirmBuilder.setMessage(message)
            confirmBuilder.setPositiveButton("MainActivity로 이동") { _, _ ->
                goToMainActivity()
            }
            confirmBuilder.setNegativeButton("현재 화면 유지") { _, _ ->
                showMessage("날짜 초기화 완료! 현재 화면에서 계속 진행합니다.")
            }
            confirmBuilder.show()

        } catch (e: Exception) {
            Log.e(TAG, "디버그 날짜 초기화 실패: ${e.message}")
            showMessage("날짜 초기화 중 오류가 발생했습니다.")
        }
    }

    /**
     * MainActivity로 이동 (새로운 날짜 테스트를 위해)
     */
    private fun goToMainActivity() {
        try {
            Log.d(TAG, "MainActivity로 이동 시작")

            val intent = Intent(this, MainActivity::class.java).apply {
                // 새로운 태스크로 시작하고 기존 액티비티 스택 제거
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                
                // 디버그 모드임을 알리는 플래그 (선택사항)
                putExtra("debugMode", DateUtils.isDebugModeActive())
                putExtra("debugDate", DateUtils.getCurrentDate())
            }

            startActivity(intent)
            
            // 현재 액티비티 종료
            finish()

            Log.d(TAG, "MainActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "MainActivity로 이동 실패: ${e.message}")
            showMessage("MainActivity로 이동하는 중 오류가 발생했습니다.")
        }
    }

    /**
     * 현재 디버그 상태 확인
     */
    private fun showCurrentDebugStatus() {
        try {
            val isDebugActive = DateUtils.isDebugModeActive()
            val currentDate = DateUtils.getCurrentDate()
            val offset = DateUtils.getDebugDayOffset()
            
            val message = if (isDebugActive) {
                "🔧 디버그 모드 활성화\n\n" +
                "현재 앱 날짜: $currentDate\n" +
                "오프셋: ${if (offset > 0) "+$offset" else "$offset"}일\n\n" +
                "실제 시스템 날짜와 다를 수 있습니다."
            } else {
                "디버그 모드 비활성화\n\n" +
                "현재 앱 날짜: $currentDate\n" +
                "실제 시스템 날짜와 동일합니다."
            }
            
            showMessage(message)
            
            Log.d(TAG, "🔧 디버그 상태 확인 - 활성화: $isDebugActive, 날짜: $currentDate, 오프셋: $offset")

        } catch (e: Exception) {
            Log.e(TAG, "디버그 상태 확인 실패: ${e.message}")
            showMessage("상태 확인 중 오류가 발생했습니다.")
        }
    }

    private fun displayMissionInfo() {
        try {
            tvUserName.text = "${userName}님\n오늘도 화이팅!"
            tvMissionTitle.text = missionTitle

            Log.d(TAG, "미션 정보 표시 완료")

        } catch (e: Exception) {
            Log.e(TAG, "미션 정보 표시 실패: ${e.message}")
            tvUserName.text = "${userName}님\n오늘도 화이팅!"
            tvMissionTitle.text = "오늘의 미션"
        }
    }

    private fun updateUIForMissionStatus() {
        try {
            // 사용자가 앱을 한 번이라도 사용했는지 확인 (사주 생성 여부)
            val hasUsedAppBefore = SharedPreferencesUtils.getTodaySajuResult(this) != null || 
                                   SharedPreferencesUtils.getTodaySajuId(this) != null

            when (missionStatus) {
                MissionStatus.ACCEPTED -> {
                    // 미션 수락 상태 - 아직 완료하지 않음
                    btnMissionComplete.visibility = View.VISIBLE
                    
                    // 미션을 완료하지 않았으므로 별자리 보기 버튼 숨김
                    btnViewConstellation.visibility = View.GONE

                    val btnText = btnMissionComplete.findViewById<TextView>(R.id.tv_btn_text)
                    btnText?.text = "미션 완료"
                    
                    // 미션명과 남은시간을 기본 상태로 설정
                    updateMissionAppearanceForStatus(false)
                }

                MissionStatus.COMPLETED -> {
                    // 미션 완료 상태 - 별자리 보기 가능
                    btnMissionComplete.visibility = View.GONE  // 미션완료 버튼 숨기기
                    btnViewConstellation.visibility = View.VISIBLE

                    val btnText = btnViewConstellation.findViewById<TextView>(R.id.tv_btn_text)
                    btnText?.text = "별자리 보기"
                    
                    // 미션명을 녹색으로, 남은시간에 취소선 처리
                    updateMissionAppearanceForStatus(true)
                }

                MissionStatus.REWARD_RECEIVED -> {
                    // 보상 수령 완료 상태 - 별자리 보기 가능
                    btnMissionComplete.visibility = View.GONE
                    btnViewConstellation.visibility = View.VISIBLE

                    val btnText = btnViewConstellation.findViewById<TextView>(R.id.tv_btn_text)
                    btnText?.text = "내 별자리 보기"
                    
                    // 미션명을 녹색으로, 남은시간에 취소선 처리
                    updateMissionAppearanceForStatus(true)
                }

                else -> {
                    // 기타 상태 - 기본적으로 미션 완료 버튼만 표시
                    btnMissionComplete.visibility = View.VISIBLE
                    btnViewConstellation.visibility = View.GONE
                    
                    // 미션명과 남은시간을 기본 상태로 설정
                    updateMissionAppearanceForStatus(false)
                }
            }

            Log.d(TAG, "UI 상태 업데이트 완료 - 상태: $missionStatus, 별자리 보기 버튼: ${btnViewConstellation.visibility}")
            
            // 디버그 로그 추가
            debugCurrentUIState()

        } catch (e: Exception) {
            Log.e(TAG, "UI 상태 업데이트 실패: ${e.message}")
        }
    }

    /**
     * 미션 완료 상태에 따른 외관 업데이트
     * @param isCompleted 미션 완료 여부
     */
    private fun updateMissionAppearanceForStatus(isCompleted: Boolean) {
        try {
            Log.d(TAG, "미션 외관 업데이트 시작 - 완료 여부: $isCompleted")
            
            if (isCompleted) {
                // 미션 완료 시: 미션명을 녹색으로, 남은시간에 취소선
                tvMissionTitle.setTextColor(ContextCompat.getColor(this, R.color.green))
                tvRemainingTime.paintFlags = tvRemainingTime.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                
                // 강제로 UI 새로고침
                tvMissionTitle.invalidate()
                tvRemainingTime.invalidate()
                
                Log.d(TAG, "미션 완료 외관 적용 - 미션명: 녹색, 남은시간: 취소선")
            } else {
                // 미션 진행 중: 미션명을 기본 색상으로, 남은시간 취소선 제거
                tvMissionTitle.setTextColor(ContextCompat.getColor(this, R.color.white))
                tvRemainingTime.paintFlags = tvRemainingTime.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                
                // 강제로 UI 새로고침
                tvMissionTitle.invalidate()
                tvRemainingTime.invalidate()
                
                Log.d(TAG, "미션 진행 중 외관 적용 - 미션명: 기본 색상, 남은시간: 정상")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "미션 외관 업데이트 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    // ================================
    // 타이머 관련 함수들
    // ================================

    private fun startMissionTimer() {
        try {
            val totalTimeMillis = DEFAULT_MISSION_DURATION_HOURS * 60 * 60 * 1000

            missionCountDownTimer = object : CountDownTimer(totalTimeMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val hours = millisUntilFinished / (60 * 60 * 1000)
                    val minutes = (millisUntilFinished % (60 * 60 * 1000)) / (60 * 1000)
                    val seconds = (millisUntilFinished % (60 * 1000)) / 1000

                    val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    tvRemainingTime.text = "남은 시간: $timeString"
                }

                override fun onFinish() {
                    tvRemainingTime.text = "시간 종료!"
                }
            }

            missionCountDownTimer?.start()
            Log.d(TAG, "미션 타이머 시작")

        } catch (e: Exception) {
            Log.e(TAG, "미션 타이머 시작 실패: ${e.message}")
            tvRemainingTime.text = "시간 정보 없음"
        }
    }

    private fun stopMissionTimer() {
        try {
            missionCountDownTimer?.cancel()
            missionCountDownTimer = null
            Log.d(TAG, "미션 타이머 정리 완료")
        } catch (e: Exception) {
            Log.e(TAG, "미션 타이머 정리 실패: ${e.message}")
        }
    }

    // ================================
    // 버튼 클릭 이벤트 처리 함수들
    // ================================

    private fun onMissionCompleteClicked() {
        try {
            Log.d(TAG, "미션 완료 처리 시작")

            if (isMissionCompleted) {
                Log.w(TAG, "이미 완료된 미션")
                showMessage("이미 완료된 미션입니다!")
                return
            }

            completeMission()

        } catch (e: Exception) {
            Log.e(TAG, "미션 완료 처리 실패: ${e.message}")
            showMessage("미션 완료 처리 중 오류가 발생했습니다.")
        }
    }

    private fun completeMission() {
        try {
            currentMission = currentMission?.nextStatus()
            missionStatus = MissionStatus.COMPLETED
            isMissionCompleted = true

            // 미션 정보  저장
            SharedPreferencesUtils.saveTodayMission(
                context = this, 
                missionId = missionId,
                missionTitle = missionTitle,
                missionDescription = missionDescription,
                missionLocation = missionLocation
            )
            
            stopMissionTimer()
            
            // 미션 완료 시 남은시간 표시를 "완료됨!"으로 변경
            tvRemainingTime.text = "완료됨!"
            
            // UI 즉시 업데이트 (모달 표시 전에)
            updateUIForMissionStatus()
            
            // 감정 분석을 조건부로 시작 (이미 분석했다면 건너뛰기)
            checkAndStartEmotionAnalysis()
            
            // 잠시 UI 변경을 보여준 후 모달 표시
            Handler().postDelayed({
                showMissionSuccessModal()
            }, 500) // 0.5초 딜레이
            
            Log.d(TAG, "미션 완료 처리 완료")

        } catch (e: Exception) {
            Log.e(TAG, "미션 완료 처리 실패: ${e.message}")
        }
    }

    /**
     * 감정 분석 필요 여부 확인 후 시작
     */
    private fun checkAndStartEmotionAnalysis() {
        try {
            Log.d(TAG, "감정 분석 필요 여부 확인 시작")

            // 이미 오늘 감정 분석을 했는지 확인
            val savedEmotion = SharedPreferencesUtils.getTodayEmotionAnalysis(this)
            
            if (savedEmotion != null) {
                // 이미 분석된 감정이 있으면 그것을 사용
                val (savedEmotionType, savedEmotionDisplayName) = savedEmotion
                
                try {
                    analyzedEmotion = EmotionType.valueOf(savedEmotionType.uppercase())
                    this.emotionDisplayName = savedEmotionDisplayName
                    isEmotionAnalyzed = true
                    
                    Log.d(TAG, "저장된 감정 분석 결과 사용 - 감정: $savedEmotionType, 표시명: $savedEmotionDisplayName")
                } catch (e: Exception) {
                    Log.e(TAG, "저장된 감정 파싱 실패, 새로 분석: ${e.message}")
                    startEmotionAnalysis()
                }
            } else {
                // 아직 분석하지 않았으면 새로 분석
                Log.d(TAG, "감정 분석이 필요함 - 새로 시작")
                startEmotionAnalysis()
            }

        } catch (e: Exception) {
            Log.e(TAG, "감정 분석 확인 실패: ${e.message}")
            startEmotionAnalysis()
        }
    }

    private fun startEmotionAnalysis() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "새로운 감정 분석 시작")

                if (userMessages.isEmpty()) {
                    Log.w(TAG, "분석할 메시지가 없음 - 기본 감정 사용")
                    analyzedEmotion = EmotionType.HAPPY
                    emotionDisplayName = EmotionType.HAPPY.displayName
                    isEmotionAnalyzed = true
                    
                    // 감정 분석 결과 저장
                    SharedPreferencesUtils.saveTodayEmotionAnalysis(this@AcceptMissionActivity, 
                        analyzedEmotion!!.name, emotionDisplayName)
                    
                    return@launch
                }

                analyzedEmotion = openAIRepository.analyzeEmotion(userMessages.toList())
                emotionDisplayName = analyzedEmotion?.displayName ?: EmotionType.HAPPY.displayName
                isEmotionAnalyzed = true

                // 감정 분석 결과 저장
                SharedPreferencesUtils.saveTodayEmotionAnalysis(this@AcceptMissionActivity, 
                    analyzedEmotion!!.name, emotionDisplayName)

                Log.d(TAG, "새로운 감정 분석 완료 및 저장 - 결과: $emotionDisplayName")

            } catch (e: Exception) {
                Log.e(TAG, "감정 분석 실패: ${e.message}")
                analyzedEmotion = EmotionType.HAPPY
                emotionDisplayName = EmotionType.HAPPY.displayName
                isEmotionAnalyzed = true
                
                // 기본 감정도 저장
                SharedPreferencesUtils.saveTodayEmotionAnalysis(this@AcceptMissionActivity, 
                    EmotionType.HAPPY.name, EmotionType.HAPPY.displayName)
            }
        }
    }

    /**
     * 별자리 보기 버튼 클릭 시 처리 (수정된 버전 - 별 수집 여부 우선 확인)
     */
    private fun onViewConstellationClicked() {
        try {
            Log.d(TAG, "별자리 보기 처리 시작")

            // 미션 완료 여부 다시 한번 확인
            if (missionStatus != MissionStatus.COMPLETED && missionStatus != MissionStatus.REWARD_RECEIVED) {
                Log.w(TAG, "미션 미완료 상태 - 별자리 보기 차단")
                showMessage("먼저 미션을 완료해주세요!")
                return
            }

            // 미션이 실제로 완료되었는지 SharedPreferences에서도 확인
            if (!SharedPreferencesUtils.hasTodayMission(this)) {
                Log.w(TAG, "SharedPreferences에서 미션 미완료 - 별자리 보기 차단")
                showMessage("먼저 미션을 완료해주세요!")
                return
            }

            // 별자리를 이미 수집했다면 감정 분석 확인 없이 바로 이동
            val hasCollectedStar = SharedPreferencesUtils.hasTodayConstellation(this)
            if (hasCollectedStar) {
                Log.d(TAG, "별을 이미 수집함 - 감정 분석 확인 없이 바로 ConstellationActivity로 이동")
                
                // 저장된 감정 분석 결과 복원 (표시용)
                val savedEmotion = SharedPreferencesUtils.getTodayEmotionAnalysis(this)
                if (savedEmotion != null) {
                    val (savedEmotionType, savedEmotionDisplayName) = savedEmotion
                    try {
                        analyzedEmotion = EmotionType.valueOf(savedEmotionType.uppercase())
                        this.emotionDisplayName = savedEmotionDisplayName
                        isEmotionAnalyzed = true
                        Log.d(TAG, "저장된 감정 분석 결과 복원: $savedEmotionType")
                    } catch (e: Exception) {
                        Log.e(TAG, "저장된 감정 복원 실패: ${e.message}")
                        // 실패해도 기본값으로 진행
                        analyzedEmotion = EmotionType.HAPPY
                        this.emotionDisplayName = EmotionType.HAPPY.displayName
                        isEmotionAnalyzed = true
                    }
                } else {
                    // 저장된 감정이 없어도 기본값으로 진행 (이미 별을 수집했으므로)
                    analyzedEmotion = EmotionType.HAPPY
                    this.emotionDisplayName = EmotionType.HAPPY.displayName
                    isEmotionAnalyzed = true
                    Log.d(TAG, "저장된 감정 정보 없음 - 기본값으로 진행")
                }
                
                // 바로 별자리 화면으로 이동
                proceedToConstellationFlow()
                return
            }

            // 별을 아직 수집하지 않은 경우에만 감정 분석 확인
            Log.d(TAG, "별을 아직 수집하지 않음 - 감정 분석 상태 확인")

            // 저장된 감정 분석 결과가 있는지 먼저 확인
            if (!isEmotionAnalyzed) {
                val savedEmotion = SharedPreferencesUtils.getTodayEmotionAnalysis(this)
                if (savedEmotion != null) {
                    val (savedEmotionType, savedEmotionDisplayName) = savedEmotion
                    try {
                        analyzedEmotion = EmotionType.valueOf(savedEmotionType.uppercase())
                        this.emotionDisplayName = savedEmotionDisplayName
                        isEmotionAnalyzed = true
                        Log.d(TAG, "저장된 감정 분석 결과 복원: $savedEmotionType")
                    } catch (e: Exception) {
                        Log.e(TAG, "저장된 감정 복원 실패: ${e.message}")
                        // 실패해도 기본값으로 진행
                        analyzedEmotion = EmotionType.HAPPY
                        this.emotionDisplayName = EmotionType.HAPPY.displayName
                        isEmotionAnalyzed = true
                    }
                }
                
                // 바로 별자리 화면으로 이동
                proceedToConstellationFlow()
                return
            }

            // 감정 분석이 완료되지 않았다면 대기
            if (!isEmotionAnalyzed) {
                Log.d(TAG, "감정 분석 진행 중 - 잠시 대기")
                showMessage("감정 분석이 진행 중입니다. 잠시만 기다려주세요...")
                waitForEmotionAnalysisAndProceed()
                return
            }

            // 모든 조건을 만족하면 별자리 화면으로 이동
            proceedToConstellationFlow()

        } catch (e: Exception) {
            Log.e(TAG, "별자리 보기 처리 실패: ${e.message}")
            showMessage("별자리 화면으로 이동하는 중 오류가 발생했습니다.")
        }
    }

    /**
     * 감정 분석 완료를 기다린 후 별자리 화면으로 이동
     */
    private fun waitForEmotionAnalysisAndProceed() {
        lifecycleScope.launch {
            try {
                var waitTime = 0
                val maxWaitTime = 10000 // 최대 10초 대기
                
                while (!isEmotionAnalyzed && waitTime < maxWaitTime) {
                    delay(100) // 0.1초마다 확인
                    waitTime += 100
                }
                
                if (isEmotionAnalyzed) {
                    Log.d(TAG, "감정 분석 완료 - 별자리 화면으로 이동")
                    proceedToConstellationFlow()
                } else {
                    Log.w(TAG, "감정 분석 시간 초과 - 기본 감정으로 진행")
                    analyzedEmotion = EmotionType.HAPPY
                    emotionDisplayName = EmotionType.HAPPY.displayName
                    isEmotionAnalyzed = true
                    proceedToConstellationFlow()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "감정 분석 대기 실패: ${e.message}")
                proceedToConstellationFlow()
            }
        }
    }

    /**
     * 별자리 수집 플로우 진행 (수정된 버전)
     */
    private fun proceedToConstellationFlow() {
        try {
            Log.d(TAG, "별자리 수집 플로우 시작")

            val emotion = analyzedEmotion ?: EmotionType.HAPPY

            // 항상 ConstellationActivity로 이동 (이쁘니까! ✨)
            // ConstellationActivity에서 수집 여부에 따라 적절한 UI와 다음 화면을 결정
            Log.d(TAG, "ConstellationActivity로 이동 (별 수집 여부와 관계없이)")
            proceedToConstellationActivity()

        } catch (e: Exception) {
            Log.e(TAG, "별자리 수집 플로우 실패: ${e.message}")
        }
    }

    private fun showMissionSuccessModal() {
        try {
            Log.d(TAG, "미션 성공 모달창 표시")

            updateMissionSuccessModalContent()

            layoutMissionSuccess.visibility = View.VISIBLE
            layoutMissionSuccess.alpha = 0f
            layoutMissionSuccess.animate()
                .alpha(1f)
                .setDuration(300)
                .start()

        } catch (e: Exception) {
            Log.e(TAG, "미션 성공 모달창 표시 실패: ${e.message}")
        }
    }

    private fun updateMissionSuccessModalContent() {
        try {
            val modalTitle = layoutMissionSuccess.findViewById<TextView>(R.id.tv_modal_title)
            val modalDescription = layoutMissionSuccess.findViewById<TextView>(R.id.tv_modal_description)

            // 모달 내부의 버튼 찾기
            val modalInnerLayout = layoutMissionSuccess.findViewById<View>(R.id.layout_mission_success_modal)
            val modalButton = modalInnerLayout?.findViewById<View>(R.id.btn_modal_ok)
            val modalButtonText = modalButton?.findViewById<TextView>(R.id.tv_btn_text)

            // 미션 완료 시 아이콘을 ic_success로 변경
            val modalIcon = modalInnerLayout?.findViewById<ImageView>(R.id.iv_modal_icon)
            modalIcon?.setImageResource(R.drawable.ic_success)

            modalButtonText?.text = "확인"
            modalTitle?.text = "미션 성공!"
            modalDescription?.text = "축하합니다! 미션을 성공적으로 완료하셨습니다.\n감정 분석을 통해 별자리를 수집하세요! ✨"

            Log.d(TAG, "모달 내용 업데이트 완료 - 아이콘: ic_success")

        } catch (e: Exception) {
            Log.e(TAG, "모달 내용 업데이트 실패: ${e.message}")
        }
    }

    private fun hideMissionSuccessModal() {
        try {
            layoutMissionSuccess.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    layoutMissionSuccess.visibility = View.GONE
                }
                .start()
            
            // 모달을 닫은 후에는 자동으로 이동하지 않음
            // 사용자가 "별자리 보기" 버튼을 직접 클릭할 때까지 대기
            Log.d(TAG, "미션 완료 모달 닫힘 - AcceptMissionActivity에 머무름")
            
        } catch (e: Exception) {
            Log.e(TAG, "모달 숨기기 실패: ${e.message}")
            layoutMissionSuccess.visibility = View.GONE
        }
    }

    // ================================
    // 알림 스케줄링 관련 함수들
    // ================================

    private fun scheduleMissionCheckNotification() {
        try {
            Log.d(TAG, "미션 확인 알림 스케줄링")

            val calendar = java.util.Calendar.getInstance()
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(java.util.Calendar.MINUTE)

            calendar.set(java.util.Calendar.HOUR_OF_DAY, MISSION_CHECK_TIME_HOUR)
            calendar.set(java.util.Calendar.MINUTE, MISSION_CHECK_TIME_MINUTE)
            calendar.set(java.util.Calendar.SECOND, 0)

            if (currentHour > MISSION_CHECK_TIME_HOUR ||
                (currentHour == MISSION_CHECK_TIME_HOUR && currentMinute > MISSION_CHECK_TIME_MINUTE)) {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }

            Log.d(TAG, "미션 확인 알림 스케줄링 완료 - 시간: ${calendar.time}")

        } catch (e: Exception) {
            Log.e(TAG, "미션 확인 알림 스케줄링 실패: ${e.message}")
        }
    }

    // ================================
    // 화면 이동 관련 함수들
    // ================================

    private fun proceedToConstellationActivity() {
        try {
            Log.d(TAG, "ConstellationActivity로 이동 시작")

            val intent = Intent(this, ConstellationActivity::class.java).apply {
                putExtra("userName", userName)
                putExtra("userBirthDate", userBirthDate)
                putExtra("sajuId", sajuId)
                putExtra("category", selectedCategory.name)
                putExtra("categoryDisplayName", categoryDisplayName)
                putExtra("missionId", missionId)
                putExtra("missionCompleted", isMissionCompleted)
                putExtra("analyzedEmotion", analyzedEmotion?.name)
                putExtra("emotionDisplayName", emotionDisplayName)
                putExtra("starCollected", collectedStar != null)
                putExtra("collectedDate", collectedStar?.collectedDate ?: "")
                putExtra("userLatitude", userLatitude)
                putExtra("userLongitude", userLongitude)
                putExtra("userAddress", userAddress)
            }

            startActivity(intent)
            // 앞으로 이동하는 애니메이션 (오른쪽에서 슬라이드 인)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()

            Log.d(TAG, "ConstellationActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "ConstellationActivity 이동 실패: ${e.message}")
            showMessage("별자리 화면으로 이동하는 중 오류가 발생했습니다.")
        }
    }

    private fun proceedToConstellationPersonalActivity() {
        try {
            Log.d(TAG, "ConstellationPersonalActivity로 이동 시작")

            val intent = Intent(this, ConstellationPersonalActivity::class.java).apply {
                putExtra("userName", userName)
                putExtra("userBirthDate", userBirthDate)
                putExtra("sajuId", sajuId)
                putExtra("category", selectedCategory.name)
                putExtra("categoryDisplayName", categoryDisplayName)
                putExtra("userLatitude", userLatitude)
                putExtra("userLongitude", userLongitude)
                putExtra("userAddress", userAddress)
                putExtra("analyzedEmotion", analyzedEmotion?.name)
                putExtra("emotionDisplayName", emotionDisplayName)
                putExtra("fromAcceptMission", true)
            }

            startActivity(intent)
            overridePendingTransition(0, 0) // 애니메이션 비활성화
            finish()

            Log.d(TAG, "ConstellationPersonalActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "ConstellationPersonalActivity 이동 실패: ${e.message}")
            showMessage("별자리 개인 맵으로 이동하는 중 오류가 발생했습니다.")
        }
    }

    // ================================
    // 결과 다시 보기 관련 함수들
    // ================================

    /**
     * "결과 다시 보기" 버튼 클릭 시 처리하는 함수
     */
    private fun onResultReviewClicked() {
        try {
            Log.d(TAG, "결과 다시 보기 처리 시작")

            // SharedPreferences에서 오늘의 사주 결과 가져오기
            val savedSajuResult = SharedPreferencesUtils.getTodaySajuResult(this)

            if (savedSajuResult != null) {
                // 사주 결과가 있으면 SajuResultActivity로 이동
                proceedToSajuResultActivity(savedSajuResult)
            } else {
                Log.w(TAG, "저장된 사주 결과가 없음")
                showMessage("오늘의 사주 결과를 찾을 수 없습니다. 사주를 다시 생성해주세요.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "결과 다시 보기 처리 실패: ${e.message}")
            showMessage("사주 결과를 불러오는 중 오류가 발생했습니다.")
        }
    }

    /**
     * SajuResultActivity로 이동하는 함수
     */
    private fun proceedToSajuResultActivity(sajuContent: String) {
        try {
            Log.d(TAG, "SajuResultActivity로 이동 시작")

            val intent = Intent(this, SajuResultActivity::class.java).apply {
                putExtra("userName", userName)
                putExtra("userBirthDate", userBirthDate)
                putExtra("sajuId", sajuId)
                putExtra("category", selectedCategory.name)
                putExtra("categoryDisplayName", categoryDisplayName)
                putExtra("sajuContent", sajuContent)
                putExtra("userLatitude", userLatitude)
                putExtra("userLongitude", userLongitude)
                putExtra("userAddress", userAddress)
                putExtra("fromAcceptMission", true)
            }

            startActivity(intent)
            // 앞으로 이동하는 애니메이션
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

            Log.d(TAG, "SajuResultActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "SajuResultActivity 이동 실패: ${e.message}")
            showMessage("사주 결과 화면으로 이동하는 중 오류가 발생했습니다.")
        }
    }

    // ================================
    // 유틸리티 함수들
    // ================================

    private fun showMessage(message: String) {
        try {
            Log.i(TAG, "사용자 메시지: $message")
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "메시지 표시 실패: ${e.message}")
        }
    }

    private fun showErrorMessage(message: String) {
        try {
            Log.w(TAG, "에러 메시지: $message")
            showMessage(message)
        } catch (e: Exception) {
            Log.e(TAG, "에러 메시지 표시 실패: ${e.message}")
        }
    }

    private fun showErrorAndFinish(message: String) {
        try {
            Log.e(TAG, "심각한 오류: $message")
            showMessage(message)

            lifecycleScope.launch {
                kotlinx.coroutines.delay(3000)
                returnToMainActivity()
            }

        } catch (e: Exception) {
            Log.e(TAG, "에러 처리 실패: ${e.message}")
            finish()
        }
    }

    private fun debugCurrentState() {
        try {
            Log.d(TAG, "=== AcceptMissionActivity 상태 ===")
            Log.d(TAG, "사용자: $userName")
            Log.d(TAG, "미션 ID: $missionId")
            Log.d(TAG, "미션 제목: $missionTitle")
            Log.d(TAG, "미션 상태: $missionStatus")
            Log.d(TAG, "완료 여부: $isMissionCompleted")
            Log.d(TAG, "감정 분석: $isEmotionAnalyzed")
            Log.d(TAG, "분석된 감정: ${analyzedEmotion?.displayName}")
            Log.d(TAG, "메시지 개수: ${userMessages.size}")
            Log.d(TAG, "========================================")

        } catch (e: Exception) {
            Log.e(TAG, "상태 디버깅 실패: ${e.message}")
        }
    }

    /**
     * 현재 UI 상태를 디버그 로그로 출력 (개선된 버전)
     */
    private fun debugCurrentUIState() {
        try {
            // SharedPreferences 상태 먼저 출력
            SharedPreferencesUtils.debugPrintAll(this)
            
            Log.d(TAG, "=== 현재 UI 상태 디버그 ===")
            Log.d(TAG, "미션 제목: '${tvMissionTitle.text}'")
            Log.d(TAG, "미션 제목 색상: ${tvMissionTitle.currentTextColor} (녹색: ${ContextCompat.getColor(this, R.color.green)})")
            Log.d(TAG, "남은 시간: '${tvRemainingTime.text}'")
            Log.d(TAG, "남은 시간 Paint Flags: ${tvRemainingTime.paintFlags}")
            Log.d(TAG, "Strike Through Flag: ${Paint.STRIKE_THRU_TEXT_FLAG}")
            Log.d(TAG, "미션 완료 버튼 visibility: ${btnMissionComplete.visibility} (GONE=${View.GONE}, VISIBLE=${View.VISIBLE})")
            Log.d(TAG, "별자리 보기 버튼 visibility: ${btnViewConstellation.visibility}")
            Log.d(TAG, "미션 상태: $missionStatus")
            Log.d(TAG, "미션 완료 플래그: $isMissionCompleted")
            Log.d(TAG, "타이머 실행 중: ${missionCountDownTimer != null}")
            Log.d(TAG, "=============================")
        } catch (e: Exception) {
            Log.e(TAG, "UI 상태 디버그 실패: ${e.message}")
        }
    }

    // ================================
    // 액티비티 생명주기 확장 함수들
    // ================================

    override fun onResume() {
        super.onResume()
        
        try {
            Log.d(TAG, "onResume - 가벼운 상태 확인")
            
            // 디버그 로그만 출력
            debugCurrentUIState()
            
            // 특별한 상태 복원은 하지 않음
            
        } catch (e: Exception) {
            Log.e(TAG, "onResume 처리 실패: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()

        try {
            Log.d(TAG, "onPause - 상태 저장")
            // TODO: Room DB나 SharedPreferences에 상태 저장

        } catch (e: Exception) {
            Log.e(TAG, "onPause 처리 실패: ${e.message}")
        }
    }

    /**
     * 조건에 따른 상태 복원 (수정된 버전 - 새 미션과 복원 구분)
     */
    private fun conditionalStateRestore() {
        try {
            val isFromMainActivity = intent.getBooleanExtra("fromMainActivity", false)
            val isFromConstellation = intent.getBooleanExtra("fromConstellation", false)
            val isFromConstellationPersonal = intent.getBooleanExtra("fromConstellationPersonal", false)
            val isFromChat = intent.getBooleanExtra("fromChat", false)
            val isNewMission = intent.getBooleanExtra("isNewMission", false) // 새로운 미션 여부
            val hasTodayMission = SharedPreferencesUtils.hasTodayMission(this)
            
            Log.d(TAG, "=== 조건부 상태 복원 시작 ===")
            Log.d(TAG, "fromMainActivity: $isFromMainActivity")
            Log.d(TAG, "fromConstellation: $isFromConstellation")
            Log.d(TAG, "fromConstellationPersonal: $isFromConstellationPersonal")
            Log.d(TAG, "fromChat: $isFromChat")
            Log.d(TAG, "isNewMission: $isNewMission")
            Log.d(TAG, "hasTodayMission: $hasTodayMission")
            Log.d(TAG, "현재 missionStatus: $missionStatus")
            
            when {
                // 새로운 미션을 수락해서 들어온 경우 - 무조건 ACCEPTED 상태로 시작
                isNewMission -> {
                    Log.d(TAG, "새로운 미션 수락 - ACCEPTED 상태로 시작")
                    missionStatus = MissionStatus.ACCEPTED
                    isMissionCompleted = false
                    updateUIForMissionStatus()
                    startMissionTimer()
                }
                
                // 채팅에서 온 경우 - 상태 복원하지 않음 (진행 중인 미션 유지)
                isFromChat -> {
                    Log.d(TAG, "채팅에서 돌아옴 - 기존 상태 유지")
                    updateUIForMissionStatus()
                    if (missionStatus == MissionStatus.ACCEPTED && !isMissionCompleted) {
                        startMissionTimer()
                    }
                }
                
                // MainActivity에서 온 경우이고 미션이 완료된 경우
                isFromMainActivity && hasTodayMission -> {
                    Log.d(TAG, "MainActivity에서 온 완료된 미션 - 상태 복원")
                    restoreMissionToCompleted()
                }
                
                // 별자리 관련 Activity에서 돌아온 경우이고 미션이 완료된 경우
                (isFromConstellation || isFromConstellationPersonal) && hasTodayMission -> {
                    Log.d(TAG, "별자리 화면에서 돌아온 완료된 미션 - 상태 복원")
                    restoreMissionToCompleted()
                }
                
                // 앱 재시작 등 기타 경우 - SharedPreferences 확인
                hasTodayMission -> {
                    Log.d(TAG, "앱 재시작 시 완료된 미션 감지 - 완료 상태로 복원")
                    restoreMissionToCompleted()
                }
                
                // 미션이 완료되지 않은 경우 - 진행 상태 유지
                else -> {
                    Log.d(TAG, "미완료 미션 - 진행 상태 유지")
                    updateUIForMissionStatus()
                    if (missionStatus == MissionStatus.ACCEPTED && !isMissionCompleted) {
                        startMissionTimer()
                    }
                }
            }
            
            Log.d(TAG, "=== 조건부 상태 복원 완료 ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "조건부 상태 복원 실패: ${e.message}")
            e.printStackTrace()
            // 실패 시 안전한 기본 동작 - 새 미션으로 시작
            Log.d(TAG, "복원 실패 - 새 미션으로 안전하게 시작")
            missionStatus = MissionStatus.ACCEPTED
            isMissionCompleted = false
            updateUIForMissionStatus()
            startMissionTimer()
        }
    }

    /**
     * 미션을 완료 상태로 복원 (감정 분석 포함)
     */
    private fun restoreMissionToCompleted() {
        try {
            Log.d(TAG, "미션 완료 상태로 복원 시작 (모달 없음)")
            
            // 모달을 확실히 숨기기 (먼저 처리)
            layoutMissionSuccess.visibility = View.GONE
            layoutMissionSuccess.alpha = 0f
            
            // 미션 상태를 완료로 변경
            missionStatus = MissionStatus.COMPLETED
            isMissionCompleted = true
            
            // ⭐ 저장된 미션 정보 복원
            val savedMissionInfo = SharedPreferencesUtils.getTodayMissionInfo(this)
            if (savedMissionInfo != null) {
                val (savedTitle, savedDescription, savedLocation) = savedMissionInfo
                
                // Intent에서 미션 정보가 없거나 기본값이면 복원
                if (missionTitle == "오늘의 미션" || missionTitle.isEmpty()) {
                    missionTitle = savedTitle
                    Log.d(TAG, "미션 제목 복원: $missionTitle")
                }
                if (missionDescription == "미션을 수행해보세요!" || missionDescription.isEmpty()) {
                    missionDescription = savedDescription
                    Log.d(TAG, "미션 설명 복원: $missionDescription")
                }
                if (missionLocation == "현재 위치" || missionLocation.isEmpty()) {
                    missionLocation = savedLocation
                    Log.d(TAG, "미션 위치 복원: $missionLocation")
                }
                
                // UI에 복원된 미션 제목 표시
                tvMissionTitle.text = missionTitle
            }
            
            // 타이머 정지 및 완료 텍스트 표시
            stopMissionTimer()
            tvRemainingTime.text = "완료됨!"
            
            // 감정 분석 결과 복원 (우선순위 높임)
            val savedEmotion = SharedPreferencesUtils.getTodayEmotionAnalysis(this)
            if (savedEmotion != null) {
                val (savedEmotionType, savedEmotionDisplayName) = savedEmotion
                try {
                    analyzedEmotion = EmotionType.valueOf(savedEmotionType.uppercase())
                    this.emotionDisplayName = savedEmotionDisplayName
                    isEmotionAnalyzed = true
                    Log.d(TAG, " 저장된 감정 분석 결과 복원: $savedEmotionDisplayName")
                } catch (e: Exception) {
                    Log.e(TAG, "감정 분석 결과 복원 실패: ${e.message}")
                    // 복원 실패 시 기본값 설정
                    analyzedEmotion = EmotionType.HAPPY
                    this.emotionDisplayName = EmotionType.HAPPY.displayName
                    isEmotionAnalyzed = true
                }
            } else {
                // 별을 이미 수집했다면 기본값이라도 설정
                val hasCollectedStar = SharedPreferencesUtils.hasTodayConstellation(this)
                if (hasCollectedStar) {
                    analyzedEmotion = EmotionType.HAPPY
                    this.emotionDisplayName = EmotionType.HAPPY.displayName
                    isEmotionAnalyzed = true
                    Log.d(TAG, "별 수집 완료 - 기본 감정으로 설정")
                }
            }
            
            // UI 업데이트만 수행 (모달 표시 없음)
            updateUIForMissionStatus()
            
            Log.d(TAG, "미션 완료 상태 복원 완료 (감정 분석 포함)")
            
            // 복원 후 UI 상태 확인
            debugCurrentUIState()
            
        } catch (e: Exception) {
            Log.e(TAG, "미션 완료 상태 복원 실패: ${e.message}")
        }
    }

    private fun returnToMainActivity() {
        try {
            Log.d(TAG, "MainActivity로 돌아가기")

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("hasError", true)
                putExtra("errorMessage", "미션 처리 중 오류가 발생했습니다.")
            }

            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Log.e(TAG, "MainActivity 돌아가기 실패: ${e.message}")
            finish()
        }
    }

    /**
     * 디버깅용: SharedPreferences 초기화
     * 테스트할 때 사용
     */
    private fun resetSharedPreferencesForDebug() {
        try {
            SharedPreferencesUtils.resetDailyLimits(this)
            Log.d(TAG, "SharedPreferences 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "SharedPreferences 초기화 실패: ${e.message}")
        }
    }

    // 모든 별을 기본 상태로 초기화 (반투명)
    private fun resetAllStars() {
        // 구현 코드 추가
    }

    // 수집된 별자리들을 순서대로 활성화
    private fun activateStarAtPosition(index: Int, emotionType: EmotionType, date: String) {
        // 구현 코드 추가
    }

    private fun showAlreadyCollectedModal() {
        // 별 이미지 숨기기
        ivStarImage.visibility = View.GONE
        
        // 제목에 메시지 설정
        tvStarTitle.text = "오늘은\n이미 수집하셨습니다."
        
        // 상세 정보와 설명 숨기기
        tvStarInfo.visibility = View.GONE
        tvStarDescription.visibility = View.GONE
        

    }
}