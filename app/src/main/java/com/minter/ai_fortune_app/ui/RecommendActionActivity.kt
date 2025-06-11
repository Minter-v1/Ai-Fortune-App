package com.minter.ai_fortune_app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.minter.ai_fortune_app.R
import com.minter.ai_fortune_app.api.repository.OpenAIRepository
import com.minter.ai_fortune_app.data.model.*
import com.minter.ai_fortune_app.utils.SharedPreferencesUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 위치 기반 미션 추천 액티비티
 *
 * 이 액티비티의 주요 역할:
 * 1. 사용자의 현재 위치를 GPS로 획득
 * 2. 위치 정보를 바탕으로 AI가 맞춤형 미션 생성
 * 3. 생성된 미션을 사용자에게 제시
 * 4. 사용자가 "Try it" 버튼을 누르면 미션 수락 처리
 * 5. 미션 수락 후 AcceptMissionActivity로 이동
 *
 * 화면 플로우:
 * ChatActivity → RecommendActionActivity → AcceptMissionActivity
 *
 * 사용하는 주요 기술:
 * - Google Location Services (GPS 위치 획득)
 * - OpenAI API (위치 기반 미션 생성)
 * - 안드로이드 권한 시스템 (위치 권한 요청)
 * - 코루틴 (비동기 처리)
 */
class RecommendActionActivity : AppCompatActivity() {

    // ================================
    // 상수 및 태그 정의
    // ================================

    companion object {
        // companion object는 Java의 static과 같은 개념
        // 클래스 이름으로 직접 접근할 수 있는 정적 멤버들을 정의
        private const val TAG = "RecommendActionActivity"

        // 위치 권한 요청 코드
        // 안드로이드에서 권한을 요청할 때 사용하는 고유 번호
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

        // 위치 관련 설정값들
        private const val LOCATION_TIMEOUT_MS = 10000L    // 위치 획득 최대 대기 시간 (10초)
        private const val MISSION_GENERATION_DELAY = 2000L // 미션 생성 전 대기 시간 (2초)
    }

    // ================================
    // UI 요소 정의
    // ================================

    // lateinit var는 나중에 초기화할 변수를 의미
    // onCreate에서 findViewById로 실제 뷰 객체와 연결됨
    private lateinit var tvRecommendAction: TextView      // 미션 제목 표시 텍스트뷰
    private lateinit var btnTryAction: View               // "Try it" 버튼
    private lateinit var btnTryText: TextView             // 버튼 내부 텍스트
    private lateinit var layoutModal: View                // 미션 수락 모달창
    private lateinit var btnModalOk: View                 // 모달창의 OK 버튼

    // ================================
    // 데이터 변수들
    // ================================

    // Intent로 받아온 데이터를 저장할 변수들
    private var userName: String = ""                     // 사용자 이름
    private var userBirthDate: String = ""               // 사용자 생년월일
    private var sajuId: String = ""                      // 사주 고유 ID
    private var chatSessionId: String = ""               // 채팅 세션 ID
    private var selectedCategory: SajuCategory = SajuCategory.DAILY  // 선택된 사주 카테고리
    private var categoryDisplayName: String = ""         // 카테고리 표시명

    // 채팅에서 받아온 사용자 메시지들 (감정 분석용)
    private var userMessages: Array<String> = emptyArray()

    // ================================
    // 미션 관련 데이터
    // ================================

    // 현재 생성된 미션 정보
    private var currentMission: Mission? = null          // 현재 추천된 미션
    private var missionTitle: String = ""                // 미션 제목
    private var missionDescription: String = ""          // 미션 설명
    private var userLocation: LocationInfo? = null       // 사용자 위치 정보

    // ================================
    // 위치 서비스 관련
    // ================================

    // Google Location Services를 사용하기 위한 클라이언트
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 위치 요청 취소를 위한 토큰
    private var locationCancellationToken: CancellationTokenSource? = null

    // ================================
    // API 관련
    // ================================

    // OpenAI API를 호출하기 위한 Repository
    // 싱글톤 패턴으로 구현되어 있어 getInstance()로 인스턴스를 가져옴
    private val openAIRepository = OpenAIRepository.getInstance()

    // ================================
    // 상태 관리 변수들
    // ================================

    private var isLocationPermissionGranted: Boolean = false  // 위치 권한 허용 여부
    private var isLocationObtained: Boolean = false          // 위치 획득 완료 여부
    private var isMissionGenerated: Boolean = false          // 미션 생성 완료 여부
    private var isMissionAccepted: Boolean = false           // 미션 수락 여부

    // ================================
    // 액티비티 생명주기 함수들
    // ================================

    /**
     * 액티비티가 생성될 때 호출되는 함수
     *
     * onCreate는 액티비티의 생명주기에서 가장 먼저 호출되는 함수입니다.
     * 여기서 모든 초기화 작업을 수행합니다.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // 부모 클래스의 onCreate 호출 (필수)
        // 이를 호출하지 않으면 앱이 크래시됩니다
        super.onCreate(savedInstanceState)

        // XML 레이아웃 파일을 이 액티비티에 연결
        // activity_recommand_action.xml 파일을 화면에 표시
        setContentView(R.layout.activity_recommand_action)

        // 로그 출력 (개발자가 앱의 동작을 추적하기 위함)
        // Log.d는 Debug 레벨의 로그를 의미
        Log.d(TAG, "RecommendActionActivity 시작")

        // 초기화 순서가 중요합니다
        // 1. Intent로 전달받은 데이터 처리
        handleIntentData()

        // 2. XML의 UI 요소들을 변수에 연결
        initViews()

        // 3. 위치 서비스 초기화
        initLocationServices()

        // 4. 버튼 클릭 이벤트 설정
        setupButtonListeners()

        // 5. 위치 권한 확인 및 요청
        checkLocationPermission()

        Log.d(TAG, "RecommendActionActivity 초기화 완료")
    }

    /**
     * 액티비티가 완전히 종료될 때 호출
     *
     * onDestroy는 액티비티가 메모리에서 제거되기 직전에 호출됩니다.
     * 여기서 리소스 정리 작업을 수행하여 메모리 누수를 방지합니다.
     */
    override fun onDestroy() {
        super.onDestroy()

        // 위치 요청 취소 (배터리 절약을 위해)
        cancelLocationRequest()

        Log.d(TAG, "RecommendActionActivity 종료")
    }

    /**
     * 뒤로가기 버튼이 눌렸을 때 호출
     *
     * 미션 추천 화면에서는 뒤로가기를 허용하지 않습니다.
     * 사용자가 반드시 미션을 수락하고 다음 단계로 진행하도록 유도합니다.
     */
    override fun onBackPressed() {
        Log.d(TAG, "뒤로가기 버튼 클릭 - 무시됨")
        // super.onBackPressed()를 호출하지 않으면 뒤로가기가 동작하지 않음
        showMessage("미션을 선택해주세요!")
    }

    // ================================
    // 데이터 처리 함수들
    // ================================

    /**
     * Intent로 전달받은 데이터를 처리하는 함수
     *
     * Intent는 액티비티 간에 데이터를 전달하는 안드로이드의 메커니즘입니다.
     * ChatActivity에서 보낸 사용자 정보와 채팅 데이터를 받아옵니다.
     */
    private fun handleIntentData() {
        try {
            // intent는 이 액티비티를 시작할 때 전달된 Intent 객체
            // getStringExtra()는 문자열 데이터를 가져오는 함수
            // ?: "기본값"은 null일 경우 기본값을 사용하는 Kotlin의 Elvis 연산자
            userName = intent.getStringExtra("userName") ?: "사용자"
            userBirthDate = intent.getStringExtra("userBirthDate") ?: "0000-00-00"
            sajuId = intent.getStringExtra("sajuId") ?: ""
            chatSessionId = intent.getStringExtra("chatSessionId") ?: ""
            categoryDisplayName = intent.getStringExtra("categoryDisplayName") ?: "오늘의 사주"

            // 카테고리 enum 변환
            // enum은 미리 정의된 상수들의 집합
            val categoryName = intent.getStringExtra("category") ?: "DAILY"
            selectedCategory = try {
                // valueOf()는 문자열을 enum으로 변환하는 함수
                SajuCategory.valueOf(categoryName)
            } catch (e: Exception) {
                // try-catch는 예외(에러) 처리를 위한 구문
                // 예외 발생 시 기본값 사용
                Log.w(TAG, "알 수 없는 카테고리: $categoryName")
                SajuCategory.DAILY
            }

            // 사용자 메시지 배열 가져오기 (채팅에서 전달받은 메시지들)
            userMessages = intent.getStringArrayExtra("userMessages") ?: emptyArray()

            // 문자열 템플릿 사용: ${}로 변수 값을 문자열에 삽입
            Log.d(TAG, "데이터 처리 완료 - 사용자: $userName, 메시지 개수: ${userMessages.size}")

        } catch (e: Exception) {
            // 예외(에러) 발생 시 로그 출력 및 기본값 설정
            Log.e(TAG, "Intent 데이터 처리 실패: ${e.message}")

            // 안전한 기본값 설정
            userName = "사용자"
            userBirthDate = "0000-00-00"
            selectedCategory = SajuCategory.DAILY
            categoryDisplayName = "오늘의 사주"
            userMessages = emptyArray()
        }
    }

    // ================================
    // UI 초기화 함수들
    // ================================

    /**
     * XML 레이아웃의 UI 요소들을 찾아서 변수에 연결하는 함수
     *
     * findViewById()는 XML에서 android:id로 지정된 뷰를 찾는 함수입니다.
     * 이를 통해 코틀린 코드에서 XML의 뷰들을 조작할 수 있습니다.
     */
    private fun initViews() {
        try {
            // activity_recommand_action.xml에서 정의된 ID들
            // R.id.xxx는 XML에서 android:id="@+id/xxx"로 정의된 뷰의 ID
            tvRecommendAction = findViewById(R.id.tv_recommand_action)
            btnTryAction = findViewById(R.id.btn_try_action) // include된 레이아웃의 전체 뷰
            layoutModal = findViewById(R.id.layout_modal) // 모달창 전체 레이아웃

            // include된 컴포넌트 내부의 텍스트뷰 찾기
            // component_no_glow_btn.xml 내부의 TextView
            btnTryText = btnTryAction.findViewById(R.id.tv_btn_text)

            // component_mission_modal.xml 내부의 OK 버튼
            btnModalOk = layoutModal.findViewById(R.id.btn_modal_ok)

            // 초기 텍스트 설정
            tvRecommendAction.text = "미션을\n준비하고\n있어요..."
            btnTryText.text = "위치 확인 중..."

            // 초기 상태: 버튼 비활성화, 모달 숨김
            btnTryAction.isEnabled = false
            layoutModal.visibility = View.GONE  // View.GONE은 뷰를 완전히 숨김

            Log.d(TAG, "UI 요소 초기화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "UI 요소 초기화 실패: ${e.message}")
            showErrorAndFinish("화면 초기화에 실패했습니다.")
        }
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수
     *
     * setOnClickListener는 뷰가 클릭되었을 때 실행할 코드를 설정하는 함수입니다.
     * { } 안에 클릭 시 실행할 코드를 작성합니다.
     */
    private fun setupButtonListeners() {
        try {
            // "Try it" 버튼 클릭 이벤트
            // setOnClickListener { } 는 람다 함수를 의미
            // 클릭 시 { } 안의 코드가 실행됨
            btnTryAction.setOnClickListener {
                Log.d(TAG, "Try it 버튼 클릭")
                onTryActionClicked()
            }

            // 모달창의 OK 버튼 클릭 이벤트
            btnModalOk.setOnClickListener {
                Log.d(TAG, "모달 OK 버튼 클릭")
                onModalOkClicked()
            }

            Log.d(TAG, "버튼 이벤트 설정 완료")

        } catch (e: Exception) {
            Log.e(TAG, "버튼 이벤트 설정 실패: ${e.message}")
        }
    }

    // ================================
    // 위치 서비스 관련 함수들
    // ================================

    /**
     * 위치 서비스를 초기화하는 함수
     *
     * Google Location Services를 사용하기 위한 클라이언트를 생성합니다.
     * 이는 GPS, 네트워크, 센서 등을 종합해 정확한 위치를 제공합니다.
     */
    private fun initLocationServices() {
        try {
            // FusedLocationProviderClient는 Google Play Services에서 제공하는 고급 위치 API
            // 여러 위치 소스(GPS, WiFi, 셀룰러)를 융합해 최적의 위치를 제공
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            Log.d(TAG, "위치 서비스 초기화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "위치 서비스 초기화 실패: ${e.message}")
            handleLocationError("위치 서비스를 사용할 수 없습니다.")
        }
    }

    /**
     * 위치 권한을 확인하고 필요시 요청하는 함수
     *
     * 안드로이드 6.0 이상에서는 위험한 권한에 대해 런타임 권한 요청이 필요합니다.
     * 위치 권한은 사용자의 개인정보와 관련된 민감한 권한입니다.
     */
    private fun checkLocationPermission() {
        try {
            Log.d(TAG, "위치 권한 확인 시작")

            // ContextCompat.checkSelfPermission은 현재 권한 상태를 확인하는 함수
            // PackageManager.PERMISSION_GRANTED는 권한이 허용된 상태를 의미
            val fineLocationPermission = ContextCompat.checkSelfPermission(
                this, // 현재 액티비티의 컨텍스트
                Manifest.permission.ACCESS_FINE_LOCATION // 정밀한 위치 권한
            )

            // 권한이 이미 허용되어 있는지 확인
            isLocationPermissionGranted = fineLocationPermission == PackageManager.PERMISSION_GRANTED

            if (isLocationPermissionGranted) {
                // 권한이 있으면 바로 위치 획득 시작
                Log.d(TAG, "위치 권한 이미 허용됨 - 위치 획득 시작")
                startLocationAcquisition()
            } else {
                // 권한이 없으면 사용자에게 권한 요청
                Log.d(TAG, "위치 권한 없음 - 권한 요청")
                requestLocationPermission()
            }

        } catch (e: Exception) {
            Log.e(TAG, "위치 권한 확인 실패: ${e.message}")
            handleLocationError("위치 권한 확인 중 오류가 발생했습니다.")
        }
    }

    /**
     * 위치 권한을 요청하는 함수
     *
     * 사용자에게 위치 권한을 허용할지 묻는 시스템 다이얼로그를 표시합니다.
     */
    private fun requestLocationPermission() {
        try {
            Log.d(TAG, "위치 권한 요청 다이얼로그 표시")

            // ActivityCompat.requestPermissions는 권한 요청 다이얼로그를 표시하는 함수
            ActivityCompat.requestPermissions(
                this, // 현재 액티비티
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), // 요청할 권한 배열
                LOCATION_PERMISSION_REQUEST_CODE // 요청 코드 (응답을 구분하기 위한 고유 번호)
            )

            // 사용자에게 권한이 필요한 이유 설명
            updateUIForPermissionRequest()

        } catch (e: Exception) {
            Log.e(TAG, "위치 권한 요청 실패: ${e.message}")
            handleLocationError("위치 권한 요청에 실패했습니다.")
        }
    }

    /**
     * 권한 요청 결과를 처리하는 함수
     *
     * 이 함수는 사용자가 권한 요청 다이얼로그에서 선택을 했을 때 자동으로 호출됩니다.
     * override fun은 부모 클래스의 함수를 재정의한다는 의미입니다.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, // 권한 요청 시 전달한 요청 코드
        permissions: Array<out String>, // 요청한 권한들의 배열
        grantResults: IntArray // 각 권한에 대한 허용/거부 결과 배열
    ) {
        // 부모 클래스의 onRequestPermissionsResult 호출 (필수)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        try {
            Log.d(TAG, "권한 요청 결과 처리 - 요청 코드: $requestCode")

            // 우리가 요청한 위치 권한에 대한 응답인지 확인
            if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
                // grantResults 배열이 비어있지 않고, 첫 번째 결과가 허용인지 확인
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // 권한이 허용됨
                    Log.d(TAG, "위치 권한 허용됨")
                    isLocationPermissionGranted = true
                    startLocationAcquisition()

                } else {
                    // 권한이 거부됨
                    Log.w(TAG, "위치 권한 거부됨")
                    isLocationPermissionGranted = false
                    handleLocationPermissionDenied()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "권한 요청 결과 처리 실패: ${e.message}")
            handleLocationError("권한 처리 중 오류가 발생했습니다.")
        }
    }

    /**
     * 위치 권한이 거부되었을 때 처리하는 함수
     */
    private fun handleLocationPermissionDenied() {
        try {
            Log.w(TAG, "위치 권한 거부 처리")

            // UI 업데이트
            tvRecommendAction.text = "위치 권한이\n필요해요"
            btnTryText.text = "설정에서 권한 허용"
            btnTryAction.isEnabled = true

            // 버튼 클릭 시 설정 화면으로 이동하도록 변경
            btnTryAction.setOnClickListener {
                openAppSettings()
            }

        } catch (e: Exception) {
            Log.e(TAG, "권한 거부 처리 실패: ${e.message}")
        }
    }

    /**
     * 앱 설정 화면으로 이동하는 함수
     */
    private fun openAppSettings() {
        try {
            Log.d(TAG, "앱 설정 화면으로 이동")

            // 앱 설정 화면으로 이동하는 Intent 생성
            val intent = Intent().apply {
                action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = android.net.Uri.fromParts("package", packageName, null)
            }

            startActivity(intent)

        } catch (e: Exception) {
            Log.e(TAG, "앱 설정 화면 이동 실패: ${e.message}")
            showMessage("설정 화면을 열 수 없습니다.")
        }
    }

    /**
     * 위치 획득을 시작하는 함수
     *
     * 권한이 허용된 후 실제로 GPS를 사용해 현재 위치를 가져옵니다.
     */
    private fun startLocationAcquisition() {
        try {
            Log.d(TAG, "위치 획득 시작")

            // UI 업데이트
            tvRecommendAction.text = "현재 위치를\n확인하고\n있어요..."
            btnTryText.text = "위치 확인 중..."

            // 위치 요청 설정
            // 위치의 정확도, 업데이트 간격 등을 설정
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY // 높은 정확도 (GPS 사용)
                interval = 10000 // 위치 업데이트 간격 (10초)
                fastestInterval = 5000 // 가장 빠른 업데이트 간격 (5초)
                numUpdates = 1 // 한 번만 위치 획득
            }

            // 권한 재확인 (안전을 위해)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "위치 권한이 없음")
                handleLocationError("위치 권한이 필요합니다.")
                return
            }

            // 위치 요청 취소 토큰 생성
            // 이는 나중에 위치 요청을 취소할 때 사용
            locationCancellationToken = CancellationTokenSource()

            // 현재 위치 획득 요청
            // getCurrentLocation은 한 번만 위치를 가져오는 함수
            fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                locationCancellationToken!!.token
            ).addOnSuccessListener { location ->
                // 위치 획득 성공 시 호출되는 콜백
                // location은 위도, 경도 정보를 포함한 Location 객체
                if (location != null) {
                    Log.d(TAG, "위치 획득 성공: ${location.latitude}, ${location.longitude}")
                    handleLocationSuccess(location)
                } else {
                    Log.w(TAG, "위치 정보가 null")
                    handleLocationError("위치를 찾을 수 없습니다.")
                }
            }.addOnFailureListener { exception ->
                // 위치 획득 실패 시 호출되는 콜백
                Log.e(TAG, "위치 획득 실패: ${exception.message}")
                handleLocationError("위치 획득에 실패했습니다.")
            }

            // 타임아웃 설정 (10초 후에도 위치를 못 가져오면 실패 처리)
            startLocationTimeout()

        } catch (e: Exception) {
            Log.e(TAG, "위치 획득 시작 실패: ${e.message}")
            handleLocationError("위치 서비스 시작에 실패했습니다.")
        }
    }

    /**
     * 위치 획득 타임아웃을 설정하는 함수
     */
    private fun startLocationTimeout() {
        // lifecycleScope.launch는 코루틴을 시작하는 함수
        // 코루틴은 비동기 작업을 위한 코틀린의 기능
        lifecycleScope.launch {
            try {
                // delay는 지정된 시간만큼 대기하는 suspend 함수
                delay(LOCATION_TIMEOUT_MS)

                // 아직 위치를 못 가져왔다면 타임아웃 처리
                if (!isLocationObtained) {
                    Log.w(TAG, "위치 획득 타임아웃")
                    cancelLocationRequest()
                    handleLocationTimeout()
                }

            } catch (e: Exception) {
                Log.e(TAG, "위치 타임아웃 처리 실패: ${e.message}")
            }
        }
    }

    /**
     * 위치 요청을 취소하는 함수
     */
    private fun cancelLocationRequest() {
        try {
            // 위치 요청 취소 토큰이 있다면 취소 실행
            locationCancellationToken?.cancel()
            locationCancellationToken = null

            Log.d(TAG, "위치 요청 취소 완료")

        } catch (e: Exception) {
            Log.e(TAG, "위치 요청 취소 실패: ${e.message}")
        }
    }

    /**
     * 위치 획득 성공 시 처리하는 함수
     *
     * GPS로부터 위치 정보를 성공적으로 받아왔을 때 호출됩니다.
     * 받아온 위치 정보로 LocationInfo 객체를 생성하고 미션 생성을 시작합니다.
     */
    private fun handleLocationSuccess(location: Location) {
        try {
            Log.d(TAG, "위치 처리 시작 - 위도: ${location.latitude}, 경도: ${location.longitude}")

            // 위치 획득 완료 플래그 설정
            isLocationObtained = true

            // 위치 정보를 우리 앱의 데이터 모델로 변환
            // Location 객체 (안드로이드 기본)를 LocationInfo 객체 (우리 앱 전용)로 변환
            userLocation = LocationInfo(
                address = "현재 위치", // 실제로는 Geocoding API로 주소를 변환할 수 있음
                latitude = location.latitude,   // 위도 (남북 위치)
                longitude = location.longitude, // 경도 (동서 위치)
                timestamp = System.currentTimeMillis() // 현재 시간 (밀리초)
            )

            // UI 업데이트
            tvRecommendAction.text = "맞춤 미션을\n생성하고\n있어요..."
            btnTryText.text = "미션 생성 중..."

            // 미션 생성 시작 (약간의 지연 후)
            startMissionGeneration()

        } catch (e: Exception) {
            Log.e(TAG, "위치 처리 실패: ${e.message}")
            handleLocationError("위치 정보 처리 중 오류가 발생했습니다.")
        }
    }

    /**
     * 위치 획득 실패 시 처리하는 함수
     */
    private fun handleLocationError(errorMessage: String) {
        try {
            Log.w(TAG, "위치 오류 처리: $errorMessage")

            // 기본 위치로 대체 (대전 유성구 카이스트)
            userLocation = LocationInfo(
                address = "대전 유성구",
                latitude = 36.3741,
                longitude = 127.3604,
                timestamp = System.currentTimeMillis()
            )

            // UI 업데이트
            tvRecommendAction.text = "기본 위치로\n미션을 생성할게요"
            btnTryText.text = "미션 생성 중..."

            // 잠시 후 미션 생성 시작
            lifecycleScope.launch {
                delay(1000)
                startMissionGeneration()
            }

        } catch (e: Exception) {
            Log.e(TAG, "위치 오류 처리 실패: ${e.message}")
            showErrorAndFinish("위치 관련 오류가 발생했습니다.")
        }
    }

    /**
     * 위치 획득 타임아웃 처리 함수
     */
    private fun handleLocationTimeout() {
        try {
            Log.w(TAG, "위치 획득 타임아웃 처리")

            // 타임아웃 시에도 기본 위치로 진행
            handleLocationError("위치 확인 시간이 초과되었습니다. 기본 위치로 진행합니다.")

        } catch (e: Exception) {
            Log.e(TAG, "위치 타임아웃 처리 실패: ${e.message}")
        }
    }

    // ================================
    // 미션 생성 관련 함수들
    // ================================

    /**
     * AI를 사용해 미션을 생성하는 함수
     *
     * 사용자의 위치 정보를 바탕으로 OpenAI API를 호출해
     * 맞춤형 행운의 액션(미션)을 생성합니다.
     */
    private fun startMissionGeneration() {
        // lifecycleScope.launch는 코루틴(비동기 처리)을 시작
        lifecycleScope.launch {
            try {
                Log.d(TAG, "미션 생성 시작")

                // userLocation이 null이면 에러 처리
                if (userLocation == null) {
                    Log.e(TAG, "위치 정보가 없음")
                    handleMissionGenerationError("위치 정보가 없습니다.")
                    return@launch // 코루틴에서 함수 종료
                }

                // 미션 생성 전 약간의 지연 (사용자 경험 개선)
                delay(MISSION_GENERATION_DELAY)

                // OpenAI API를 통한 미션 생성
                // generateMission은 suspend 함수이므로 코루틴에서 호출
                val (title, description) = openAIRepository.generateMission(userLocation!!)

                // 생성된 미션 정보 저장
                missionTitle = title
                missionDescription = description

                // Mission 객체 생성
                currentMission = Mission(
                    title = missionTitle,
                    description = missionDescription,
                    location = userLocation!!.address,
                    status = MissionStatus.RECOMMENDED // 초기 상태는 "추천됨"
                )

                // 미션 생성 완료 플래그 설정
                isMissionGenerated = true

                // UI 업데이트 (메인 스레드에서 실행)
                updateUIWithGeneratedMission()

                Log.d(TAG, "미션 생성 완료 - 제목: $missionTitle")

            } catch (e: Exception) {
                Log.e(TAG, "미션 생성 실패: ${e.message}")
                handleMissionGenerationError("미션 생성 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 생성된 미션으로 UI를 업데이트하는 함수
     */
    private fun updateUIWithGeneratedMission() {
        try {
            // 메인 스레드에서 실행되는지 확인
            // UI 업데이트는 반드시 메인 스레드에서 해야 함
            runOnUiThread {
                // 미션 제목 표시
                tvRecommendAction.text = missionTitle

                // 버튼 활성화 및 텍스트 변경
                btnTryText.text = "Try it!"
                btnTryAction.isEnabled = true
                btnTryAction.alpha = 1.0f // 완전 불투명

                Log.d(TAG, "미션 UI 업데이트 완료")
            }

        } catch (e: Exception) {
            Log.e(TAG, "미션 UI 업데이트 실패: ${e.message}")
        }
    }

    /**
     * 미션 생성 실패 시 처리하는 함수
     */
    private fun handleMissionGenerationError(errorMessage: String) {
        try {
            Log.w(TAG, "미션 생성 오류: $errorMessage")

            // 기본 미션으로 대체
            missionTitle = "오늘의 힐링 미션"
            missionDescription = "주변을 천천히 산책하며 좋은 기운을 받아보세요! ✨"

            // 기본 Mission 객체 생성
            currentMission = Mission(
                title = missionTitle,
                description = missionDescription,
                location = userLocation?.address ?: "현재 위치",
                status = MissionStatus.RECOMMENDED
            )

            isMissionGenerated = true

            // UI 업데이트
            updateUIWithGeneratedMission()

            Log.d(TAG, "기본 미션으로 대체 완료")

        } catch (e: Exception) {
            Log.e(TAG, "미션 오류 처리 실패: ${e.message}")
            showErrorAndFinish("미션을 준비할 수 없습니다.")
        }
    }

    // ================================
    // 권한 요청 UI 업데이트 함수들
    // ================================

    /**
     * 권한 요청 중일 때 UI를 업데이트하는 함수
     */
    private fun updateUIForPermissionRequest() {
        try {
            tvRecommendAction.text = "위치 권한이\n필요해요"
            btnTryText.text = "권한 허용 대기 중..."
            btnTryAction.isEnabled = false

            Log.d(TAG, "권한 요청 UI 업데이트")

        } catch (e: Exception) {
            Log.e(TAG, "권한 요청 UI 업데이트 실패: ${e.message}")
        }
    }

    // ================================
    // 버튼 클릭 이벤트 처리 함수들
    // ================================

    /**
     * "Try it" 버튼이 클릭되었을 때 처리하는 함수
     *
     * 사용자가 미션을 수락한다는 의미입니다.
     * 미션 수락 확인 모달을 표시합니다.
     */
    private fun onTryActionClicked() {
        try {
            Log.d(TAG, "Try it 버튼 클릭 처리")

            // 미션이 생성되지 않았다면 클릭 무시
            if (!isMissionGenerated || currentMission == null) {
                Log.w(TAG, "미션이 준비되지 않음")
                showMessage("미션을 준비 중입니다. 잠시만 기다려주세요.")
                return
            }

            // 이미 미션을 수락했다면 중복 수락 방지
            if (isMissionAccepted) {
                Log.w(TAG, "이미 미션 수락됨")
                showMessage("이미 미션을 수락하셨습니다!")
                return
            }

            // 미션 수락 확인 모달 표시
            showMissionAcceptModal()

        } catch (e: Exception) {
            Log.e(TAG, "Try it 버튼 처리 실패: ${e.message}")
            showMessage("미션 수락 처리 중 오류가 발생했습니다.")
        }
    }

    /**
     * 미션 수락 확인 모달을 표시하는 함수
     */
    private fun showMissionAcceptModal() {
        try {
            Log.d(TAG, "미션 수락 모달 표시")

            // 모달창 내용 업데이트
            updateModalContent()

            // 모달창 표시 (애니메이션 효과 추가 가능)
            layoutModal.visibility = View.VISIBLE
            layoutModal.alpha = 0f // 투명하게 시작
            layoutModal.animate()
                .alpha(1f) // 불투명하게 변경
                .setDuration(300) // 300ms 동안 애니메이션
                .start()

        } catch (e: Exception) {
            Log.e(TAG, "미션 수락 모달 표시 실패: ${e.message}")
        }
    }

    /**
     * 모달창 내용을 업데이트하는 함수
     */
    private fun updateModalContent() {
        try {
            // component_mission_modal.xml 내부의 텍스트들 업데이트
            val modalTitle = layoutModal.findViewById<TextView>(R.id.tv_modal_title)
            val modalDescription = layoutModal.findViewById<TextView>(R.id.tv_modal_description)
            val modalButtonText = btnModalOk.findViewById<TextView>(R.id.tv_btn_text)

            modalTitle?.text = "미션 수락"
            modalDescription?.text = "오늘의 미션을 수락했습니다.\n오후 9시에 뾰롱이가 확인하러 올거에요!"
            modalButtonText?.text = "확인"

            Log.d(TAG, "모달 내용 업데이트 완료")

        } catch (e: Exception) {
            Log.e(TAG, "모달 내용 업데이트 실패: ${e.message}")
        }
    }

    /**
     * 모달의 OK 버튼이 클릭되었을 때 처리하는 함수
     *
     * 사용자가 미션 수락을 최종 확인한 것입니다.
     * 미션 상태를 업데이트하고 다음 화면으로 이동합니다.
     */
    private fun onModalOkClicked() {
        try {
            Log.d(TAG, "모달 OK 버튼 클릭 처리")

            // 모달창 숨기기
            hideModal()

            // 미션 수락 처리
            acceptMission()

            // 잠시 후 다음 화면으로 이동
            lifecycleScope.launch {
                delay(1000) // 1초 대기
                proceedToAcceptMissionActivity()
            }

        } catch (e: Exception) {
            Log.e(TAG, "모달 OK 처리 실패: ${e.message}")
            showMessage("미션 처리 중 오류가 발생했습니다.")
        }
    }

    /**
     * 모달창을 숨기는 함수
     */
    private fun hideModal() {
        try {
            // 페이드 아웃 애니메이션
            layoutModal.animate()
                .alpha(0f) // 투명하게 변경
                .setDuration(300) // 300ms 동안
                .withEndAction {
                    // 애니메이션 완료 후 완전히 숨김
                    layoutModal.visibility = View.GONE
                }
                .start()

        } catch (e: Exception) {
            Log.e(TAG, "모달 숨기기 실패: ${e.message}")
            layoutModal.visibility = View.GONE
        }
    }

    /**
     * 미션을 수락 처리하는 함수
     *
     * 미션 상태를 업데이트하고 관련 데이터를 저장합니다.
     */
    private fun acceptMission() {
        try {
            Log.d(TAG, "미션 수락 처리 시작")

            // 미션이 없다면 에러
            if (currentMission == null) {
                Log.e(TAG, "수락할 미션이 없음")
                return
            }

            // 미션 상태를 "수락됨"으로 변경
            currentMission = currentMission!!.nextStatus() // RECOMMENDED → ACCEPTED

            // 미션 수락 플래그 설정
            isMissionAccepted = true

            // SharedPreferences에 오늘의 미션 기록
            SharedPreferencesUtils.saveTodayMission(this, currentMission!!.id)

            Log.d(TAG, "미션 수락 처리 완료 - ID: ${currentMission!!.id}")

        } catch (e: Exception) {
            Log.e(TAG, "미션 수락 처리 실패: ${e.message}")
        }
    }

    // ================================
    // 화면 이동 관련 함수들
    // ================================

    /**
     * AcceptMissionActivity로 이동하는 함수
     *
     * 미션을 수락한 후 미션 진행 상황을 관리하는 화면으로 이동합니다.
     */
    private fun proceedToAcceptMissionActivity() {
        try {
            Log.d(TAG, "AcceptMissionActivity로 이동 시작")

            // 미션이 수락되지 않았다면 이동 불가
            if (!isMissionAccepted || currentMission == null) {
                Log.w(TAG, "미션 미수락 상태 - 이동 불가")
                showMessage("먼저 미션을 수락해주세요!")
                return
            }

            // AcceptMissionActivity로 이동
            val intent = Intent(this, AcceptMissionActivity::class.java).apply {
                // 사용자 정보 전달
                putExtra("userName", userName)
                putExtra("userBirthDate", userBirthDate)

                // 사주 및 채팅 정보 전달
                putExtra("sajuId", sajuId)
                putExtra("chatSessionId", chatSessionId)
                putExtra("category", selectedCategory.name)
                putExtra("categoryDisplayName", categoryDisplayName)

                // 미션 정보 전달
                putExtra("missionId", currentMission!!.id)
                putExtra("missionTitle", currentMission!!.title)
                putExtra("missionDescription", currentMission!!.description)
                putExtra("missionLocation", currentMission!!.location)
                putExtra("missionStatus", currentMission!!.status.name)

                // 위치 정보 전달
                putExtra("userLatitude", userLocation?.latitude ?: 0.0)
                putExtra("userLongitude", userLocation?.longitude ?: 0.0)
                putExtra("userAddress", userLocation?.address ?: "현재 위치")

                // 사용자 메시지들 전달 (감정 분석용)
                putExtra("userMessages", userMessages)

                // 미션 수락 완료 플래그
                putExtra("missionAccepted", true)
            }

            startActivity(intent)

            // 현재 액티비티 종료 (뒤로가기 방지)
            finish()

            Log.d(TAG, "AcceptMissionActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "AcceptMissionActivity 이동 실패: ${e.message}")
            showErrorMessage("다음 화면으로 이동하는 중 오류가 발생했습니다.")
        }
    }

    /**
     * MainActivity로 돌아가는 함수 (에러 발생 시 사용)
     */
    private fun returnToMainActivity() {
        try {
            Log.d(TAG, "MainActivity로 돌아가기")

            val intent = Intent(this, MainActivity::class.java).apply {
                // 모든 이전 액티비티를 스택에서 제거하고 새로 시작
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

                // 에러 정보 전달
                putExtra("hasError", true)
                putExtra("errorMessage", "미션 생성 중 오류가 발생했습니다.")
            }

            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Log.e(TAG, "MainActivity 돌아가기 실패: ${e.message}")
            finish()
        }
    }

    // ================================
    // 유틸리티 함수들
    // ================================

    /**
     * 사용자에게 간단한 메시지를 표시하는 함수
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
     * 에러 메시지를 표시하는 함수
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
     * 심각한 에러 발생 시 액티비티를 종료하는 함수
     */
    private fun showErrorAndFinish(message: String) {
        try {
            Log.e(TAG, "심각한 오류: $message")

            showMessage(message)

            // 3초 후 MainActivity로 돌아가기
            lifecycleScope.launch {
                delay(3000)
                returnToMainActivity()
            }

        } catch (e: Exception) {
            Log.e(TAG, "에러 처리 실패: ${e.message}")
            finish()
        }
    }

    /**
     * 현재 액티비티의 상태를 확인하는 함수 (디버깅용)
     */
    private fun debugCurrentState() {
        try {
            Log.d(TAG, "=== RecommendActionActivity 상태 ===")
            Log.d(TAG, "사용자: $userName")
            Log.d(TAG, "위치 권한: $isLocationPermissionGranted")
            Log.d(TAG, "위치 획득: $isLocationObtained")
            Log.d(TAG, "미션 생성: $isMissionGenerated")
            Log.d(TAG, "미션 수락: $isMissionAccepted")
            Log.d(TAG, "현재 위치: ${userLocation?.address}")
            Log.d(TAG, "미션 제목: $missionTitle")
            Log.d(TAG, "========================================")

        } catch (e: Exception) {
            Log.e(TAG, "상태 디버깅 실패: ${e.message}")
        }
    }

    /**
     * 액티비티가 다시 활성화될 때 호출되는 함수
     *
     * 설정 화면에서 돌아왔을 때 권한 상태를 다시 확인합니다.
     */
    override fun onResume() {
        super.onResume()

        try {
            Log.d(TAG, "onResume - 권한 상태 재확인")

            // 위치 권한 상태 재확인
            val currentPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            val wasPermissionGranted = isLocationPermissionGranted
            isLocationPermissionGranted = currentPermission == PackageManager.PERMISSION_GRANTED

            // 권한 상태가 변경되었다면 처리
            if (!wasPermissionGranted && isLocationPermissionGranted) {
                Log.d(TAG, "권한이 새로 허용됨 - 위치 획득 시작")

                // 아직 위치를 못 가져왔다면 다시 시도
                if (!isLocationObtained) {
                    startLocationAcquisition()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "onResume 처리 실패: ${e.message}")
        }
    }

    /**
     * 액티비티가 일시 정지될 때 호출되는 함수
     *
     * 배터리 절약을 위해 위치 요청을 일시 정지합니다.
     */
    override fun onPause() {
        super.onPause()

        try {
            Log.d(TAG, "onPause - 위치 요청 일시 정지")

            // 진행 중인 위치 요청이 있다면 일시 정지
            if (!isLocationObtained) {
                cancelLocationRequest()
            }

        } catch (e: Exception) {
            Log.e(TAG, "onPause 처리 실패: ${e.message}")
        }
    }

    // ================================
    // Geocoding 관련 함수들 (선택사항)
    // ================================

    /**
     * 위도/경도를 주소로 변환하는 함수 (선택 구현)
     *
     * Google Geocoding API를 사용해 좌표를 실제 주소로 변환할 수 있습니다.
     * 현재는 간단히 "현재 위치"로 표시하지만, 실제 주소로 변환 가능합니다.
     */
    private fun convertLocationToAddress(latitude: Double, longitude: Double): String {
        return try {
            // TODO: Geocoding API 구현
            // val geocoder = Geocoder(this, Locale.getDefault())
            // val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            // addresses?.firstOrNull()?.getAddressLine(0) ?: "현재 위치"

            // 현재는 기본값 반환
            "현재 위치"

        } catch (e: Exception) {
            Log.e(TAG, "주소 변환 실패: ${e.message}")
            "현재 위치"
        }
    }

    /**
     * 특정 위치에 맞는 맞춤형 미션을 생성하는 함수 (확장 가능)
     *
     * 위치별로 다른 미션을 제안할 수 있습니다.
     */
    private fun generateLocationSpecificMission(locationInfo: LocationInfo): Pair<String, String> {
        return try {
            val address = locationInfo.address.lowercase()

            when {
                address.contains("대전") || address.contains("유성") -> Pair(
                    "화산천 런닝",
                    "화산천을 따라 가볍게 런닝하며 자연의 에너지를 느껴보세요! 🏃‍♀️"
                )
                address.contains("서울") -> Pair(
                    "도심 속 쉼터 찾기",
                    "바쁜 서울 속에서 작은 쉼터를 찾아 잠시 여유를 즐겨보세요! 🏙️"
                )
                address.contains("부산") -> Pair(
                    "바다 바람 맞기",
                    "바다 근처로 가서 시원한 바람을 맞으며 마음을 정화해보세요! 🌊"
                )
                else -> Pair(
                    "주변 산책하기",
                    "주변을 천천히 산책하며 좋은 기운을 받아보세요! ✨"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "위치별 미션 생성 실패: ${e.message}")
            Pair("오늘의 힐링 미션", "주변을 둘러보며 마음을 편안하게 해보세요! 💚")
        }
    }
}