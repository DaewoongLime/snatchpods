package com.example.client_android.data

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET

// 1. API 인터페이스 정의
interface AirPodsApi {
    @GET("connect")
    suspend fun connect(): String

    @GET("disconnect")
    suspend fun disconnect(): String
}

// 2. 싱글톤 객체 (DI 없이 간단하게 구현)
object NetworkModule {
    // 주의: 실제 Mac의 IP 주소로 변경하세요! (예: 192.168.0.5)
    // 포트 번호도 아까 설정한 것(5000 or 5001)과 맞춰야 합니다.
    private const val BASE_URL = "http://192.168.219.114:5001"

    val api: AirPodsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(AirPodsApi::class.java)
    }
}