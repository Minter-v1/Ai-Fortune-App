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

            // 북두칠성 7개 별 찾기
            for (i in 1..7) {
                val starId = resources.getIdentifier("star_$i", "id", packageName)
                val starView = findViewById<ImageView>(starId)
                starViews.add(starView)
            }

            Log.d(TAG, "UI 초기화 완료 - 별 개수: ${starViews.size}")

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

            // 각 별 클릭 이벤트 (별 세부 정보 표시용 - 선택사항)
            starViews.forEachIndexed { index, starView ->
                starView.setOnClickListener {
                    Log.d(TAG, "별 ${index + 1} 클릭")
                    showStarInfo(index + 1)
                }
            }

            Log.d(TAG, "버튼 이벤트 설정 완료")

        } catch (e: Exception) {
            Log.e(TAG, "버튼 이벤트 설정 실패: ${e.message}")
        }
    }

    /**
     * 별자리 맵 설정
     * 감정에 따라 별들의 색상과 상태를 변경
     */
    private fun setupConstellationMap() {
        try {
            Log.d(TAG, "별자리 맵 설정 시작")

            // 감정에 따른 별 이미지 리소스 결정
            val starImageRes = getStarImageResource(analyzedEmotion)

            // 현재 감정에 해당하는 별을 활성화
            activateEmotionStar(starImageRes)

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
     * 현재 감정에 해당하는 별을 활성화
     */
    private fun activateEmotionStar(starImageRes: Int) {
        try {
            // TODO: 실제로는 감정별로 특정 별 위치를 지정해야 함
            // 현재는 첫 번째 별을 활성화
            if (starViews.isNotEmpty()) {
                val targetStar = starViews[0] // 첫 번째 별 활성화
                targetStar.setImageResource(starImageRes)
                targetStar.tag = analyzedEmotion?.name ?: "HAPPY"
                
                // 애니메이션 효과 (선택사항)
                targetStar.alpha = 0f
                targetStar.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .start()
            }

            Log.d(TAG, "감정 별 활성화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "감정 별 활성화 실패: ${e.message}")
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
     * 별 정보 표시 (별 클릭 시)
     */
    private fun showStarInfo(starNumber: Int) {
        try {
            val starName = getStarName(starNumber)
            val message = "${starName}을 선택하셨습니다!"
            
            Log.d(TAG, "별 정보: $message")
            // TODO: 실제로는 다이얼로그나 Toast로 표시

        } catch (e: Exception) {
            Log.e(TAG, "별 정보 표시 실패: ${e.message}")
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
     * 뒤로가기 처리
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Log.d(TAG, "뒤로가기 클릭")
        proceedToNext() // 뒤로가기도 다음으로 이동
        // super.onBackPressed() // 의도적으로 기본 동작을 막음
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ConstellationPersonalActivity 종료")
    }
}