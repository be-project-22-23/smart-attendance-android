package com.example.presentmaam.api

import com.example.presentmaam.models.MessageResponse
import com.example.presentmaam.models.Student
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthenticationRestApi {

    @POST("/api/registration/student")
    suspend fun registerStudent(@Body body: Student): Response<MessageResponse>

    @POST("/api/login/student")
    suspend fun loginStudent(@Body body: Map<String, String>): Response<MessageResponse>

    @GET("/")
    suspend fun flightTest(): Response<MessageResponse>
}