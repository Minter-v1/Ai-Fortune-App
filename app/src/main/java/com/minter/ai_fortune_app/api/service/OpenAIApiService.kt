package com.minter.ai_fortune_app.api.service

import retrofit2.Response
import retrofit2.http.*
import com.minter.ai_fortune_app.data.model.OpenAIRequest
import com.minter.ai_fortune_app.data.model.OpenAIResponse

interface OpenAIApiService {
    @POST("v1/chat/completions") //이 주소로 보냄(엔드포인트)
    @Headers("Content-Type: application/json")//JSON타입의 데이터를 보낸다

    //조금 기다리는 함수 -> 자연 함수 -> 비동기처리를 위한 suspend
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): Response<OpenAIResponse>
}

