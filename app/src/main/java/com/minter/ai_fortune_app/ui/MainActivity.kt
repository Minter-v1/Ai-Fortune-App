package com.minter.ai_fortune_app.ui.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.minter.ai_fortune_app.R
import android.content.Intent
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import com.minter.ai_fortune_app.ui.saju.SajuInputActivity
import org.w3c.dom.Text

class MainActivity : AppCompatActivity() {

    private lateinit var dailySajuBtn: View
    private lateinit var loveSajuBtn: View
    private lateinit var careerSajuBtn: View
    private lateinit var studySajuBtn: View
    private lateinit var healthSajuBtn: View

    //MARK: 앱이 시작될 때 실행되는 함수
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main) //XML 연결



        initViews() //화면 요소 찾기
        setupButtonListeners() //버튼 이벤트 리스터
        updateUIForCurrentState() //화면 전환 함수
    }




    override fun onResume() {
        super.onResume()
        updateUIForCurrentState()
    }



    private fun initViews() {

        dailySajuBtn = findViewById(R.id.daily_saju_btn)
        loveSajuBtn = findViewById(R.id.love_saju_btn)
        careerSajuBtn = findViewById(R.id.career_saju_btn)
        studySajuBtn = findViewById(R.id.study_saju_btn)
        healthSajuBtn = findViewById(R.id.health_saju_btn)

        setCategoryButtonTexts() //버튼 글자 변경
    }

    //MARK: - 버튼 안의 글자 변경
    private fun setCategoryButtonTexts() {
        loveSajuBtn.findViewById<TextView>(R.id.purple_btn).text = getString(R.string.love)
        careerSajuBtn.findViewById<TextView>(R.id.purple_btn).text = getString(R.string.career)
        studySajuBtn.findViewById<TextView>(R.id.purple_btn).text = getString(R.string.study)
        healthSajuBtn.findViewById<TextView>(R.id.purple_btn).text = getString(R.string.health)
    }

    //MARK: - 버튼 클릭 이벤트 리스너
    private fun setupButtonListeners() {
        //도메인 지정
        dailySajuBtn.setOnClickListener { onCategorySelected("DAILY") }
        loveSajuBtn.setOnClickListener { onCategorySelected("LOVE") }
        careerSajuBtn.setOnClickListener { onCategorySelected("CAREER") }
        studySajuBtn.setOnClickListener { onCategorySelected("STUDY") }
        healthSajuBtn.setOnClickListener { onCategorySelected("HEALTH") }
    }


    //MARK: - 카테고리별 버튼 눌렀을때 -> qa뭐 어쩌라는거지
    private fun onCategorySelected(category: String) {
        startSajuInput(category)
        }

    //MARK: - 정보 입력 화면으로 이동
    private fun startSajuInput(category: String) {
        //이동할 액티비티 지정
        val intent = Intent(this, SajuInputActivity::class.java)
        //카테고리 정보 보냄
        intent.putExtra("category",category)

        startActivity(intent)
    }

    private fun updateUIForCurrentState() {

    }

    }
