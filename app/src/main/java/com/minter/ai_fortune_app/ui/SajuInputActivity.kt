package com.minter.ai_fortune_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import com.minter.ai_fortune_app.R
import com.minter.ai_fortune_app.data.model.SajuCategory
import com.minter.ai_fortune_app.utils.ValidationUtils

/**
 * 사용자 정보 입력 액티비티
 * - 이름과 생년월일 입력 받기
 * - 실시간 입력 검증
 * - LoadingActivity로 데이터 전달
 */
class SajuInputActivity : AppCompatActivity() {

    // ================================
    // 상수 및 태그 정의
    // ================================

    companion object {
        private const val TAG = "SajuInputActivity"
    }

    // ================================
    // UI 요소 정의
    // ================================

    private lateinit var etName: EditText          // 이름 입력 필드
    private lateinit var etNameHint: EditText      // 이름 입력 힌트
    private lateinit var etBirthDate: EditText     // 생년월일 입력 필드
    private lateinit var etBirthDateHint: EditText // 생년월일 입력 힌트
    private lateinit var btnGenerate: View         // 생성 버튼
    private lateinit var btnText: TextView         // 버튼 텍스트

    // ================================
    // 전달받은 데이터
    // ================================

    private var selectedCategory: SajuCategory = SajuCategory.DAILY  // 선택된 카테고리
    private var categoryDisplayName: String = ""                     // 카테고리 표시명

    // ================================
    // 액티비티 생명주기
    // ================================

    /**
     * 액티비티가 생성될 때 호출되는 함수
     * UI 초기화 및 이벤트 설정
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "=== SajuInputActivity onCreate 시작 ===")
        
        setContentView(R.layout.activity_saju_input)
        Log.d(TAG, "레이아웃 설정 완료")

        // 1. 전달받은 데이터 처리
        handleIntentData()
        Log.d(TAG, "데이터 처리 완료")

        // 2. UI 요소 초기화
        initViews()
        Log.d(TAG, "UI 초기화 완료")

        // 3. 이벤트 리스너 설정
        setupListeners()
        Log.d(TAG, "리스너 설정 완료")

        // 4. UI 상태 업데이트
        updateUI()
        Log.d(TAG, "=== SajuInputActivity onCreate 완료 ===")
    }

    /**
     * 액티비티 종료 시 정리
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SajuInputActivity 종료")
    }

    // ================================
    // 데이터 처리 함수들
    // ================================

    /**
     * Intent로 전달받은 데이터를 처리하는 함수
     * MainActivity에서 보낸 카테고리 정보를 받아옴
     */
    private fun handleIntentData() {
        try {
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

            Log.d(TAG, "데이터 처리 완료 - 카테고리: ${selectedCategory.displayName}")

        } catch (e: Exception) {
            Log.e(TAG, "Intent 데이터 처리 실패: ${e.message}")

            // 기본값으로 설정
            selectedCategory = SajuCategory.DAILY
            categoryDisplayName = "오늘의 사주"
        }
    }

    /**
     * XML 레이아웃의 UI 요소들을 찾아서 변수에 연결하는 함수
     */
    private fun initViews() {
        try {
            Log.d(TAG, "initViews 시작")
            
            // 컨테이너들 먼저 찾기
            val nameContainer = findViewById<View>(R.id.et_name)
            val birthdateContainer = findViewById<View>(R.id.et_birthdate) 
            val btnContainer = findViewById<View>(R.id.btn_generate)
            
            Log.d(TAG, "컨테이너 찾기 - 이름: ${nameContainer != null}, 생년월일: ${birthdateContainer != null}, 버튼: ${btnContainer != null}")
            
            // 각 컨테이너에서 내부 요소들 찾기
            etNameHint = nameContainer.findViewById<EditText>(R.id.et_input_field)
            etBirthDateHint = birthdateContainer.findViewById<EditText>(R.id.et_input_field)
            btnText = findViewById<TextView>(R.id.tv_btn_text)
            
            Log.d(TAG, "내부 요소 찾기 - 이름입력: ${etNameHint != null}, 생년월일입력: ${etBirthDateHint != null}, 버튼텍스트: ${btnText != null}")

            // lateinit var 설정 (안전한 방식)
            etName = etNameHint  // 실제 EditText 할당
            etBirthDate = etBirthDateHint  // 실제 EditText 할당
            btnGenerate = btnContainer
            
            // 초기 텍스트 설정
            if (btnText != null) {
                btnText.text = "결과보기"
                Log.d(TAG, "버튼 텍스트 설정: '결과보기'")
            }
            
            if (etNameHint != null) {
                etNameHint.hint = "이름을 입력하세요"
                Log.d(TAG, "이름 힌트 설정 완료")
            }
            
            if (etBirthDateHint != null) {
                etBirthDateHint.hint = "생년월일을 입력하세요 (YYYY-MM-DD)"
                Log.d(TAG, "생년월일 힌트 설정 완료")
            }

            Log.d(TAG, "UI 요소 초기화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "UI 요소 초기화 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 이벤트 리스너를 설정하는 함수
     * 버튼 클릭 및 텍스트 변경 이벤트 처리
     */
    private fun setupListeners() {
        try {
            Log.d(TAG, "이벤트 리스너 설정 시작")

            // 사주 생성 버튼 클릭 이벤트
            btnGenerate.setOnClickListener {
                Log.d(TAG, "🔥🔥🔥 사주 생성 버튼 클릭됨!!! 🔥🔥🔥")
                try {
                    onGenerateButtonClick()
                } catch (e: Exception) {
                    Log.e(TAG, "버튼 클릭 처리 중 오류: ${e.message}")
                    e.printStackTrace()
                }
            }

            // 이름 입력 실시간 검증
            etNameHint.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    Log.d(TAG, "이름 텍스트 변경됨: '${s.toString()}'")
                    validateInputs()
                }
            })

            // 생년월일 입력 실시간 검증 및 포맷팅
            etBirthDateHint.addTextChangedListener(object : android.text.TextWatcher {
                private var isFormatting = false // 무한 루프 방지 플래그
                
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (isFormatting) return // 포맷팅 중이면 무시
                    
                    val original = s.toString()
                    Log.d(TAG, "생년월일 텍스트 변경됨: '$original'")
                    
                    // 6자리 또는 8자리 숫자가 입력되면 자동 포맷팅
                    val cleanInput = original.replace(Regex("[^0-9]"), "")
                    if (cleanInput.length == 6 || cleanInput.length == 8) {
                        val formatted = ValidationUtils.formatBirthDate(original)
                        if (formatted != original) {
                            isFormatting = true
                            try {
                                s?.clear()
                                s?.append(formatted)
                                Log.d(TAG, "생년월일 자동 포맷팅: '$original' → '$formatted'")
                            } finally {
                                isFormatting = false
                            }
                        }
                    }
                    
                    validateInputs()
                }
            })

            // 이름 입력 필드 엔터 키 처리
            etNameHint.setOnEditorActionListener { _, actionId, event ->
                Log.d(TAG, "이름 필드 엔터 키 입력 - actionId: $actionId")
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_NEXT -> {
                        hideKeyboardAndClearFocus(etNameHint)
                        // 생년월일 필드로 포커스 이동
                        etBirthDateHint.requestFocus()
                        true
                    }
                    else -> {
                        if (event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                            hideKeyboardAndClearFocus(etNameHint)
                            etBirthDateHint.requestFocus()
                            true
                        } else false
                    }
                }
            }

            // 생년월일 입력 필드 엔터 키 처리
            etBirthDateHint.setOnEditorActionListener { _, actionId, event ->
                Log.d(TAG, "생년월일 필드 엔터 키 입력 - actionId: $actionId")
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        hideKeyboardAndClearFocus(etBirthDateHint)
                        // 입력이 완료되었으면 사주 생성 시도
                        if (btnGenerate.isEnabled) {
                            onGenerateButtonClick()
                        }
                        true
                    }
                    else -> {
                        if (event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                            hideKeyboardAndClearFocus(etBirthDateHint)
                            if (btnGenerate.isEnabled) {
                                onGenerateButtonClick()
                            }
                            true
                        } else false
                    }
                }
            }

            Log.d(TAG, "이벤트 리스너 설정 완료")

        } catch (e: Exception) {
            Log.e(TAG, "리스너 설정 실패: ${e.message}")
        }
    }

    /**
     * UI 상태를 업데이트하는 함수
     * 초기 화면 설정
     */
    private fun updateUI() {
        try {
            Log.d(TAG, "UI 업데이트 시작")

            // 초기에는 버튼 비활성화
            updateGenerateButton(false)

            // 화면 제목 업데이트
            title = "${categoryDisplayName} 정보 입력"

            Log.d(TAG, "UI 업데이트 완료")

        } catch (e: Exception) {
            Log.e(TAG, "UI 업데이트 실패: ${e.message}")
        }
    }

    // ================================
    // 입력 검증 관련 함수들
    // ================================

    /**
     * 입력값을 실시간으로 검증하는 함수
     * 이름과 생년월일의 유효성을 확인
     */
    private fun validateInputs() {
        try {
            // 입력된 텍스트 가져오기
            val name = etNameHint.text.toString().trim()
            val birthDate = etBirthDateHint.text.toString().trim()

            // 각각 검증
            val isNameValid = ValidationUtils.validateName(name)
            val isBirthDateValid = ValidationUtils.validateBirthDate(birthDate)

            // 시각적 피드백
            updateFieldAppearance(etNameHint, isNameValid, name.isNotEmpty())
            updateFieldAppearance(etBirthDateHint, isBirthDateValid, birthDate.isNotEmpty())

            // 버튼 활성화 여부 결정
            val allValid = isNameValid && isBirthDateValid
            updateGenerateButton(allValid)

            Log.d(TAG, "입력 검증 - 이름: '$name' ($isNameValid), 생년월일: '$birthDate' ($isBirthDateValid), 전체: $allValid")

        } catch (e: Exception) {
            Log.e(TAG, "입력 검증 실패: ${e.message}")
        }
    }

    /**
     * 입력 필드의 외관을 업데이트하는 함수
     * 검증 결과에 따른 시각적 피드백
     */
    private fun updateFieldAppearance(editText: EditText, isValid: Boolean, hasText: Boolean) {
        try {
            when {
                !hasText -> {
                    // 빈 칸이면 기본 색상
                    editText.setTextColor(getColor(android.R.color.white))
                }
                isValid -> {
                    // 유효하면 흰색 (정상)
                    editText.setTextColor(getColor(android.R.color.white))
                }
                else -> {
                    // 무효하면 빨간색 (경고)
                    editText.setTextColor(getColor(android.R.color.holo_red_light))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "필드 외관 업데이트 실패: ${e.message}")
        }
    }

    /**
     * 생성 버튼 활성화/비활성화
     */
    private fun updateGenerateButton(enabled: Boolean) {
        try {
            btnGenerate.isEnabled = enabled

            // 버튼 텍스트 업데이트
            btnText.text = if (enabled) {
                "사주 생성하기"
            } else {
                "정보를 입력해주세요"
            }

            // 버튼 투명도 변경 (시각적 피드백)
            btnGenerate.alpha = if (enabled) 1.0f else 0.5f

        } catch (e: Exception) {
            Log.e(TAG, "버튼 업데이트 실패: ${e.message}")
        }
    }

    /**
     * 사주 생성 버튼 클릭 처리
     */
    private fun onGenerateButtonClick() {
        try {
            Log.d(TAG, "🚀 onGenerateButtonClick 시작")
            
            val name = etNameHint.text.toString().trim()
            val birthDate = etBirthDateHint.text.toString().trim()

            Log.d(TAG, "📝 입력값 확인 - 이름: '$name', 생년월일: '$birthDate'")

            // 최종 검증
            val isNameValid = ValidationUtils.validateName(name)
            val isBirthDateValid = ValidationUtils.validateBirthDate(birthDate)
            
            Log.d(TAG, "✅ 검증 결과 - 이름: $isNameValid, 생년월일: $isBirthDateValid")
            
            if (!isNameValid) {
                Log.w(TAG, "❌ 이름 검증 실패")
                showError("올바른 이름을 입력해주세요 (2-20자)")
                return
            }

            if (!isBirthDateValid) {
                Log.w(TAG, "❌ 생년월일 검증 실패")
                showError("올바른 생년월일을 입력해주세요 (YYYY-MM-DD)")
                return
            }

            Log.d(TAG, "🎯 검증 통과! LoadingActivity로 이동 시작")
            // LoadingActivity로 이동
            generateSaju(name, birthDate)

        } catch (e: Exception) {
            Log.e(TAG, "💥 사주 생성 처리 실패: ${e.message}")
            e.printStackTrace()
            showError("사주 생성 중 오류가 발생했습니다.")
        }
    }

    /**
     * 에러 메시지 표시
     */
    private fun showError(message: String) {
        try {
            Log.w(TAG, "에러 메시지: $message")

            // 임시로 버튼 텍스트에 에러 메시지 표시
            val originalText = btnText.text.toString()
            btnText.text = message

            // 3초 후 원래 텍스트로 복원
            btnText.postDelayed({
                btnText.text = originalText
                validateInputs() // 다시 검증해서 올바른 상태로 복원
            }, 3000)

        } catch (e: Exception) {
            Log.e(TAG, "에러 메시지 표시 실패: ${e.message}")
        }
    }

    /**
     * 사주 생성 및 LoadingActivity로 이동 (추가됨)
     * 사용자 입력 데이터를 LoadingActivity에 전달
     */
    private fun generateSaju(name: String, birthDate: String) {
        try {
            Log.d(TAG, "LoadingActivity로 이동 시작 - 카테고리: ${selectedCategory.displayName}")

            // 버튼 비활성화 (중복 클릭 방지)
            btnGenerate.isEnabled = false
            btnText.text = "사주 생성 중..."

            // LoadingActivity로 이동
            val intent = Intent(this, LoadingActivity::class.java).apply {
                // 사용자 정보 전달
                putExtra("userName", name)
                putExtra("userBirthDate", birthDate)

                // 카테고리 정보 전달
                putExtra("category", selectedCategory.name)
                putExtra("categoryDisplayName", categoryDisplayName)
            }

            startActivity(intent)

            // 현재 화면 종료 (뒤로가기 방지)
            finish()

            Log.d(TAG, "LoadingActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "LoadingActivity 이동 실패: ${e.message}")

            // 버튼 복구
            btnGenerate.isEnabled = true
            btnText.text = "결과 보기"

            showError("화면 이동 중 오류가 발생했습니다. 다시 시도해주세요.")
        }
    }

    // ================================
    // 유틸리티 함수들
    // ================================

    /**
     * 키보드를 숨기고 EditText의 포커스를 해제하는 함수
     */
    private fun hideKeyboardAndClearFocus(editText: EditText) {
        try {
            Log.d(TAG, "키보드 숨김 및 포커스 해제")
            
            // InputMethodManager를 사용해서 키보드 숨기기
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(editText.windowToken, 0)
            
            // 포커스 해제
            editText.clearFocus()
            
            Log.d(TAG, "키보드 숨김 완료")
            
        } catch (e: Exception) {
            Log.e(TAG, "키보드 숨김 실패: ${e.message}")
        }
    }
}