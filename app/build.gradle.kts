import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt") // Room 데이터베이스 컴파일러 지원
}

android {
    namespace = "com.minter.ai_fortune_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.minter.ai_fortune_app"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"


        // OpenAI API 키 설정 (local.properties에서 읽어옴)
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        
        val openaiApiKey = localProperties.getProperty("MOCOM_API_KEY") ?: ""
        
        // API 키 상태 로깅 (빌드 시점)
        println("🔥 BUILD: local.properties 파일 존재: ${localPropertiesFile.exists()}")
        println("🔥 BUILD: MOCOM_API_KEY 길이: ${openaiApiKey.length}")
        println("🔥 BUILD: MOCOM_API_KEY 시작 10자: ${openaiApiKey.take(10)}")
        
        buildConfigField("String", "MOCOM_API_KEY", "\"$openaiApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true //BuildConfig 활성화
    }
}

dependencies {
    // 기본 Android 라이브러리
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    
    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation(libs.androidx.room.external.antlr)
    implementation(libs.androidx.media3.common.ktx)
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    //kotlin 버전 명시
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.0")

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    
    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Lottie 라이브러리
    implementation("com.airbnb.android:lottie:6.1.0")


    // 테스트 라이브러리
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}