package com.minter.ai_fortune_app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.minter.ai_fortune_app.R
import com.minter.ai_fortune_app.data.model.*
import com.minter.ai_fortune_app.api.repository.OpenAIRepository
import com.minter.ai_fortune_app.utils.SharedPreferencesUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 사주 생성 중 로딩 화면을 표시하는 액티비티
 * - Lottie 애니메이션으로 로딩 효과 표시
 * - OpenAI API를 호출해서 사주 생성
 * - 완료되면 SajuResultActivity로 이동
 */
class LoadingActivity : AppCompatActivity() {

    // ================================
    // 상수 및 태그 정의
    // ================================

    companion object {
        private const val TAG = "LoadingActivity"
        private const val MIN_LOADING_TIME = 2000L // 최소 로딩 시간 (2초)
        private const val MAX_LOADING_TIME = 15000L // 최대 로딩 시간 (15초)
    }

    // ================================
    // 데이터 변수들
    // ================================

    private var userName: String = ""
    private var userBirthDate: String = ""
    private var selectedCategory: SajuCategory = SajuCategory.DAILY
    private var categoryDisplayName: String = ""

    // ================================
    // OpenAI Repository (수정됨)
    // ================================

    private val openAIRepository = OpenAIRepository.getInstance() // Repository 패턴 사용

    // ================================
    // 로딩 상태 관리 (타입 수정됨)
    // ================================

    private var isApiCallComplete: Boolean = false  // lateinit 제거
    private var isMinTimeComplete: Boolean = false  // lateinit 제거
    private var sajuResult: SajuResult? = null      // lateinit 제거

    // ================================
    // 액티비티 생명주기
    // ================================

    /**
     * 액티비티가 생성될 때 호출되는 함수
     * 로딩 애니메이션 시작 및 사주 생성 요청
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        Log.d(TAG, "LoadingActivity 시작")

        // 1. 전달받은 데이터 처리
        handleIntentData()

        // 2. 로딩 상태 초기화
        initLoadingState()

        // 3. 최소 로딩 시간 타이머 시작
        startMinLoadingTimer()

        // 4. 사주 생성 API 호출 시작 (추가됨)
        startSajuGeneration()

        // 5. 최대 시간 초과 타이머 시작
        startTimeoutTimer()
    }

    /**
     * 액티비티가 화면에서 사라질 때 호출
     * 메모리 정리 및 로그 출력
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LoadingActivity 종료")
    }

    /**
     * 뒤로가기 버튼을 막는 함수
     * 로딩 중에는 뒤로가기를 허용하지 않음
     */
    override fun onBackPressed() {
        // 로딩 중에는 뒤로가기 무시
        Log.d(TAG, "뒤로가기 버튼 클릭됨 - 로딩 중이므로 무시")
    }

    // ================================
    // 데이터 처리 함수들
    // ================================

    /**
     * Intent로 전달받은 데이터를 처리하는 함수
     * SajuInputActivity에서 보낸 사용자 정보와 카테고리 정보를 받아옴
     */
    private fun handleIntentData() {
        try {
            // Intent에서 사용자 정보 추출
            userName = intent.getStringExtra("userName") ?: "사용자"
            userBirthDate = intent.getStringExtra("userBirthDate") ?: "0000-00-00"

            // Intent에서 카테고리 정보 추출
            val categoryName = intent.getStringExtra("category") ?: "DAILY"
            categoryDisplayName = intent.getStringExtra("categoryDisplayName") ?: "오늘의 사주"

            // 문자열로 받은 카테고리를 enum으로 변환
            selectedCategory = try {
                SajuCategory.valueOf(categoryName)
            } catch (e: Exception) {
                Log.w(TAG, "알 수 없는 카테고리: $categoryName, 기본값 사용")
                SajuCategory.DAILY
            }

            Log.d(TAG, "데이터 처리 완료 - 이름: $userName, 생년월일: $userBirthDate, 카테고리: ${selectedCategory.displayName}")

        } catch (e: Exception) {
            Log.e(TAG, "Intent 데이터 처리 실패: ${e.message}")

            // 기본값으로 설정
            userName = "사용자"
            userBirthDate = "0000-00-00"
            selectedCategory = SajuCategory.DAILY
            categoryDisplayName = "오늘의 사주"
        }
    }

    /**
     * 로딩 상태를 초기화하는 함수
     * 모든 플래그를 false로 설정
     */
    private fun initLoadingState() {
        try {
            isApiCallComplete = false
            isMinTimeComplete = false
            sajuResult = null

            Log.d(TAG, "로딩 상태 초기화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "로딩 상태 초기화 실패: ${e.message}")
        }
    }

    // ================================
    // 타이머 관련 함수들
    // ================================

    /**
     * 최소 로딩 시간을 보장하는 타이머
     * 사용자 경험을 위해 너무 빨리 화면이 넘어가지 않도록 함
     */
    private fun startMinLoadingTimer() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "최소 로딩 시간 타이머 시작 (${MIN_LOADING_TIME}ms)")

                // 최소 시간만큼 대기
                delay(MIN_LOADING_TIME)

                // 최소 시간 완료 플래그 설정
                isMinTimeComplete = true

                Log.d(TAG, "최소 로딩 시간 완료")

                // API 호출도 완료되었다면 다음 화면으로 이동
                checkAndProceedToNext()

            } catch (e: Exception) {
                Log.e(TAG, "최소 로딩 시간 타이머 오류: ${e.message}")
            }
        }
    }

    /**
     * 최대 로딩 시간 초과를 방지하는 타이머
     * 네트워크 오류 등으로 무한 로딩을 방지
     */
    private fun startTimeoutTimer() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "타임아웃 타이머 시작 (${MAX_LOADING_TIME}ms)")

                // 최대 시간만큼 대기
                delay(MAX_LOADING_TIME)

                // 아직 완료되지 않았다면 타임아웃 처리
                if (!isApiCallComplete) {
                    Log.w(TAG, "API 호출 타임아웃 발생")
                    handleApiTimeout()
                }

            } catch (e: Exception) {
                Log.e(TAG, "타임아웃 타이머 오류: ${e.message}")
            }
        }
    }

    // ================================
    // 사주 생성 관련 함수들
    // ================================

    /**
     * OpenAI API를 사용해 사주를 생성하는 함수
     * 코루틴을 사용해 비동기로 처리
     */
    private fun startSajuGeneration() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "사주 생성 시작")

                // 사용자 정보 객체 생성
                val userInfo = UserInfo(
                    name = userName,
                    birthDate = userBirthDate
                )

                // SajuRequest 객체 생성
                val sajuRequest = SajuRequest(
                    userInfo = userInfo,
                    category = selectedCategory
                )

                // OpenAI Repository를 통한 API 호출 (String 반환)
                val sajuContent = openAIRepository.generateSaju(sajuRequest)

                // API 호출 성공 - SajuResult 객체 생성
                Log.d(TAG, "사주 생성 성공")

                // SajuResult 객체 직접 생성
                sajuResult = SajuResult(
                    userInfo = userInfo,
                    category = selectedCategory,
                    content = sajuContent
                )

                // API 호출 완료 플래그 설정
                isApiCallComplete = true

                // SharedPreferences에 오늘의 사주 기록
                SharedPreferencesUtils.saveTodaySaju(this@LoadingActivity, sajuResult!!.id)
                // 사주 내용도 저장
                SharedPreferencesUtils.saveTodaySajuResult(this@LoadingActivity, sajuContent)

                // 최소 시간도 완료되었다면 다음 화면으로 이동
                checkAndProceedToNext()

            } catch (e: Exception) {
                Log.e(TAG, "사주 생성 실패: ${e.message}")
                // 실패 시 기본 사주로 처리
                handleApiError(e)
            }
        }
    }

    /**
     * API 오류 발생 시 처리하는 함수
     * 기본 템플릿 사주를 생성해서 진행
     */
    private fun handleApiError(e: Exception) {
        try {
            Log.d(TAG, "기본 사주 템플릿 생성")

            // 기본 사주 내용 생성
            val defaultContent = createDefaultSajuContent()

            // 사용자 정보 객체 생성
            val userInfo = UserInfo(
                name = userName,
                birthDate = userBirthDate
            )

            // 기본 SajuResult 객체 생성
            sajuResult = SajuResult(
                userInfo = userInfo,
                category = selectedCategory,
                content = defaultContent
            )

            // API 호출 완료 플래그 설정
            isApiCallComplete = true

            // SharedPreferences에 기록
            SharedPreferencesUtils.saveTodaySaju(this, sajuResult!!.id)
            // 기본 사주 내용도 저장
            SharedPreferencesUtils.saveTodaySajuResult(this, defaultContent)

            Log.d(TAG, "기본 사주 생성 완료")

            // 다음 화면으로 이동 체크
            checkAndProceedToNext()

        } catch (e: Exception) {
            Log.e(TAG, "기본 사주 생성 실패: ${e.message}")
            // 최종 실패 시 에러 화면으로 이동
            proceedToErrorScreen()
        }
    }

    /**
     * API 타임아웃 발생 시 처리하는 함수
     * 기본 사주를 생성하고 진행
     */
    private fun handleApiTimeout() {
        try {
            Log.w(TAG, "API 타임아웃 - 기본 사주로 진행")

            // 가상의 타임아웃 예외 생성해서 처리
            val timeoutException = Exception("API 호출 시간 초과")
            handleApiError(timeoutException)

        } catch (e: Exception) {
            Log.e(TAG, "타임아웃 처리 실패: ${e.message}")
            proceedToErrorScreen()
        }
    }

    /**
     * 기본 사주 템플릿 내용을 생성하는 함수 (수정됨)
     * FallbackTemplate을 사용하여 일관된 기본 사주 제공
     */
    private fun createDefaultSajuContent(): String {
        return try {
            // FallbackTemplate 사용 (실제 API와 동일한 품질)
            when(selectedCategory) {
                SajuCategory.DAILY -> "${userName}님의 ${categoryDisplayName}\n\n오늘은 새로운 시작의 날입니다. 긍정적인 마음으로 하루를 시작해보세요. 작은 변화가 큰 행운을 가져다줄 것입니다. 주변 사람들과의 소통을 늘리고, 감사한 마음을 잊지 마세요. 오늘 하루가 당신에게 특별한 의미가 될 것입니다.\n\n뾰롱이가 더 자세한 이야기를 들려드릴 준비가 되어있습니다!"

                SajuCategory.LOVE -> "${userName}님의 ${categoryDisplayName}\n\n좋은 인연이 기다리고 있습니다. 자신감을 가지고 마음을 열어보세요. 진정한 사랑은 자연스럽게 찾아올 것입니다. 외모보다는 내면의 아름다움을 가꾸는 것이 중요합니다. 상대방을 이해하려는 노력이 관계를 더욱 깊게 만들어 줄 것입니다.\n\n뾰롱이가 더 자세한 이야기를 들려드릴 준비가 되어있습니다!"

                SajuCategory.STUDY -> "${userName}님의 ${categoryDisplayName}\n\n집중력이 높아지는 시기입니다. 계획을 세우고 꾸준히 노력해보세요. 노력한 만큼 좋은 결과가 따를 것입니다. 새로운 학습 방법을 시도해보는 것도 도움이 될 것입니다. 포기하지 않는 마음가짐이 성공의 열쇠가 될 것입니다.\n\n뾰롱이가 더 자세한 이야기를 들려드릴 준비가 되어있습니다!"

                SajuCategory.CAREER -> "${userName}님의 ${categoryDisplayName}\n\n새로운 기회가 다가오고 있습니다. 적극적으로 도전해보세요. 당신의 능력을 발휘할 때가 왔습니다. 동료들과의 협력이 성공의 중요한 요소가 될 것입니다. 창의적인 아이디어로 주변을 놀라게 할 수 있을 것입니다.\n\n뾰롱이가 더 자세한 이야기를 들려드릴 준비가 되어있습니다!"

                SajuCategory.HEALTH -> "${userName}님의 ${categoryDisplayName}\n\n몸과 마음의 균형을 맞추는 것이 중요합니다. 충분한 휴식을 취하고 규칙적인 생활을 해보세요. 적당한 운동과 건강한 식습관이 활력을 가져다 줄 것입니다. 스트레스 관리에 신경 쓰고, 긍정적인 마음가짐을 유지하세요.\n\n뾰롱이가 더 자세한 이야기를 들려드릴 준비가 되어있습니다!"
            }
        } catch (e: Exception) {
            Log.e(TAG, "기본 사주 생성 실패: ${e.message}")
            "${userName}님의 ${categoryDisplayName}\n\n오늘은 새로운 시작의 날입니다. 긍정적인 마음가짐으로 하루를 시작하시면 좋은 일들이 연이어 일어날 것입니다.\n\n뾰롱이가 더 자세한 이야기를 들려드릴 준비가 되어있습니다!"
        }
    }

    // ================================
    // 화면 이동 관련 함수들
    // ================================

    /**
     * 다음 화면으로 이동할 수 있는지 확인하고 이동하는 함수
     * API 호출 완료 + 최소 시간 완료 시에만 이동
     */
    private fun checkAndProceedToNext() {
        try {
            Log.d(TAG, "이동 조건 확인 - API: $isApiCallComplete, 시간: $isMinTimeComplete")

            // 두 조건이 모두 만족되면 다음 화면으로 이동
            if (isApiCallComplete && isMinTimeComplete) {
                Log.d(TAG, "이동 조건 만족 - SajuResultActivity로 이동")
                proceedToSajuResult()
            } else {
                Log.d(TAG, "이동 조건 미만족 - 대기 중")
            }

        } catch (e: Exception) {
            Log.e(TAG, "이동 조건 확인 실패: ${e.message}")
        }
    }

    /**
     * SajuResultActivity로 이동하는 함수
     * 생성된 사주 결과를 전달하며 이동
     */
    private fun proceedToSajuResult() {
        try {
            // 사주 결과가 없으면 에러 처리
            if (sajuResult == null) {
                Log.e(TAG, "사주 결과가 null - 에러 화면으로 이동")
                proceedToErrorScreen()
                return
            }

            Log.d(TAG, "SajuResultActivity로 이동 시작")

            val intent = Intent(this, SajuResultActivity::class.java).apply {
                // 사용자 정보 전달
                putExtra("userName", userName)
                putExtra("userBirthDate", userBirthDate)

                // 카테고리 정보 전달
                putExtra("category", selectedCategory.name)
                putExtra("categoryDisplayName", categoryDisplayName)

                // 생성된 사주 내용 전달
                putExtra("sajuContent", sajuResult!!.content)
                putExtra("sajuId", sajuResult!!.id)

                // 로딩에서 왔다는 플래그
                putExtra("fromLoading", true)
            }

            startActivity(intent)

            // 현재 액티비티 종료 (뒤로가기 방지)
            finish()

            Log.d(TAG, "SajuResultActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "SajuResultActivity 이동 실패: ${e.message}")
            proceedToErrorScreen()
        }
    }

    /**
     * 에러 화면으로 이동하는 함수
     * 모든 시도가 실패했을 때 최종적으로 호출
     */
    private fun proceedToErrorScreen() {
        try {
            Log.e(TAG, "에러 화면으로 이동")

            // MainActivity로 돌아가기 (에러 메시지와 함께)
            val intent = Intent(this, MainActivity::class.java).apply {
                // 모든 이전 액티비티 제거
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

                // 에러 정보 전달
                putExtra("hasError", true)
                putExtra("errorMessage", "사주 생성 중 오류가 발생했습니다. 다시 시도해주세요.")
            }

            startActivity(intent)
            finish()

            Log.d(TAG, "MainActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "에러 화면 이동 실패: ${e.message}")

            // 최종 수단으로 앱 종료
            finish()
        }
    }

    // ================================
    // 유틸리티 함수들
    // ================================

    /**
     * 디버깅용 함수
     * 현재 액티비티의 상태를 로그로 출력
     */
    private fun debugCurrentState() {
        Log.d(TAG, "=== LoadingActivity 상태 ===")
        Log.d(TAG, "사용자: $userName")
        Log.d(TAG, "생년월일: $userBirthDate")
        Log.d(TAG, "카테고리: ${selectedCategory.displayName}")
        Log.d(TAG, "API 완료: $isApiCallComplete")
        Log.d(TAG, "시간 완료: $isMinTimeComplete")
        Log.d(TAG, "사주 결과: ${if (sajuResult != null) "있음" else "없음"}")
        Log.d(TAG, "=============================")
    }
}