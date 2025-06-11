package com.minter.ai_fortune_app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.minter.ai_fortune_app.R
import com.minter.ai_fortune_app.api.repository.OpenAIRepository
import com.minter.ai_fortune_app.data.model.ChatMessage
import com.minter.ai_fortune_app.data.model.ChatSession
import com.minter.ai_fortune_app.data.model.SajuCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.ViewTreeObserver
import android.graphics.Rect
import android.view.ViewGroup

/**
 * 뾰롱이와의 채팅 화면
 *
 * 주요 기능:
 * 1. 뾰롱이(AI 귀신)와 최대 3번의 대화
 * 2. 실시간 채팅 UI (RecyclerView 사용)
 * 3. OpenAI API를 통한 AI 응답 생성
 * 4. 채팅 완료 후 미션 화면으로 자동 이동
 * 5. 앱 종료 후에도 채팅 진행상태 유지 (추후 Room DB 연동)
 *
 * 화면 플로우:
 * SajuResultActivity → ChatActivity → MissionActivity
 */
class ChatActivity : AppCompatActivity() {

    // ================================
    // 상수 및 태그 정의
    // ================================

    companion object {
        // companion object는 Java의 static과 같은 개념
        // 클래스 이름으로 직접 접근할 수 있는 정적 멤버들을 정의
        private const val TAG = "ChatActivity"

        // const val은 컴파일 타임 상수를 의미
        // private는 이 클래스에서만 접근 가능
        private const val MAX_CONVERSATIONS = 3        // 최대 대화 횟수
        private const val GHOST_TYPING_DELAY = 1500L   // 뾰롱이 응답 대기 시간 (밀리초)
        private const val SCROLL_DELAY = 300L          // 스크롤 애니메이션 지연 시간
    }

    // ================================
    // UI 요소 정의
    // ================================

    // lateinit var는 나중에 초기화할 변수를 의미
    // 액티비티 생성 시점에는 null이지만, onCreate에서 반드시 초기화해야 함
    private lateinit var rvChatMessages: RecyclerView    // 채팅 메시지 목록
    private lateinit var etChatInput: EditText           // 채팅 입력창
    private lateinit var ivSendButton: ImageView         // 전송 버튼
    private lateinit var btnCancel: ImageView            // 취소 버튼 (X)
    private lateinit var layoutChatInput: View           // 입력창 레이아웃
    private lateinit var layoutCompleteButton: View      // 완료 버튼 레이아웃

    // ChatAdapter는 RecyclerView에서 채팅 메시지들을 표시하는 어댑터
    // (별도로 구현 필요)
    private lateinit var chatAdapter: ChatAdapter

    // ================================
    // 데이터 변수들
    // ================================

    // Intent로 받아온 데이터를 저장할 변수들
    private var userName: String = ""                     // 사용자 이름
    private var userBirthDate: String = ""               // 사용자 생년월일
    private var sajuId: String = ""                      // 사주 고유 ID
    private var sajuContent: String = ""                 // 사주 내용
    private var selectedCategory: SajuCategory = SajuCategory.DAILY  // 선택된 카테고리
    private var categoryDisplayName: String = ""         // 카테고리 표시명

    // ================================
    // 채팅 관련 데이터
    // ================================

    // 현재 채팅 세션 정보
    private var currentSession: ChatSession = ChatSession.createToday()

    // 채팅 메시지들을 저장하는 리스트
    // mutableListOf()는 수정 가능한 리스트를 생성
    private val chatMessages: MutableList<ChatMessage> = mutableListOf()

    // 현재 대화 횟수 (1~3)
    private var conversationCount: Int = 0

    // 채팅 완료 여부
    private var isChatCompleted: Boolean = false

    // ================================
    // API 관련
    // ================================

    // OpenAI API를 호출하기 위한 Repository
    // 싱글톤 패턴으로 구현되어 있음
    private val openAIRepository = OpenAIRepository.getInstance()

    // ================================
    // 액티비티 생명주기 함수들
    // ================================

    /**
     * 액티비티가 생성될 때 호출되는 함수
     *
     * onCreate는 액티비티의 생명주기에서 가장 먼저 호출되는 함수
     * 여기서 화면 초기화와 데이터 설정을 모두 수행
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // 부모 클래스의 onCreate 호출 (필수)
        super.onCreate(savedInstanceState)

        // XML 레이아웃 파일을 이 액티비티에 연결
        setContentView(R.layout.activity_chat)

        // 로그 출력 (개발자가 앱의 동작을 추적하기 위함)
        Log.d(TAG, "ChatActivity 시작")

        // 1. Intent로 전달받은 데이터 처리
        handleIntentData()

        // 2. XML의 UI 요소들을 변수에 연결
        initViews()

        // 3. RecyclerView 설정 (채팅 목록)
        setupRecyclerView()

        // 4. 버튼 클릭 이벤트 설정
        setupButtonListeners()

        // 5. 채팅 세션 초기화 및 복구
        initializeChatSession()

        // 6. 뾰롱이의 첫 인사말 표시
        startInitialGreeting()

        Log.d(TAG, "ChatActivity 초기화 완료")
    }

    /**
     * 액티비티가 완전히 종료될 때 호출
     * 메모리 누수 방지를 위한 정리 작업 수행
     */
    override fun onDestroy() {
        super.onDestroy()

        // 채팅 세션 상태 저장 (추후 Room DB 구현 시 사용)
        saveChatSession()

        Log.d(TAG, "ChatActivity 종료")
    }

    /**
     * 뒤로가기 버튼 처리
     * 채팅 중에는 뒤로가기를 제한
     */
    override fun onBackPressed() {
        if (isChatCompleted) {
            // 채팅이 완료된 경우에만 뒤로가기 허용
            super.onBackPressed()
        } else {
            // 채팅 중에는 뒤로가기 무시
            Log.d(TAG, "채팅 진행 중 - 뒤로가기 무시")
            showMessage("뾰롱이와의 대화를 완료해주세요!")
        }
    }

    // ================================
    // 데이터 처리 함수들
    // ================================

    /**
     * Intent로 전달받은 데이터를 처리하는 함수
     * SajuResultActivity에서 보낸 사용자 정보와 사주 정보를 받아옴
     */
    private fun handleIntentData() {
        try {
            // intent는 이 액티비티를 시작할 때 전달된 Intent 객체
            // getStringExtra()는 문자열 데이터를 가져오는 함수
            // ?: "기본값"은 null일 경우 기본값을 사용하는 Elvis 연산자

            userName = intent.getStringExtra("userName") ?: "사용자"
            userBirthDate = intent.getStringExtra("userBirthDate") ?: "0000-00-00"
            sajuId = intent.getStringExtra("sajuId") ?: ""
            sajuContent = intent.getStringExtra("sajuContent") ?: ""
            categoryDisplayName = intent.getStringExtra("categoryDisplayName") ?: "오늘의 사주"

            // 카테고리 enum 변환
            val categoryName = intent.getStringExtra("category") ?: "DAILY"
            selectedCategory = try {
                // valueOf()는 문자열을 enum으로 변환
                SajuCategory.valueOf(categoryName)
            } catch (e: Exception) {
                // 예외 발생 시 기본값 사용
                Log.w(TAG, "알 수 없는 카테고리: $categoryName")
                SajuCategory.DAILY
            }

            // 문자열 템플릿 사용: ${}로 변수 값을 문자열에 삽입
            Log.d(TAG, "데이터 처리 완료 - 사용자: $userName, 카테고리: ${selectedCategory.displayName}")

        } catch (e: Exception) {
            // 예외(에러) 발생 시 로그 출력 및 기본값 설정
            Log.e(TAG, "Intent 데이터 처리 실패: ${e.message}")

            // 안전한 기본값 설정
            userName = "사용자"
            userBirthDate = "0000-00-00"
            selectedCategory = SajuCategory.DAILY
            categoryDisplayName = "오늘의 사주"
        }
    }

    // ================================
    // UI 초기화 함수들
    // ================================

    /**
     * XML 레이아웃의 UI 요소들을 찾아서 변수에 연결하는 함수
     * findViewById()를 사용해 XML의 뷰들을 코틀린 변수와 연결
     */
    private fun initViews() {
        try {
            // activity_chat.xml에서 정의된 ID들
            rvChatMessages = findViewById(R.id.rv_chat_messages)
            btnCancel = findViewById(R.id.btn_chat_cancel)
            layoutChatInput = findViewById(R.id.layout_chat_input)
            layoutCompleteButton = findViewById(R.id.layout_complete_button)

            // component_chat_input_field.xml에서 추가한 ID들
            etChatInput = findViewById(R.id.et_chat_input)
            ivSendButton = findViewById(R.id.iv_send_button)

            // 초기 힌트 설정
            etChatInput.hint = "당신의 이야기를 들려주세요"

            Log.d(TAG, "UI 요소 초기화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "UI 요소 초기화 실패: ${e.message}")

            // 초기화 실패 시 앱이 크래시되지 않도록 처리
            showErrorAndFinish("화면 초기화에 실패했습니다.")
        }
    }

    /**
     * RecyclerView를 설정하는 함수
     * 채팅 메시지들을 스크롤 가능한 목록으로 표시
     */
    private fun setupRecyclerView() {
        try {
            // ChatAdapter 초기화 (채팅 메시지를 표시하는 어댑터)
            chatAdapter = ChatAdapter(chatMessages)

            // RecyclerView에 어댑터와 레이아웃 매니저 설정
            rvChatMessages.apply {
                // apply는 객체의 속성을 설정할 때 사용하는 scope 함수
                adapter = chatAdapter

                // LinearLayoutManager는 세로로 스크롤되는 리스트를 만들어줌
                layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                    // 최신 메시지가 아래쪽에 표시되도록 설정
                    stackFromEnd = false
                }
            }

            Log.d(TAG, "RecyclerView 설정 완료")

        } catch (e: Exception) {
            Log.e(TAG, "RecyclerView 설정 실패: ${e.message}")
        }
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수
     * 전송 버튼, 취소 버튼 등의 클릭 이벤트 처리
     */
    private fun setupButtonListeners() {
        try {
            // 전송 버튼 클릭 이벤트
            // setOnClickListener는 뷰가 클릭되었을 때 실행할 코드를 설정
            ivSendButton.setOnClickListener {
                Log.d(TAG, "전송 버튼 클릭")
                sendUserMessage()
            }

            // 취소 버튼 (X) 클릭 이벤트
            btnCancel.setOnClickListener {
                Log.d(TAG, "취소 버튼 클릭")
                showCancelDialog()
            }

            // 완료 버튼 클릭 이벤트 (채팅 완료 후 표시)
            layoutCompleteButton.setOnClickListener {
                Log.d(TAG, "완료 버튼 클릭")
                proceedToRecommendActionActivity()
            }

            // 입력창에서 엔터키 눌렀을 때 전송
            etChatInput.setOnEditorActionListener { _, actionId, _ ->
                // actionId는 키보드의 액션 (완료, 전송 등)
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                    sendUserMessage()
                    true  // true를 반환하면 이벤트 처리 완료를 의미
                } else {
                    false // false를 반환하면 기본 동작 수행
                }
            }

            Log.d(TAG, "버튼 이벤트 설정 완료")

        } catch (e: Exception) {
            Log.e(TAG, "버튼 이벤트 설정 실패: ${e.message}")
        }
    }

    // ================================
    // 채팅 세션 관련 함수들
    // ================================

    /**
     * 채팅 세션을 초기화하는 함수
     * 앱이 재시작되어도 이전 채팅 상태를 복구할 수 있도록 함
     */
    private fun initializeChatSession() {
        try {
            // TODO: Room DB에서 오늘의 채팅 세션 조회
            // 현재는 새로운 세션으로 시작
            currentSession = ChatSession.createToday()

            conversationCount = currentSession.conversationCount
            isChatCompleted = currentSession.isCompleted

            // 저장된 채팅 메시지들 복구 (TODO: Room DB 구현 후)
            // loadSavedMessages()

            Log.d(TAG, "채팅 세션 초기화 - 대화 횟수: $conversationCount, 완료: $isChatCompleted")

        } catch (e: Exception) {
            Log.e(TAG, "채팅 세션 초기화 실패: ${e.message}")

            // 실패 시 새 세션으로 시작
            currentSession = ChatSession.createToday()
            conversationCount = 0
            isChatCompleted = false
        }
    }

    /**
     * 채팅 세션 상태를 저장하는 함수
     * 앱이 종료되어도 진행상태가 유지되도록 함
     */
    private fun saveChatSession() {
        try {
            // TODO: Room DB에 현재 세션 상태 저장
            // chatSessionDao.updateSession(currentSession)

            Log.d(TAG, "채팅 세션 저장 완료")

        } catch (e: Exception) {
            Log.e(TAG, "채팅 세션 저장 실패: ${e.message}")
        }
    }

    // ================================
    // 채팅 메시지 관련 함수들
    // ================================

    /**
     * 뾰롱이의 첫 인사말을 표시하는 함수
     * 채팅 시작 시 자동으로 표시되는 환영 메시지
     */
    private fun startInitialGreeting() {
        try {
            // 이미 채팅이 진행 중이면 인사말 생략
            if (chatMessages.isNotEmpty()) {
                Log.d(TAG, "이미 채팅 진행 중 - 인사말 생략")
                return
            }

            Log.d(TAG, "뾰롱이 첫 인사말 표시")

            // 뾰롱이의 인사말 메시지
            val greetingMessage = """
                안녕, 나는 뾰롱이라고 해! 👻
                너의 답답한 마음을 나에게 털어놔봐.
                더 자세한 사주를 알려줄게. 나와 3번의 대화를 할 수 있어!
            """.trimIndent() // trimIndent()는 들여쓰기를 제거해줌

            // 뾰롱이 메시지 추가
            addGhostMessage(greetingMessage)

            // UI 상태 업데이트
            updateChatUI()

        } catch (e: Exception) {
            Log.e(TAG, "첫 인사말 표시 실패: ${e.message}")
        }
    }

    /**
     * 사용자 메시지를 전송하는 함수
     * 전송 버튼 클릭 시 호출됨
     */
    private fun sendUserMessage() {
        try {
            // 입력창에서 텍스트 가져오기
            val userInput = etChatInput.text.toString().trim()

            // 빈 메시지는 전송하지 않음
            if (userInput.isEmpty()) {
                Log.d(TAG, "빈 메시지 - 전송 취소")
                return
            }

            // 이미 채팅이 완료된 경우 전송 불가
            if (isChatCompleted) {
                Log.d(TAG, "채팅 완료됨 - 전송 불가")
                showMessage("이미 모든 대화가 완료되었습니다!")
                return
            }

            // 최대 대화 횟수 확인
            if (conversationCount >= MAX_CONVERSATIONS) {
                Log.d(TAG, "최대 대화 횟수 초과")
                showMessage("오늘의 대화가 모두 완료되었습니다!")
                return
            }

            Log.d(TAG, "사용자 메시지 전송: $userInput")

            // 1. 사용자 메시지를 채팅 목록에 추가
            addUserMessage(userInput)

            // 2. 입력창 초기화
            etChatInput.setText("")

            // 3. 대화 횟수 증가
            conversationCount++

            // 4. UI 업데이트
            updateChatUI()

            // 5. 뾰롱이 응답 요청 (비동기 처리)
            requestGhostResponse(userInput)

        } catch (e: Exception) {
            Log.e(TAG, "사용자 메시지 전송 실패: ${e.message}")
            showMessage("메시지 전송에 실패했습니다.")
        }
    }

    /**
     * 사용자 메시지를 채팅 목록에 추가하는 함수
     */
    private fun addUserMessage(message: String) {
        try {
            // ChatMessage 객체 생성
            val chatMessage = ChatMessage(
                sessionId = currentSession.id,    // 현재 채팅 세션 ID
                message = message,                // 메시지 내용
                isFromUser = true,               // 사용자가 보낸 메시지임을 표시
                timestamp = System.currentTimeMillis() // 현재 시간 (밀리초)
            )

            // 메시지 리스트에 추가
            chatMessages.add(chatMessage)

            // RecyclerView에 새 메시지 알림
            chatAdapter.notifyItemInserted(chatMessages.size - 1)

            // 최신 메시지로 스크롤
            scrollToBottom()

            Log.d(TAG, "사용자 메시지 추가 완료")

        } catch (e: Exception) {
            Log.e(TAG, "사용자 메시지 추가 실패: ${e.message}")
        }
    }

    /**
     * 뾰롱이 메시지를 채팅 목록에 추가하는 함수
     */
    private fun addGhostMessage(message: String) {
        try {
            val chatMessage = ChatMessage(
                sessionId = currentSession.id,
                message = message,
                isFromUser = false,              // 뾰롱이가 보낸 메시지
                timestamp = System.currentTimeMillis()
            )

            chatMessages.add(chatMessage)
            chatAdapter.notifyItemInserted(chatMessages.size - 1)
            scrollToBottom()

            Log.d(TAG, "뾰롱이 메시지 추가 완료")

        } catch (e: Exception) {
            Log.e(TAG, "뾰롱이 메시지 추가 실패: ${e.message}")
        }
    }

    /**
     * 뾰롱이의 응답을 요청하는 함수
     * OpenAI API를 사용해 AI 응답 생성
     */
    private fun requestGhostResponse(userMessage: String) {
        // lifecycleScope.launch는 코루틴을 시작하는 함수
        // 코루틴은 비동기 작업을 위한 코틀린의 기능
        lifecycleScope.launch {
            try {
                Log.d(TAG, "뾰롱이 응답 요청 시작")

                // 타이핑 중 표시 (선택사항)
                showGhostTyping()

                // 응답 생성을 위한 약간의 지연
                delay(GHOST_TYPING_DELAY)

                // OpenAI API 호출 (suspend 함수이므로 코루틴에서 호출)
                val ghostResponse = openAIRepository.generateChatResponse(
                    userMessage = userMessage,
                    conversationCount = conversationCount
                )

                // 타이핑 중 표시 제거
                hideGhostTyping()

                // 응답 메시지 추가
                addGhostMessage(ghostResponse)

                // 세션 상태 업데이트
                updateSessionAfterConversation()

                Log.d(TAG, "뾰롱이 응답 완료")

            } catch (e: Exception) {
                Log.e(TAG, "뾰롱이 응답 요청 실패: ${e.message}")

                // 오류 시 기본 응답 표시
                hideGhostTyping()
                addGhostMessage("앗, 잠깐 문제가 생겼어... 다시 말해줄래? 🥺")
            }
        }
    }

    /**
     * 대화 후 세션 상태를 업데이트하는 함수
     */
    private fun updateSessionAfterConversation() {
        try {
            // 세션 상태 업데이트
            currentSession = currentSession.nextConversation()

            // 완료 상태 확인
            if (conversationCount >= MAX_CONVERSATIONS) {
                isChatCompleted = true

                // 완료 메시지 표시
                addGhostMessage("우리의 대화가 끝났어! 이제 행운의 액션을 확인해보자! ✨")

                // UI를 완료 상태로 변경
                showCompletedState()
            }

            Log.d(TAG, "세션 상태 업데이트 - 대화: $conversationCount, 완료: $isChatCompleted")

        } catch (e: Exception) {
            Log.e(TAG, "세션 상태 업데이트 실패: ${e.message}")
        }
    }

    // ================================
    // UI 업데이트 관련 함수들
    // ================================

    /**
     * 채팅 UI를 현재 상태에 맞게 업데이트하는 함수
     */
    private fun updateChatUI() {
        try {
            // 남은 대화 횟수 표시 (선택사항)
            val remainingChats = MAX_CONVERSATIONS - conversationCount

            if (isChatCompleted) {
                // 채팅 완료 상태
                layoutChatInput.visibility = View.GONE     // 입력창 숨김
                layoutCompleteButton.visibility = View.VISIBLE  // 완료 버튼 표시
            } else {
                // 채팅 진행 중 상태
                layoutChatInput.visibility = View.VISIBLE
                layoutCompleteButton.visibility = View.GONE

                // 입력창 힌트 업데이트
                etChatInput.hint = "당신의 이야기를 들려주세요 ($remainingChats/3)"
            }

            Log.d(TAG, "채팅 UI 업데이트 완료")

        } catch (e: Exception) {
            Log.e(TAG, "채팅 UI 업데이트 실패: ${e.message}")
        }
    }

    /**
     * 채팅 완료 상태 UI로 변경하는 함수
     */
    private fun showCompletedState() {
        try {
            // 입력창 숨기고 완료 버튼 표시
            layoutChatInput.visibility = View.GONE
            layoutCompleteButton.visibility = View.VISIBLE

            // 완료 버튼 텍스트 설정
            val btnCompleteText = layoutCompleteButton.findViewById<TextView>(R.id.tv_btn_text)
            btnCompleteText?.text = "행운의 액션 확인하기"

            Log.d(TAG, "완료 상태 UI 표시")

        } catch (e: Exception) {
            Log.e(TAG, "완료 상태 UI 표시 실패: ${e.message}")
        }
    }

    /**
     * 뾰롱이가 타이핑 중임을 표시하는 함수 (선택사항)
     */
    private fun showGhostTyping() {
        try {
            // 타이핑 중 메시지 추가 (임시)
            val typingMessage = "뾰롱이가 답변을 생각하고 있어요... 💭"
            addGhostMessage(typingMessage)

        } catch (e: Exception) {
            Log.e(TAG, "타이핑 표시 실패: ${e.message}")
        }
    }

    /**
     * 뾰롱이 타이핑 중 표시를 제거하는 함수 (선택사항)
     */
    private fun hideGhostTyping() {
        try {
            // 마지막 메시지가 타이핑 메시지인지 확인하고 제거
            if (chatMessages.isNotEmpty()) {
                val lastMessage = chatMessages.last()
                if (lastMessage.message.contains("생각하고 있어요")) {
                    // 타이핑 메시지 제거
                    chatMessages.removeAt(chatMessages.size - 1)
                    chatAdapter.notifyItemRemoved(chatMessages.size)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "타이핑 표시 제거 실패: ${e.message}")
        }
    }

    /**
     * RecyclerView를 맨 아래로 스크롤하는 함수
     * 새 메시지가 추가될 때마다 호출되어 최신 메시지를 보여줌
     */
    private fun scrollToBottom() {
        try {
            if (chatMessages.isNotEmpty()) {
                // postDelayed는 일정 시간 후에 코드를 실행하는 함수
                // UI 업데이트가 완료된 후 스크롤하기 위해 약간의 지연을 줌
                rvChatMessages.postDelayed({
                    rvChatMessages.scrollToPosition(chatMessages.size - 1)
                }, SCROLL_DELAY)
            }

        } catch (e: Exception) {
            Log.e(TAG, "스크롤 실패: ${e.message}")
        }
    }

    // ================================
    // 취소 및 완료 처리 함수들
    // ================================

    /**
     * 취소 확인 다이얼로그를 표시하는 함수
     * 사용자가 X 버튼을 눌렀을 때 호출
     */
    private fun showCancelDialog() {
        try {
            // AlertDialog는 안드로이드의 기본 확인 다이얼로그
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("채팅 종료")
                .setMessage("뾰롱이와의 대화를 종료하시겠습니까?\n진행 상황이 저장됩니다.")
                .setPositiveButton("종료") { _, _ ->
                    // 긍정 버튼(종료) 클릭 시 실행
                    Log.d(TAG, "사용자가 채팅 종료 선택")
                    finishChatActivity()
                }
                .setNegativeButton("계속") { dialog, _ ->
                    // 부정 버튼(계속) 클릭 시 실행
                    Log.d(TAG, "사용자가 채팅 계속 선택")
                    dialog.dismiss() // 다이얼로그 닫기
                }
                .setCancelable(false) // 다이얼로그 외부 터치로 닫기 방지
                .show() // 다이얼로그 표시

        } catch (e: Exception) {
            Log.e(TAG, "취소 다이얼로그 표시 실패: ${e.message}")
            finishChatActivity()
        }
    }

    /**
     * 채팅 액티비티를 종료하는 함수
     */
    private fun finishChatActivity() {
        try {
            Log.d(TAG, "ChatActivity 종료 시작")

            // 세션 상태 저장
            saveChatSession()

            // 액티비티 종료
            finish()

        } catch (e: Exception) {
            Log.e(TAG, "ChatActivity 종료 실패: ${e.message}")
            finish() // 오류가 발생해도 강제 종료
        }
    }

    /**
     * 미션 화면으로 이동하는 함수
     * 채팅이 완료된 후 호출됨
     */
    private fun proceedToRecommendActionActivity() {
        try {
            Log.d(TAG, "미션 추천 화면으로 이동 시작")

            // 채팅이 완료되지 않았다면 이동 불가
            if (!isChatCompleted) {
                Log.w(TAG, "채팅 미완료 - 미션 추천 이동 불가")
                showMessage("뾰롱이와의 대화를 먼저 완료해주세요!")
                return
            }

            // RecommendActionActivity로 이동 (수정됨)
            val intent = Intent(this, RecommendActionActivity::class.java).apply {
                // 사용자 정보 전달
                putExtra("userName", userName)
                putExtra("userBirthDate", userBirthDate)

                // 사주 정보 전달
                putExtra("sajuId", sajuId)
                putExtra("sajuContent", sajuContent)
                putExtra("category", selectedCategory.name)
                putExtra("categoryDisplayName", categoryDisplayName)

                // 채팅 완료 정보 전달
                putExtra("chatCompleted", true)
                putExtra("chatSessionId", currentSession.id)

                // 채팅 메시지들 전달 (감정 분석용)
                val userMessages = chatMessages
                    .filter { it.isFromUser } // 사용자가 보낸 메시지만 필터링
                    .map { it.message }       // 메시지 내용만 추출
                    .toTypedArray()           // 배열로 변환
                putExtra("userMessages", userMessages)

                // 채팅에서 온 플래그
                putExtra("fromChat", true)
            }

            startActivity(intent)

            // 현재 액티비티 종료 (뒤로가기 방지)
            finish()

            Log.d(TAG, "RecommendActionActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "미션 추천 화면 이동 실패: ${e.message}")
            showErrorMessage("미션 추천 화면으로 이동하는 중 오류가 발생했습니다.")
        }
    }

    // ================================
    // 유틸리티 함수들
    // ================================

    /**
     * 사용자에게 간단한 메시지를 표시하는 함수
     * 현재는 로그로만 출력하지만, 추후 Toast나 Snackbar로 확장 가능
     */
    private fun showMessage(message: String) {
        try {
            Log.i(TAG, "사용자 메시지: $message")

            // TODO: 실제 앱에서는 Toast나 Snackbar 사용
            // Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "메시지 표시 실패: ${e.message}")
        }
    }

    /**
     * 에러 메시지를 표시하고 액티비티를 종료하는 함수
     */
    private fun showErrorAndFinish(message: String) {
        try {
            Log.e(TAG, "심각한 오류: $message")

            // TODO: 에러 다이얼로그 표시
            showMessage(message)

            // 3초 후 액티비티 종료
            rvChatMessages.postDelayed({
                finish()
            }, 3000)

        } catch (e: Exception) {
            Log.e(TAG, "에러 처리 실패: ${e.message}")
            finish()
        }
    }

    /**
     * 일반적인 에러 메시지를 표시하는 함수
     */
    private fun showErrorMessage(message: String) {
        try {
            Log.w(TAG, "에러 메시지: $message")
            showMessage(message)

        } catch (e: Exception) {
            Log.e(TAG, "에러 메시지 표시 실패: ${e.message}")
        }
    }

    /**
     * 현재 채팅 상태를 확인하는 함수 (디버깅용)
     */
    private fun debugChatState() {
        try {
            Log.d(TAG, "=== ChatActivity 상태 ===")
            Log.d(TAG, "사용자: $userName")
            Log.d(TAG, "세션 ID: ${currentSession.id}")
            Log.d(TAG, "대화 횟수: $conversationCount / $MAX_CONVERSATIONS")
            Log.d(TAG, "완료 여부: $isChatCompleted")
            Log.d(TAG, "메시지 개수: ${chatMessages.size}")
            Log.d(TAG, "==========================")

        } catch (e: Exception) {
            Log.e(TAG, "상태 디버깅 실패: ${e.message}")
        }
    }

    // ================================
    // Room DB 관련 함수들 (추후 구현)
    // ================================

    /**
     * 저장된 채팅 메시지들을 불러오는 함수
     * Room DB 구현 후 사용 예정
     */
    private fun loadSavedMessages() {
        try {
            // TODO: Room DB에서 오늘의 채팅 메시지들 조회
            /*
            lifecycleScope.launch {
                val savedMessages = chatMessageDao.getMessagesBySessionId(currentSession.id)
                chatMessages.clear()
                chatMessages.addAll(savedMessages)
                chatAdapter.notifyDataSetChanged()
                scrollToBottom()
            }
            */

            Log.d(TAG, "저장된 메시지 로드 완료 (미구현)")

        } catch (e: Exception) {
            Log.e(TAG, "저장된 메시지 로드 실패: ${e.message}")
        }
    }

    /**
     * 채팅 메시지를 DB에 저장하는 함수
     * Room DB 구현 후 사용 예정
     */
    private fun saveMessageToDB(message: ChatMessage) {
        try {
            // TODO: Room DB에 메시지 저장
            /*
            lifecycleScope.launch {
                chatMessageDao.insertMessage(message)
            }
            */

            Log.d(TAG, "메시지 DB 저장 완료 (미구현)")

        } catch (e: Exception) {
            Log.e(TAG, "메시지 DB 저장 실패: ${e.message}")
        }
    }

    /**
     * 채팅 완료 후 UI 업데이트
     */
    private fun showChatCompleteUI() {
        try {
            Log.d(TAG, "채팅 완료 UI 표시")

            // 입력창 및 전송 버튼 숨기기
            etChatInput.visibility = View.GONE
            ivSendButton.visibility = View.GONE

            // 완료 메시지 표시
            layoutCompleteButton.visibility = View.VISIBLE
            val btnText = layoutCompleteButton.findViewById<TextView>(R.id.tv_btn_text)
            btnText?.text = "미션 받기"

            Log.d(TAG, "채팅 완료 UI 설정 완료")

        } catch (e: Exception) {
            Log.e(TAG, "채팅 완료 UI 설정 실패: ${e.message}")
        }
    }
}

/**
 * ChatAdapter 클래스 (별도 파일로 구현 필요)
 * RecyclerView에서 채팅 메시지들을 표시하는 어댑터
 *
 * 주요 기능:
 * 1. 사용자 메시지와 뾰롱이 메시지를 다른 레이아웃으로 표시
 * 2. 메시지 시간 표시
 * 3. 스크롤 최적화
 */
class ChatAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 뷰 타입 상수 정의
    companion object {
        private const val VIEW_TYPE_USER = 1      // 사용자 메시지
        private const val VIEW_TYPE_GHOST = 2     // 뾰롱이 메시지
    }

    /**
     * 메시지 타입에 따라 다른 뷰 타입 반환
     */
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isFromUser) {
            VIEW_TYPE_USER
        } else {
            VIEW_TYPE_GHOST
        }
    }

    /**
     * 뷰 홀더 생성
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_USER -> {
                // 사용자 메시지 레이아웃 (item_chat_user.xml)
                val view = inflater.inflate(R.layout.item_chat_user, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_GHOST -> {
                // 뾰롱이 메시지 레이아웃 (item_chat_ghost.xml)
                val view = inflater.inflate(R.layout.item_chat_ghost, parent, false)
                GhostMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("알 수 없는 뷰 타입: $viewType")
        }
    }

    /**
     * 뷰 홀더에 데이터 바인딩
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is GhostMessageViewHolder -> holder.bind(message)
        }
    }

    /**
     * 전체 아이템 개수 반환
     */
    override fun getItemCount(): Int = messages.size

    /**
     * 사용자 메시지 뷰 홀더
     */
    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserChat: TextView = itemView.findViewById(R.id.tv_user_chat)

        fun bind(message: ChatMessage) {
            tvUserChat.text = message.message
        }
    }

    /**
     * 뾰롱이 메시지 뷰 홀더
     */
    class GhostMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvGhostChat: TextView = itemView.findViewById(R.id.tv_ghost_chat)

        fun bind(message: ChatMessage) {
            tvGhostChat.text = message.message
        }
    }
}