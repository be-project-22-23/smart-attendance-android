package com.example.presentmaam.utils

import com.example.presentmaam.R
import com.example.presentmaam.models.Attendance
import com.example.presentmaam.models.AttendanceCountModel
import com.example.presentmaam.models.Student
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

object Constants {
    val BASE_URL = "http://192.168.82.56:3000"
    val resourceIds = arrayOf(
        R.mipmap.ic_laugh,
        R.mipmap.ic_normal,
        R.mipmap.ic_small_smile,
        R.mipmap.ic_eyes_closed,
        R.mipmap.ic_smile
    )
    val imageStatements = arrayOf(
        "Please click the photo while laughing with eyes open",
        "Please click the photo with blank face",
        "Please click the photo with a small smile",
        "Please click the photo with eyes closed",
        "Please click the photo with looking forward with a bit up smiling"
    )
    var student: Student? = null
    val STUDENT_ID_KEY = "student_id_key"
    var studentId: Int? = null
    var allAttendance: List<Attendance>? = null
    var attendance: Attendance? = null
    var attendanceCount: AttendanceCountModel? = null
}