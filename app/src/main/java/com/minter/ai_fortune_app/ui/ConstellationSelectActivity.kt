package com.minter.ai_fortune_app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.minter.ai_fortune_app.R
import com.minter.ai_fortune_app.data.model.EmotionType
import com.minter.ai_fortune_app.data.model.SajuCategory
import com.minter.ai_fortune_app.utils.SharedPreferencesUtils

class ConstellationSelectActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ConstellationSelectActivity"
        private const val ANIMATION_DELAY_MS = 3000L
        private const val AUTO_PROCEED_DELAY_MS = 5000L
    }

    private lateinit var lottieAnimation: LottieAnimationView
    private lateinit var layoutContent: LinearLayout
    private lateinit var tvEmotionMessage: TextView
    private lateinit var ivStarEmotion: ImageView
    private lateinit var btnNext: View

    private var userName: String = ""
    private var analyzedEmotion: EmotionType? = null
    private var emotionDisplayName: String = ""

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_constellation_select)

        handleIntentData()
        initViews()
        setupButtonListeners()
        startCollectionSequence()
    }

    private fun handleIntentData() {
        userName = intent.getStringExtra("userName") ?: "사용자"
        emotionDisplayName = intent.getStringExtra("emotionDisplayName") ?: "Happy"

        val emotionName = intent.getStringExtra("analyzedEmotion") ?: "HAPPY"

        // 디버그 로그 추가
        Log.d(TAG, "🔥 받아온 감정 데이터:")
        Log.d(TAG, "🔥 emotionName (원본): '$emotionName'")
        Log.d(TAG, "🔥 emotionDisplayName: '$emotionDisplayName'")

        val emotionNameUpper = emotionName.uppercase()
        Log.d(TAG, "🔥 emotionNameUpper: '$emotionNameUpper'")

        analyzedEmotion = try {
            val emotion = EmotionType.valueOf(emotionNameUpper)
            Log.d(TAG, "✅ 감정 파싱 성공: $emotion")
            emotion
        } catch (e: Exception) {
            Log.e(TAG, "❌ 감정 파싱 실패: ${e.message}")
            Log.e(TAG, "❌ 기본값 HAPPY 사용")
            EmotionType.HAPPY
        }

        Log.d(TAG, "🔥 최종 analyzedEmotion: $analyzedEmotion")
    }

    private fun initViews() {
        lottieAnimation = findViewById(R.id.lottie_loading)
        layoutContent = findViewById(R.id.layout_content)
        tvEmotionMessage = findViewById(R.id.tv_emotion_message)
        ivStarEmotion = findViewById(R.id.iv_star_emotion)
        btnNext = findViewById(R.id.btn_next)

        layoutContent.visibility = View.INVISIBLE
    }

    private fun setupButtonListeners() {
        btnNext.setOnClickListener {
            proceedToConstellationActivity()
        }
    }

    private fun startCollectionSequence() {
        handler.postDelayed({
            showCollectionResult()
        }, ANIMATION_DELAY_MS)
    }

    private fun showCollectionResult() {
        updateEmotionMessage()
        updateStarImage()
        updateButtonText()

        // 별자리 수집 저장 (감정 정보 포함)
        if (!SharedPreferencesUtils.hasTodayConstellation(this)) {
            SharedPreferencesUtils.saveTodayConstellation(this, analyzedEmotion?.name)
            Log.d(TAG, "✅ 별자리 수집 완료 - 저장됨, 감정: ${analyzedEmotion?.name}")
        } else {
            Log.d(TAG, "이미 수집된 별자리 - 저장 건너뛰기")
        }

        layoutContent.alpha = 0f
        layoutContent.visibility = View.VISIBLE
        layoutContent.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()
    }

    private fun updateEmotionMessage() {
        val message = when (analyzedEmotion) {
            EmotionType.HAPPY -> "축하합니다!\n행복한 감정의\n별을 수집했어요! ✨"
            EmotionType.ANGRY -> "당신의 분노를\n별로 승화시켰어요!\n화가 나는 날도 괜찮아요 🔥"
            EmotionType.SAD -> "슬픈 마음도\n소중한 감정이에요\n별이 되어 빛나고 있어요 💙"
            EmotionType.TIMID -> "조심스러운 마음도\n아름다운 별이 되었어요\n당신은 충분히 용감해요 💜"
            EmotionType.GRUMPY -> "짜증나는 하루였지만\n별로 바뀌었어요!\n내일은 더 나을 거예요 💚"
            else -> "새로운 별을\n수집했습니다! 🌟"
        }
        tvEmotionMessage.text = message

        Log.d(TAG, "🔥 감정 메시지 설정: $message")
    }

    private fun updateStarImage() {
        Log.d(TAG, "🔥 updateStarImage 시작 - 현재 감정: $analyzedEmotion")

        val imageRes = when (analyzedEmotion) {
            EmotionType.HAPPY -> {
                Log.d(TAG, "✨ HAPPY 별 이미지 설정")
                R.drawable.iv_star_happy
            }
            EmotionType.ANGRY -> {
                Log.d(TAG, "🔥 ANGRY 별 이미지 설정")
                R.drawable.iv_star_angry
            }
            EmotionType.SAD -> {
                Log.d(TAG, "💙 SAD 별 이미지 설정")
                R.drawable.iv_star_sad
            }
            EmotionType.TIMID -> {
                Log.d(TAG, "💜 TIMID 별 이미지 설정")
                R.drawable.iv_star_timid
            }
            EmotionType.GRUMPY -> {
                Log.d(TAG, "💚 GRUMPY 별 이미지 설정")
                R.drawable.iv_star_grumpy
            }
            else -> {
                Log.d(TAG, "⭐ 기본(HAPPY) 별 이미지 설정")
                R.drawable.iv_star_happy
            }
        }

        ivStarEmotion.setImageResource(imageRes)
        Log.d(TAG, "✅ 별 이미지 설정 완료: $imageRes")
    }

    private fun updateButtonText() {
        val btnText = btnNext.findViewById<TextView>(R.id.tv_btn_text)
        btnText?.text = "별자리 보기"
    }

    private fun proceedToConstellationActivity() {
        // ConstellationActivity 대신 ConstellationPersonalActivity로 변경
        val intent = Intent(this, ConstellationPersonalActivity::class.java).apply {
            // 필요한 데이터 전달
            putExtra("userName", userName)
            putExtra("analyzedEmotion", analyzedEmotion?.name)
            putExtra("emotionDisplayName", emotionDisplayName)

            // 별자리 수집 완료 플래그 추가
            putExtra("fromSelectActivity", true)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        if (layoutContent.visibility == View.INVISIBLE) {
            return
        }
        proceedToConstellationActivity()
    }
}