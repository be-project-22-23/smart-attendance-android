package com.example.presentmaam

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.presentmaam.databinding.ActivityLoginBinding
import com.example.presentmaam.fragments.LoginFragment
import com.example.presentmaam.utils.Constants
import com.example.presentmaam.utils.Utils
import com.google.firebase.FirebaseApp
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val baseLoaderCallback = object : BaseLoaderCallback(this) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(applicationContext)

        getData()

        handleBackPressed()

        getPermissions()

        val studentId = Utils.getValueFromSharedPreferences(this, Constants.STUDENT_ID_KEY)
        if (!studentId.isNullOrEmpty() && studentId.isNotBlank()) {
            Constants.studentId = studentId.toInt()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getPermissions(){
        if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
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

    override fun onStart() {
        super.onStart()

        val loginFragment = LoginFragment.getInstance(supportFragmentManager)
        val count = supportFragmentManager.backStackEntryCount

        if (!supportFragmentManager.isDestroyed && count == 0) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.frameLayout, loginFragment)
                commit()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if(OpenCVLoader.initDebug()) {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }else{
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this, baseLoaderCallback)
        }
    }

    private fun handleBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                val count = supportFragmentManager.backStackEntryCount
                if (count == 0) {
                    finish()
                } else {
                    supportFragmentManager.popBackStack()
                }
            }
        } else {
            onBackPressedDispatcher.addCallback(this /* lifecycle owner */, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val count = supportFragmentManager.backStackEntryCount
                    if (count == 0) {
                        finish()
                    } else {
                        supportFragmentManager.popBackStack()
                    }
                }
            })
        }
    }

    private fun getData() {
        val mRequestQueue = Volley.newRequestQueue(this)

        val mStringRequest = StringRequest(Request.Method.GET, Constants.BASE_URL,
            {
                Toast.makeText(applicationContext, "Hi! Everything is up and running", Toast.LENGTH_LONG).show()
            }
        ) { Toast.makeText(applicationContext, "Sorry we are offline", Toast.LENGTH_LONG).show() }
        mRequestQueue.add(mStringRequest)
    }
}