package com.example.presentmaam.api

import com.example.presentmaam.models.Attendance
import com.example.presentmaam.models.ResponseModel
import com.example.presentmaam.models.Student
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GetDataRestApi {

    @GET("/api/activeAttendance")
    suspend fun getAllAttendance(): Response<ResponseModel<List<Attendance>>>

    @GET("/api/studentDetails")
    suspend fun getStudentDetails(
        @Query("studentId")
        studentId: String
    ): Response<ResponseModel<Student>>

    @GET("/api/getAttendanceById")
    suspend fun getAttendanceById(
        @Query("attendanceId")
        attendanceId: String
    ) : Response<ResponseModel<Attendance>>
}