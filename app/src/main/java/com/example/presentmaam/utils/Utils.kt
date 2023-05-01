package com.example.presentmaam.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Bitmap
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit


object Utils {

    fun updateSharedPreferences(context: Context, key: String, value: String) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(getApplicationName(context), MODE_PRIVATE)
        val myEdit = sharedPreferences.edit()

        myEdit.putString(key, value)
        myEdit.apply()
    }

    fun getValueFromSharedPreferences(context: Context, key: String): String? {
        val sh: SharedPreferences =
            context.getSharedPreferences(getApplicationName(context), MODE_PRIVATE)
        return sh.getString(key, "")
    }

    private fun getApplicationName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(
            stringId
        )
    }

    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        val formatter = SimpleDateFormat(dateFormat)
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    fun getMinutes(millis: Long): String {
        return String.format("%d min, %d sec",
            TimeUnit.MILLISECONDS.toMinutes(millis),
            TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        )
    }

    fun Bitmap.getResizedBitmap(maxSize: Int): Bitmap? {
        var width = this.width
        var height = this.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(this, width, height, true)
    }

}