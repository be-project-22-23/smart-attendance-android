package com.example.presentmaam.api

import com.example.presentmaam.models.MessageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST

interface AttendanceRestApi {

    @POST("/api/verifyLocation")
    suspend fun verifyLocation(@Body body: Map<String, String>): Response<MessageResponse>

    @POST("/api/markAttendance")
    suspend fun markAttendance(@Body body: Map<String, String>): Response<MessageResponse>

    @POST("/api/verifyFace")
    suspend fun verifyFace(
        @Body body: Map<String, String>
    ): Response<MessageResponse>
}