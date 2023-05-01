package com.example.presentmaam.models

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("message") val message: String,
    @SerializedName("id") val studentId: Int?
)
