package com.minter.ai_fortune_app

import android.app.Application
import android.util.Log

/**
 * AI Fortune App의 메인 Application 클래스
 * 앱이 시작될 때 가장 먼저 실행되는 클래스
 */
class AiFortuneApplication : Application() {

    companion object {
        private const val TAG = "AiFortuneApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, " AI Fortune App 시작")
        
        // 앱 전체 초기화 작업 수행
        initializeApp()
    }

    private fun initializeApp() {
        try {
            // TODO: 필요한 초기화 작업들
            // - 데이터베이스 초기화
            // - API 클라이언트 설정
            // - 글로벌 설정 로드
            
            Log.d(TAG, "앱 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "앱 초기화 실패: ${e.message}")
        }
    }
}
