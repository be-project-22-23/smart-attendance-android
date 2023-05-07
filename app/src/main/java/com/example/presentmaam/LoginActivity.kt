package com.example.presentmaam

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.presentmaam.api.RetrofitInstance
import com.example.presentmaam.databinding.ActivityLoginBinding
import com.example.presentmaam.fragments.LoginFragment
import com.example.presentmaam.utils.Constants
import com.example.presentmaam.utils.Utils
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var baseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when(status) {
                LoaderCallbackInterface.SUCCESS -> {
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(applicationContext)

        if (!supportFragmentManager.isDestroyed ) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.frameLayout, LoginFragment())
                commit()
            }
        }
        getData()

        getPermissions()
    }

    private fun getPermissions(){
        if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            checkToMoveToNewActivity()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == 111) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                getPermissions()
            }
        } else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onResume() {
        super.onResume()
        if(OpenCVLoader.initDebug()) {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }else{
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,applicationContext, baseLoaderCallback)
        }
    }

    private fun getData() {
        scope.launch {
            val response = RetrofitInstance.authApi.flightTest()
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "Hi! Everything is up and running", Toast.LENGTH_LONG).show()
                    checkToMoveToNewActivity()
                } else {
                    Toast.makeText(applicationContext, "Sorry we are offline", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkToMoveToNewActivity() {
        val studentId = Utils.getValueFromSharedPreferences(this, Constants.STUDENT_ID_KEY)
        if (!studentId.isNullOrEmpty() && studentId.isNotBlank()) {
            Constants.studentId = studentId.toInt()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}