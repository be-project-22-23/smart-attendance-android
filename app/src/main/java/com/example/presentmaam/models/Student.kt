package com.example.presentmaam.models

import com.google.gson.annotations.SerializedName

data class Student(
    @SerializedName("studentId") val studentId: Int?,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("cpassword") val cpassword: String?,
    @SerializedName("rollNo") val rollNo: String,
    @SerializedName("department") val department: String,
    @SerializedName("batch") val batch: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("currentYear") val currentYear: String,
    @SerializedName("photoUrl") var photoUrl: String?,
    @SerializedName("createdAt") val createdAt: Long?,
    @SerializedName("division") val division: String
)
