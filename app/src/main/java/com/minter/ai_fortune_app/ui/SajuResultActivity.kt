package com.minter.ai_fortune_app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.minter.ai_fortune_app.R
import com.minter.ai_fortune_app.data.model.SajuCategory
import com.minter.ai_fortune_app.utils.SharedPreferencesUtils

/**
 * 사주 결과를 표시하는 액티비티
 *
 * 주요 기능:
 * 1. 생성된 사주 내용을 타이핑 애니메이션으로 표시
 * 2. 사용자 정보 (이름, 생년월일) 표시
 * 3. "뾰롱이와 대화하기" 버튼으로 채팅 화면 이동
 * 4. 결과 공유 기능 (선택사항)
 *
 * 화면 플로우:
 * LoadingActivity → SajuResultActivity → ChatActivity
 */
class SajuResultActivity : AppCompatActivity() {

    // ================================
    // 상수 및 태그 정의
    // ================================

    companion object {
        private const val TAG = "SajuResultActivity"

        // 타이핑 애니메이션 관련 상수
        private const val TYPING_DELAY = 20L      // 각 글자가 나타나는 간격 (밀리초)
        private const val TYPING_START_DELAY = 300L // 타이핑 시작 전 대기 시간
    }

    // ================================
    // UI 요소 정의
    // ================================

    // XML 레이아웃의 뷰들을 연결할 변수들
    private lateinit var tvUserName: TextView        // 사용자 이름 표시
    private lateinit var tvUserBirthDate: TextView   // 사용자 생년월일 표시
    private lateinit var tvSajuResult: TextView      // 사주 결과 내용 표시
    private lateinit var btnChatWithGhost: View      // 뾰롱이와 대화하기 버튼

    // ================================
    // 데이터 변수들
    // ================================

    // Intent로 받아온 데이터를 저장할 변수들
    private var userName: String = ""                 // 사용자 이름
    private var userBirthDate: String = ""           // 사용자 생년월일
    private var sajuContent: String = ""             // 생성된 사주 내용
    private var sajuId: String = ""                  // 사주 고유 ID
    private var selectedCategory: SajuCategory = SajuCategory.DAILY // 선택된 카테고리
    private var categoryDisplayName: String = ""     // 카테고리 표시명

    // ================================
    // 타이핑 애니메이션 관련 변수들
    // ================================

    private var typingHandler: Handler? = null       // 타이핑 애니메이션을 위한 핸들러
    private var currentTypingIndex = 0              // 현재 타이핑된 글자의 인덱스
    private var isTypingComplete = false            // 타이핑 완료 여부

    // ================================
    // 액티비티 생명주기 함수들
    // ================================


    override fun onCreate(savedInstanceState: Bundle?) {
        // 부모 클래스의 onCreate 호출 (필수)
        super.onCreate(savedInstanceState)

        // XML 레이아웃 파일을 이 액티비티에 연결
        setContentView(R.layout.activity_saju_output)

        // 로그 출력 (디버깅용)
        Log.d(TAG, "SajuResultActivity 시작")

        // 1. Intent로 전달받은 데이터 처리
        handleIntentData()

        // 2. XML의 UI 요소들을 변수에 연결
        initViews()

        // 3. 사용자 정보를 화면에 표시
        displayUserInfo()

        // 4. 타이핑 애니메이션 시작 여부 결정
        val isFromAcceptMission = intent.getBooleanExtra("fromAcceptMission", false)
        if (isFromAcceptMission) {
            // AcceptMissionActivity에서 왔을 때는 애니메이션 없이 바로 텍스트 표시
            tvSajuResult.text = sajuContent
            isTypingComplete = true
        } else {
            // 다른 화면에서 왔을 때는 타이핑 애니메이션 시작
            startTypingAnimation()
        }

        Log.d(TAG, "SajuResultActivity 초기화 완료")
    }


    override fun onDestroy() {
        // 부모 클래스의 onDestroy 호출 (필수)
        super.onDestroy()

        // 타이핑 애니메이션 정리
        stopTypingAnimation()

        Log.d(TAG, "SajuResultActivity 종료")
    }

    /**
     * 뒤로가기 버튼이 눌렸을 때 호출
     */
    override fun onBackPressed() {
        val isFromAcceptMission = intent.getBooleanExtra("fromAcceptMission", false)
        if (isFromAcceptMission) {
            // AcceptMissionActivity로 돌아가기
            val intent = Intent(this, AcceptMissionActivity::class.java).apply {
                // 필요한 데이터 전달
                putExtra("userName", userName)
                putExtra("userBirthDate", userBirthDate)
                putExtra("sajuId", sajuId)
                putExtra("category", selectedCategory.name)
                putExtra("categoryDisplayName", categoryDisplayName)
            }
            startActivity(intent)
            finish() // 현재 액티비티 종료
        } else {
            super.onBackPressed() // 그 외의 경우 기본 뒤로가기 동작
        }
        Log.d(TAG, "뒤로가기 버튼 클릭 - fromAcceptMission: $isFromAcceptMission")
    }

    // ================================
    // 데이터 처리 함수들
    // ================================

    /**
     * Intent로 전달받은 데이터를 처리하는 함수
     *
     * Intent는 액티비티 간에 데이터를 전달하는 안드로이드의 메커니즘입니다.
     * LoadingActivity에서 보낸 사주 결과와 사용자 정보를 받아옵니다.
     */
    private fun handleIntentData() {
        try {
            // intent는 이 액티비티를 시작할 때 전달된 Intent 객체
            // getStringExtra()는 문자열 데이터를 가져오는 함수
            // ?: "기본값"은 null일 경우 기본값을 사용하는 Kotlin의 Elvis 연산자

            userName = intent.getStringExtra("userName") ?: "사용자"
            userBirthDate = intent.getStringExtra("userBirthDate") ?: "0000-00-00"
            sajuContent = intent.getStringExtra("sajuContent") ?: "사주 내용을 불러올 수 없습니다."
            sajuId = intent.getStringExtra("sajuId") ?: ""
            categoryDisplayName = intent.getStringExtra("categoryDisplayName") ?: "오늘의 사주"

            // 카테고리 enum 변환
            val categoryName = intent.getStringExtra("category") ?: "DAILY"
            selectedCategory = try {
                // valueOf()는 문자열을 enum으로 변환하는 함수
                SajuCategory.valueOf(categoryName)
            } catch (e: Exception) {
                // 예외 발생 시 기본값 사용
                Log.w(TAG, "알 수 없는 카테고리: $categoryName, 기본값 사용")
                SajuCategory.DAILY
            }

            // ${}는 Kotlin의 문자열 템플릿 - 변수 값을 문자열에 삽입
            Log.d(TAG, "데이터 처리 완료 - 이름: $userName, 카테고리: ${selectedCategory.displayName}")

        } catch (e: Exception) {
            // 예외(에러) 발생 시 로그 출력 및 기본값 설정
            Log.e(TAG, "Intent 데이터 처리 실패: ${e.message}")

            // 기본값으로 안전하게 설정
            userName = "사용자"
            userBirthDate = "0000-00-00"
            sajuContent = "사주 결과를 불러오는 중 오류가 발생했습니다."
            selectedCategory = SajuCategory.DAILY
            categoryDisplayName = "오늘의 사주"
        }
    }

    // ================================
    // UI 초기화 함수들
    // ================================

    /**
     * XML 레이아웃의 UI 요소들을 찾아서 변수에 연결하는 함수
     *
     * findViewById()는 XML의 android:id로 지정된 뷰를 찾는 함수입니다.
     * lateinit var로 선언된 변수들을 실제 뷰 객체와 연결합니다.
     */
    private fun initViews() {
        try {
            // R.id.xxx는 XML에서 android:id="@+id/xxx"로 정의된 뷰의 ID
            tvUserName = findViewById(R.id.tv_user_name)
            tvUserBirthDate = findViewById(R.id.tv_user_birthdate)
            tvSajuResult = findViewById(R.id.tv_saju_result)

            // 버튼은 component_no_glow_btn 레이아웃을 include한 것
            btnChatWithGhost = findViewById(R.id.btn_generate)

            // 버튼 텍스트 설정 (component_no_glow_btn 내부의 TextView)
            val btnText = btnChatWithGhost.findViewById<TextView>(R.id.tv_btn_text)
            
            // AcceptMissionActivity에서 왔는지 확인
            val isFromAcceptMission = intent.getBooleanExtra("fromAcceptMission", false)
            
            if (isFromAcceptMission) {
                btnText.text = "이전으로 돌아가기"
                btnChatWithGhost.setOnClickListener {
                    finish() // 단순히 이전 화면으로 돌아가기
                }
            } else {
                btnText.text = "뾰롱이와 대화하기"
                btnChatWithGhost.setOnClickListener {
                    startChatActivity()
                }
            }

            Log.d(TAG, "UI 요소 초기화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "UI 요소 초기화 실패: ${e.message}")
        }
    }

    /**
     * 사용자 정보를 화면에 표시하는 함수
     *
     * TextView.text에 값을 할당하면 화면에 텍스트가 표시됩니다.
     */
    private fun displayUserInfo() {
        try {
            // 생년월일 형식 변환 (YYYY-MM-DD → YYYY.MM.DD)
            val formattedBirthDate = userBirthDate.replace("-", ".")

            // TextView에 텍스트 설정
            tvUserBirthDate.text = formattedBirthDate
            tvUserName.text = "${userName}님의 ${categoryDisplayName}입니다."

            Log.d(TAG, "사용자 정보 표시 완료")

        } catch (e: Exception) {
            Log.e(TAG, "사용자 정보 표시 실패: ${e.message}")

            // 실패 시 기본 텍스트 표시
            tvUserBirthDate.text = "0000.00.00"
            tvUserName.text = "사용자님의 사주입니다."
        }
    }

    // ================================
    // 타이핑 애니메이션 관련 함수들
    // ================================

    /**
     * 타이핑 애니메이션을 시작하는 함수
     *
     * Handler는 안드로이드에서 시간 지연을 처리하는 클래스입니다.
     * Looper.getMainLooper()는 메인 스레드에서 UI 업데이트를 보장합니다.
     */
    private fun startTypingAnimation() {
        try {
            Log.d(TAG, "타이핑 애니메이션 시작")

            // 초기 상태 설정
            currentTypingIndex = 0
            isTypingComplete = false
            tvSajuResult.text = "" // 텍스트 초기화

            // Handler 생성 (메인 스레드에서 실행)
            typingHandler = Handler(Looper.getMainLooper())

            // 시작 전 잠시 대기 후 타이핑 시작
            typingHandler?.postDelayed({
                typeNextCharacter()
            }, TYPING_START_DELAY)

        } catch (e: Exception) {
            Log.e(TAG, "타이핑 애니메이션 시작 실패: ${e.message}")

            // 실패 시 전체 텍스트를 즉시 표시
            tvSajuResult.text = sajuContent
            isTypingComplete = true
        }
    }

    /**
     * 다음 글자를 타이핑하는 함수
     *
     * 재귀적으로 자기 자신을 호출하여 한 글자씩 순차적으로 표시합니다.
     */
    private fun typeNextCharacter() {
        try {
            // 모든 글자를 다 타이핑했는지 확인
            if (currentTypingIndex >= sajuContent.length) {
                // 타이핑 완료
                isTypingComplete = true
                Log.d(TAG, "타이핑 애니메이션 완료")
                return
            }

            // 현재 인덱스까지의 문자열을 가져와서 표시
            // substring(0, index)는 0번째부터 index-1번째까지의 문자열을 반환
            val currentText = sajuContent.substring(0, currentTypingIndex + 1)
            tvSajuResult.text = currentText

            // 다음 글자로 인덱스 증가
            currentTypingIndex++

            // 일정 시간 후 다음 글자 타이핑 (재귀 호출)
            typingHandler?.postDelayed({
                typeNextCharacter()
            }, TYPING_DELAY)

        } catch (e: Exception) {
            Log.e(TAG, "타이핑 애니메이션 오류: ${e.message}")

            // 오류 시 나머지 텍스트 모두 표시
            tvSajuResult.text = sajuContent
            isTypingComplete = true
        }
    }

    /**
     * 타이핑 애니메이션을 중지하는 함수
     *
     * 메모리 누수 방지를 위해 Handler의 모든 콜백을 제거합니다.
     */
    private fun stopTypingAnimation() {
        try {
            // Handler의 모든 대기 중인 작업 취소
            typingHandler?.removeCallbacksAndMessages(null)
            typingHandler = null

            Log.d(TAG, "타이핑 애니메이션 정리 완료")

        } catch (e: Exception) {
            Log.e(TAG, "타이핑 애니메이션 정리 실패: ${e.message}")
        }
    }

    /**
     * 타이핑 애니메이션을 즉시 완료하는 함수
     *
     * 사용자가 화면을 탭하거나 빨리 넘어가고 싶을 때 사용할 수 있습니다.
     */
    private fun completeTypingImmediately() {
        try {
            if (!isTypingComplete) {
                // 애니메이션 중지
                stopTypingAnimation()

                // 전체 텍스트 즉시 표시
                tvSajuResult.text = sajuContent
                isTypingComplete = true

                Log.d(TAG, "타이핑 애니메이션 즉시 완료")
            }

        } catch (e: Exception) {
            Log.e(TAG, "타이핑 즉시 완료 실패: ${e.message}")
        }
    }

    // ================================
    // 화면 이동 관련 함수들
    // ================================

    /**
     * ChatActivity로 이동하는 함수
     *
     * 뾰롱이와의 채팅 화면으로 사용자를 안내합니다.
     * 사주 → 채팅 → 미션 → 별자리 순서로 진행됩니다.
     */
    private fun startChatActivity() {
        try {
            Log.d(TAG, "ChatActivity로 이동 시작")

            // Intent 생성 (현재 액티비티에서 ChatActivity로 이동)
            val intent = Intent(this, ChatActivity::class.java).apply {
                // apply는 객체를 생성하면서 동시에 설정하는 Kotlin의 scope 함수

                // 사용자 정보 전달
                putExtra("userName", userName)
                putExtra("userBirthDate", userBirthDate)

                // 사주 정보 전달
                putExtra("sajuId", sajuId)
                putExtra("sajuContent", sajuContent)
                putExtra("category", selectedCategory.name)
                putExtra("categoryDisplayName", categoryDisplayName)

                // 채팅 시작 플래그
                putExtra("startNewChat", true)
            }

            // 액티비티 시작
            startActivity(intent)

            // 현재 액티비티 종료 (뒤로가기 방지)
            finish()

            Log.d(TAG, "ChatActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "ChatActivity 이동 실패: ${e.message}")
            showErrorMessage("채팅 화면으로 이동하는 중 오류가 발생했습니다.")
        }
    }

    /**
     * MainActivity로 돌아가는 함수 (필요시 사용)
     *
     * 사용자가 처음부터 다시 시작하고 싶을 때 사용할 수 있습니다.
     */
    private fun returnToMainActivity() {
        try {
            Log.d(TAG, "MainActivity로 돌아가기")

            val intent = Intent(this, MainActivity::class.java).apply {
                // 모든 이전 액티비티를 스택에서 제거하고 새로 시작
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Log.e(TAG, "MainActivity 돌아가기 실패: ${e.message}")
        }
    }

    // ================================
    // 유틸리티 함수들
    // ================================

    /**
     * 에러 메시지를 표시하는 함수
     *
     * 현재는 로그로만 출력하지만, 추후 Toast나 Dialog로 확장 가능합니다.
     */
    private fun showErrorMessage(message: String) {
        try {
            Log.w(TAG, "에러 메시지: $message")

            // TODO: 실제 앱에서는 Toast나 Snackbar로 사용자에게 표시
            // Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "에러 메시지 표시 실패: ${e.message}")
        }
    }

    /**
     * 현재 액티비티의 상태를 확인하는 함수 (디버깅용)
     *
     * 개발 중에 상태를 확인하기 위해 사용합니다.
     */
    private fun debugCurrentState() {
        try {
            Log.d(TAG, "=== SajuResultActivity 상태 ===")
            Log.d(TAG, "사용자: $userName")
            Log.d(TAG, "생년월일: $userBirthDate")
            Log.d(TAG, "카테고리: ${selectedCategory.displayName}")
            Log.d(TAG, "사주 ID: $sajuId")
            Log.d(TAG, "타이핑 완료: $isTypingComplete")
            Log.d(TAG, "사주 내용 길이: ${sajuContent.length}자")
            Log.d(TAG, "================================")

        } catch (e: Exception) {
            Log.e(TAG, "상태 디버깅 실패: ${e.message}")
        }
    }

    /**
     * 사주 내용 공유 기능 (선택사항)
     *
     * 사용자가 사주 결과를 다른 앱으로 공유할 수 있는 기능입니다.
     */
    private fun shareSajuResult() {
        try {
            Log.d(TAG, "사주 결과 공유 시작")

            // 공유할 텍스트 구성
            val shareText = """
                ${userName}님의 ${categoryDisplayName}
                
                $sajuContent
                
                - AI 포춘 앱에서 생성됨
            """.trimIndent()

            // 공유 Intent 생성
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "${userName}님의 ${categoryDisplayName}")
            }

            // 공유 앱 선택 다이얼로그 표시
            startActivity(Intent.createChooser(shareIntent, "사주 결과 공유하기"))

            Log.d(TAG, "사주 결과 공유 완료")

        } catch (e: Exception) {
            Log.e(TAG, "사주 결과 공유 실패: ${e.message}")
            showErrorMessage("공유 기능을 사용할 수 없습니다.")
        }
    }
}