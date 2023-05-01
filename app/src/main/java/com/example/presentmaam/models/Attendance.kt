package com.example.presentmaam.models

import com.google.gson.annotations.SerializedName

data class Attendance(
    @SerializedName("attendanceId") val attendanceId: Int,
    @SerializedName("classYear") val classYear: String,
    @SerializedName("classDept") val classDept: String,
    @SerializedName("classSubject") val classSubject: String,
    @SerializedName("startTime") val startTime: Long,
    @SerializedName("students") val students: String,
    @SerializedName("duration") val duration: Long,
    @SerializedName("teacherId") val teacherId: Int,
    @SerializedName("classDivision") val classDivision: Int,
    @SerializedName("isOnGoing") val isOnGoing: Int,
    @SerializedName("classBatch") val classBatch: String
)
