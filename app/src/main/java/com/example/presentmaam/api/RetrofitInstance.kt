package com.example.presentmaam.api

import com.example.presentmaam.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitInstance {
    companion object{
        private val retrofit by lazy {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()
            Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }
        val authApi: AuthenticationRestApi by lazy {
            retrofit.create(AuthenticationRestApi::class.java)
        }
        val getDataApi: GetDataRestApi by lazy {
            retrofit.create(GetDataRestApi::class.java)
        }
        val attendanceApi: AttendanceRestApi by lazy {
            retrofit.create(AttendanceRestApi::class.java)
        }
    }
}