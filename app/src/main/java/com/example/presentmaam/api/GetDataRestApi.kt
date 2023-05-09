package com.example.presentmaam.api

import com.example.presentmaam.models.Attendance
import com.example.presentmaam.models.AttendanceCountModel
import com.example.presentmaam.models.ResponseModel
import com.example.presentmaam.models.Student
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GetDataRestApi {

    @GET("/api/getAttendance")
    suspend fun getAllAttendance(
        @Query("classDept")
        classDept: String,
        @Query("currentYear")
        currentYear: String,
        @Query("classDivision")
        classDivision: String
    ): Response<ResponseModel<List<Attendance>>>

    @GET("/api/studentDetails")
    suspend fun getStudentDetails(
        @Query("studentId")
        studentId: String
    ): Response<ResponseModel<Student>>

    @GET("/api/attendanceCount")
    suspend fun getAttendanceCount(
        @Query("studentId")
        studentId: String
    ): Response<AttendanceCountModel>
}