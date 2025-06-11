package com.minter.ai_fortune_app.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.minter.ai_fortune_app.R
import android.content.Intent
import android.util.Log
import com.minter.ai_fortune_app.data.model.SajuCategory
import com.minter.ai_fortune_app.utils.SharedPreferencesUtils


//import com.minter.ai_fortune_app.ui.saju.SajuInputActivity
//import com.minter.ai_fortune_app.ui.mission.AcceptMissionActivity
//import com.minter.ai_fortune_app.utils.SharedPreferencesUtils

class MainActivity : AppCompatActivity() {

    /**
     * MARK: 코드에 태깅하기
     * */
    companion object {
        private const val TAG = "MainActivity"
    }


    private lateinit var dailySajuBtn: View
    private lateinit var loveSajuBtn: View
    private lateinit var careerSajuBtn: View
    private lateinit var studySajuBtn: View
    private lateinit var healthSajuBtn: View

    //MARK: 앱이 시작될 때 실행되는 함수
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "🔥🔥🔥 MainActivity onCreate 시작!!! 🔥🔥🔥")

        setContentView(R.layout.activity_main) //XML 연결
        Log.d(TAG, "📱 레이아웃 설정 완료")

        initViews() //화면 요소 찾기
        Log.d(TAG, "🎯 initViews 완료")
        
        setupButtonListeners() //버튼 이벤트 리스터
        Log.d(TAG, "🖱️ setupButtonListeners 완료")
        
        updateUIForCurrentState() //화면 전환 함수
        Log.d(TAG, "✅ MainActivity onCreate 모든 단계 완료!")
    }





    /**MARK:
     * 액티비티가 다시 활성화될 때 호출
     * 다른 화면에서 이 화면으로 돌아올 때 실행됨
     */
    override fun onResume() {
        super.onResume()
        updateUIForCurrentState()
    }


    /**
     * MARK:
     * 화면 요소들 초기화
     * XML에서 뷰들을 찾아서 변수에 연결
     */
    private fun initViews() {
        try{
            dailySajuBtn = findViewById(R.id.daily_saju_btn)
            loveSajuBtn = findViewById(R.id.love_saju_btn)
            careerSajuBtn = findViewById(R.id.career_saju_btn)
            studySajuBtn = findViewById(R.id.study_saju_btn)
            healthSajuBtn = findViewById(R.id.health_saju_btn)

            setCategoryButtonTexts() //버튼 글자 변경 실행
            Log.d(TAG,"UI 초기화 성공")
        } catch (e: Exception) {
            Log.e(TAG,"UI 초기화 실패")
        }

    }

    //MARK: - 버튼 안의 글자 변경
    private fun setCategoryButtonTexts() {
        try{
            loveSajuBtn.findViewById<TextView>(R.id.purple_btn).text = getString(R.string.love)
            careerSajuBtn.findViewById<TextView>(R.id.purple_btn).text = getString(R.string.career)
            studySajuBtn.findViewById<TextView>(R.id.purple_btn).text = getString(R.string.study)
            healthSajuBtn.findViewById<TextView>(R.id.purple_btn).text = getString(R.string.health)
            Log.d(TAG,"버튼 텍스트 변경 성공")
        } catch(e: Exception) {
            Log.e(TAG,"버튼 텍스트 변경 실패: ${e.message}")
        }

    }

    //MARK: - 버튼 클릭 이벤트 리스너
    private fun setupButtonListeners() {
        try {
            //도메인 지정
            dailySajuBtn.setOnClickListener { 
                Log.d(TAG, "Daily 사주 버튼 클릭됨")
                onCategorySelected(SajuCategory.DAILY) 
            }
            loveSajuBtn.setOnClickListener { 
                Log.d(TAG, "연애 사주 버튼 클릭됨")
                onCategorySelected(SajuCategory.LOVE) 
            }
            careerSajuBtn.setOnClickListener { 
                Log.d(TAG, "직업 사주 버튼 클릭됨")
                onCategorySelected(SajuCategory.CAREER) 
            }
            studySajuBtn.setOnClickListener { 
                Log.d(TAG, "학업 사주 버튼 클릭됨")
                onCategorySelected(SajuCategory.STUDY) 
            }
            healthSajuBtn.setOnClickListener { 
                Log.d(TAG, "건강 사주 버튼 클릭됨")
                onCategorySelected(SajuCategory.HEALTH) 
            }
            Log.d(TAG, "모든 버튼 리스너 설정 완료")
        } catch (e: Exception) {
            Log.e(TAG, "버튼 리스너 설정 실패: ${e.message}")
        }
    }


    /**MARK:
     * 카테고리 선택 처리
     * 사용자가 어떤 사주 카테고리를 선택했을 때 실행
     */
    private fun onCategorySelected(category: SajuCategory) {
        try {
            Log.d(TAG, "카테고리 선택됨: ${category.displayName}")
            
            if (SharedPreferencesUtils.hasTodaySaju(this)) {
                Log.d(TAG, "오늘 이미 사주 생성됨 - 미션 조회 화면으로 이동")
                showCompletedState()
            } else {
                Log.d(TAG, "사주 생성 가능(${category.displayName}) : 사용자 정보 입력 화면으로 이동")
                startSajuInput(category)
            }
        } catch (e: Exception) {
            Log.e(TAG, "카테고리 선택 처리 실패: ${e.message}")
            showErrorMessage("카테고리 선택 중 오류가 발생했습니다.")
        }
    }

    //MARK: - 정보 입력 화면으로 이동
    private fun startSajuInput(category: SajuCategory) {

        try {
            // Intent: 다른 액티비티로 이동하기 위한 객체
            //이동할 액티비티 지정
            val intent = Intent(this, SajuInputActivity::class.java)
            //카테고리 정보 보냄
            intent.putExtra("category", category.name) //ENUM에서 객체 이름
            intent.putExtra("categoryDisplayName",category.displayName) //버튼에 박혀있는 dp이름

            startActivity(intent)
            Log.d(TAG,"SajuInputActivity로 이동 ${category.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "SajuInputActivity 이동 실패: ${e.message}")
            showErrorMessage("화면 이동 중 오류가 발생했습니다.")
        }

    }
    /**MARk:
     * 완료 상태 화면으로 이동
     * 오늘 이미 모든 단계를 완료한 사용자용
     */
    private fun showCompletedState() {
        try {
            // 별자리 수집 완료 상태라면 AcceptMissionActivity로 이동
            val intent = Intent(this, AcceptMissionActivity::class.java).apply {
                putExtra("isCompleted", true)
                putExtra("showCompletedState", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "완료 상태 화면 이동 실패: ${e.message}")
            showErrorMessage("오늘은 이미 모든 단계를 완료하셨습니다!")
        }
    }



    private fun updateUIForCurrentState() {
        try {
            // 오늘 사주 생성 여부 확인
            val hasTodaySaju = SharedPreferencesUtils.hasTodaySaju(this)


            //사주 보기 메인버튼 텍스트 변경
            //택스트뷰가 담김
            //
            val dailyButtonText = dailySajuBtn.findViewById<TextView>(R.id.purple_btn)




            dailyButtonText.text = if (hasTodaySaju) {
                "사주 결과 다시보기"
            } else {
                getString(R.string.show_saju)//show_saju가 있니..?
            }

            Log.d(TAG, "UI 상태 업데이트 완료 - 오늘 사주 존재: $hasTodaySaju")

        } catch (e: Exception) {
            Log.e(TAG, "UI 상태 업데이트 실패 ${e.message}")
        }
    }

    /**MARK:
     * 에러 메시지 표시
     * 사용자에게 에러 상황을 알려주는 함수 (로그로만 처리)
     */

    private fun showErrorMessage(message: String) {
        try {
            Log.w(TAG, "에러 메세지:${message}")
        } catch (e: Exception) {
            Log.e(TAG, "에러 메시지 표시 실패: ${e.message}")
        }
    }
    }
