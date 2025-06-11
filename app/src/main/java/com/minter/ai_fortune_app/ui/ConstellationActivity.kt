package com.minter.ai_fortune_app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.minter.ai_fortune_app.R
import com.minter.ai_fortune_app.data.model.EmotionType
import com.minter.ai_fortune_app.data.model.SajuCategory
import com.minter.ai_fortune_app.utils.DateUtils
import com.minter.ai_fortune_app.utils.SharedPreferencesUtils

/**
 * 별자리 수집 완료를 표시하는 액티비티
 *
 * 주요 기능:
 * 1. 미션 완료 후 수집된 별자리 표시
 * 2. 감정별 별 수집 완료 메시지
 * 3. 중복 수집 방지 및 안내
 * 4. MainActivity로 복귀
 *
 * 화면 플로우:
 * AcceptMissionActivity → ConstellationActivity → MainActivity
 */
class ConstellationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ConstellationActivity"
    }

    // ================================
    // UI 요소 정의
    // ================================
    private lateinit var lottieAnimation: LottieAnimationView
    private lateinit var tvTitle: TextView
    private lateinit var btnCollect: View
    private lateinit var btnList: View
    private lateinit var layoutModal: View
    private lateinit var tvModalMessage: TextView
    private lateinit var btnModalClose: View

    // ================================
    // 데이터 변수들
    // ================================
    private var userName: String = ""
    private var userBirthDate: String = ""
    private var sajuId: String = ""
    private var selectedCategory: SajuCategory = SajuCategory.DAILY
    private var categoryDisplayName: String = ""

    // 미션 및 감정 분석 결과
    private var missionId: String = ""
    private var missionCompleted: Boolean = false
    private var analyzedEmotion: EmotionType? = null
    private var emotionDisplayName: String = ""
    private var starCollected: Boolean = false
    private var collectionDate: String = ""

    // 위치 정보 (참고용)
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private var userAddress: String = ""

    // ================================
    // 상태 관리 변수들
    // ================================
    private var isNewCollection: Boolean = false       // 새로 수집했는지
    private var isAlreadyCollected: Boolean = false    // 오늘 이미 수집했는지

    // ================================
    // 액티비티 생명주기 함수들
    // ================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_constellation)

        Log.d(TAG, "ConstellationActivity 시작")

        // 초기화 순서 변경
        handleIntentData()
        initViews()
        setupButtonListeners()
        checkCollectionStatus()
        updateUI()

        Log.d(TAG, "ConstellationActivity 초기화 완료")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ConstellationActivity 종료")
    }

    // ================================
    // 데이터 처리 함수들
    // ================================

    private fun handleIntentData() {
        try {
            userName = intent.getStringExtra("userName") ?: "사용자"
            userBirthDate = intent.getStringExtra("userBirthDate") ?: "0000-00-00"
            sajuId = intent.getStringExtra("sajuId") ?: ""
            categoryDisplayName = intent.getStringExtra("categoryDisplayName") ?: "오늘의 사주"

            val categoryName = intent.getStringExtra("category") ?: "DAILY"
            selectedCategory = try {
                SajuCategory.valueOf(categoryName.uppercase())
            } catch (e: Exception) {
                Log.w(TAG, "알 수 없는 카테고리: $categoryName")
                SajuCategory.DAILY
            }

            // 미션 및 감정 분석 결과
            missionId = intent.getStringExtra("missionId") ?: ""
            missionCompleted = intent.getBooleanExtra("missionCompleted", false)
            starCollected = intent.getBooleanExtra("starCollected", false)
            collectionDate = intent.getStringExtra("collectionDate") ?: ""

            val emotionName = intent.getStringExtra("analyzedEmotion") ?: "HAPPY"
            analyzedEmotion = try {
                EmotionType.valueOf(emotionName)
            } catch (e: Exception) {
                Log.w(TAG, "알 수 없는 감정: $emotionName")
                EmotionType.HAPPY
            }

            emotionDisplayName = intent.getStringExtra("emotionDisplayName") ?: "Happy"

            // 위치 정보
            userLatitude = intent.getDoubleExtra("userLatitude", 0.0)
            userLongitude = intent.getDoubleExtra("userLongitude", 0.0)
            userAddress = intent.getStringExtra("userAddress") ?: "현재 위치"

            Log.d(TAG, "데이터 처리 완료 - 감정: $emotionDisplayName, 수집: $starCollected")

        } catch (e: Exception) {
            Log.e(TAG, "Intent 데이터 처리 실패: ${e.message}")
            userName = "사용자"
            emotionDisplayName = "Happy"
            analyzedEmotion = EmotionType.HAPPY
        }
    }

    // ================================
    // UI 초기화 함수들
    // ================================

    private fun initViews() {
        try {
            // 메인 UI 요소들
            lottieAnimation = findViewById(R.id.lottie_loading)
            tvTitle = findViewById(R.id.tv_title)
            btnCollect = findViewById(R.id.btn_select)
            btnList = findViewById(R.id.btn_show)

            // 모달 관련 요소들
            layoutModal = findViewById(R.id.layout_modal)
            tvModalMessage = findViewById(R.id.tv_modal_message)
            btnModalClose = findViewById(R.id.btn_modal_close)

            // 모달은 기본적으로 숨김 상태 유지
            layoutModal.visibility = View.GONE

            Log.d(TAG, "UI 요소 초기화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "UI 요소 초기화 실패: ${e.message}")
        }
    }

    private fun setupButtonListeners() {
        try {
            // 수집하기/이전으로 버튼
            btnCollect.setOnClickListener {
                Log.d(TAG, "버튼 클릭")

                // 이미 수집했는지 확인
                if (isAlreadyCollected) {
                    // 이미 수집한 경우 AcceptMissionActivity로 돌아가기
                    Log.d(TAG, "이미 수집함 - AcceptMissionActivity로 돌아가기")
                    returnToAcceptMissionActivity()
                } else {
                    // 수집하지 않은 경우 ConstellationSelectActivity로 이동
                    Log.d(TAG, "수집 시작 - ConstellationSelectActivity로 이동")
                    val intent = Intent(this, ConstellationSelectActivity::class.java).apply {
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
                    }
                    startActivity(intent)
                }
            }

            // 목록보기 버튼은 그대로 유지
            btnList.setOnClickListener {
                Log.d(TAG, "목록보기 버튼 클릭")
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
                }
                startActivity(intent)
            }

            // 모달 닫기 버튼
            btnModalClose.setOnClickListener {
                Log.d(TAG, "모달 닫기 버튼 클릭")
                hideModal()
            }

            // 모달 배경 클릭 시 닫기
            layoutModal.setOnClickListener {
                hideModal()
            }

            Log.d(TAG, "버튼 이벤트 설정 완료")

        } catch (e: Exception) {
            Log.e(TAG, "버튼 이벤트 설정 실패: ${e.message}")
        }
    }

    // ================================
    // 수집 상태 확인 로직
    // ================================

    private fun checkCollectionStatus() {
        try {
            // SharedPreferences에서 오늘 별자리 수집 여부 확인
            isAlreadyCollected = SharedPreferencesUtils.hasTodayConstellation(this)

            // ConstellationSelectActivity를 거쳐서 왔는지 확인
            val fromSelectActivity = intent.getBooleanExtra("fromSelectActivity", false)

            // 새로 수집한 것인지 확인
            isNewCollection = fromSelectActivity && isAlreadyCollected

            Log.d(TAG, "수집 상태 확인 - 새로 수집: $isNewCollection, 이미 수집: $isAlreadyCollected")

        } catch (e: Exception) {
            Log.e(TAG, "수집 상태 확인 실패: ${e.message}")
            isNewCollection = false
            isAlreadyCollected = false
        }
    }

    // ================================
    // UI 업데이트 함수들
    // ================================

    private fun updateUI() {
        try {
            // 제목 설정
            tvTitle.text = if (isNewCollection) {
                "축하합니다!\n${analyzedEmotion?.displayName ?: "Happy"} 별을\n수집했습니다! 🌟"
            } else if (isAlreadyCollected) {
                "오늘은 이미\n별을 수집했어요"
            } else {
                "당신의\n감정 별자리를\n수집해보세요"
            }

            // 버튼 텍스트 설정 - 이미 수집한 경우 "이전으로" 변경
            val btnCollectText = btnCollect.findViewById<TextView>(R.id.tv_btn_text)
            val btnListText = btnList.findViewById<TextView>(R.id.tv_btn_text)

            btnCollectText?.text = if (isNewCollection || isAlreadyCollected) "이전으로" else "수집하기"
            btnListText?.text = "목록보기"

            Log.d(TAG, "UI 업데이트 완료 - 새 수집: $isNewCollection, 이미 수집: $isAlreadyCollected, 감정: ${analyzedEmotion?.displayName}")

        } catch (e: Exception) {
            Log.e(TAG, "UI 업데이트 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    // ================================
    // 버튼 클릭 이벤트 처리 함수들
    // ================================

    private fun onMainButtonClicked() {
        try {
            Log.d(TAG, "메인으로 돌아가기 처리 시작")

            // MainActivity로 이동
            val intent = Intent(this, MainActivity::class.java).apply {
                // 모든 이전 액티비티를 스택에서 제거하고 새로 시작
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

                // 성공 완료 정보 전달
                putExtra("constellationCompleted", true)
                putExtra("completedEmotion", emotionDisplayName)
            }

            startActivity(intent)
            finish()

            Log.d(TAG, "MainActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "MainActivity 이동 실패: ${e.message}")
        }
    }

    private fun onListButtonClicked() {
        try {
            Log.d(TAG, "목록 보기 처리 (현재는 메인으로 이동)")

            // 현재는 메인으로 이동 (추후 별자리 목록 화면 구현 가능)
            onMainButtonClicked()

        } catch (e: Exception) {
            Log.e(TAG, "목록 보기 처리 실패: ${e.message}")
        }
    }

    // ================================
    // 모달 관련 함수들
    // ================================

    private fun showAlreadyCollectedModal() {
        try {
            Log.d(TAG, "이미 수집 모달 표시")

            // 모달 내용 업데이트
            tvModalMessage.text = "오늘은\n이미 수집하셨습니다."

            // 모달 표시
            layoutModal.visibility = View.VISIBLE
            layoutModal.alpha = 0f
            layoutModal.animate()
                .alpha(1f)
                .setDuration(300)
                .start()

        } catch (e: Exception) {
            Log.e(TAG, "모달 표시 실패: ${e.message}")
        }
    }

    private fun hideModal() {
        try {
            layoutModal.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    layoutModal.visibility = View.GONE
                    // 모달 닫힌 후 메인으로 이동
                    onMainButtonClicked()
                }
                .start()

        } catch (e: Exception) {
            Log.e(TAG, "모달 숨기기 실패: ${e.message}")
            layoutModal.visibility = View.GONE
            onMainButtonClicked()
        }
    }

    // ================================
    // 뒤로가기 처리
    // ================================

    override fun onBackPressed() {
        try {
            // 모달이 표시된 경우 모달만 닫기
            if (layoutModal.visibility == View.VISIBLE) {
                hideModal()
            } else {
                // 일반적인 경우 메인으로 이동
                onMainButtonClicked()
            }

        } catch (e: Exception) {
            Log.e(TAG, "뒤로가기 처리 실패: ${e.message}")
            super.onBackPressed()
        }
    }

    // AcceptMissionActivity로 돌아가는 함수 수정
    private fun returnToAcceptMissionActivity() {
        try {
            Log.d(TAG, "AcceptMissionActivity로 돌아가기")
            
            // 명시적으로 AcceptMissionActivity로 이동
            val intent = Intent(this, AcceptMissionActivity::class.java).apply {
                // 기존 AcceptMissionActivity로 돌아가기 (스택 위의 모든 Activity 제거)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                
                // 필요한 데이터 전달
                putExtra("userName", userName)
                putExtra("userBirthDate", userBirthDate)
                putExtra("sajuId", sajuId)
                putExtra("category", selectedCategory.name)
                putExtra("categoryDisplayName", categoryDisplayName)
                putExtra("missionId", missionId)
                putExtra("missionCompleted", missionCompleted)
                putExtra("analyzedEmotion", analyzedEmotion?.name)
                putExtra("emotionDisplayName", emotionDisplayName)
                putExtra("userLatitude", userLatitude)
                putExtra("userLongitude", userLongitude)
                putExtra("userAddress", userAddress)
                
                // ConstellationActivity에서 돌아왔다는 플래그
                putExtra("fromConstellation", true)
            }
            
            startActivity(intent)
            overridePendingTransition(0, 0) // 애니메이션 비활성화
            finish()
            
            Log.d(TAG, "AcceptMissionActivity로 돌아가기 완료")
            
        } catch (e: Exception) {
            Log.e(TAG, "AcceptMissionActivity 돌아가기 실패: ${e.message}")
            finish() // 실패 시 기본 동작
        }
    }
}