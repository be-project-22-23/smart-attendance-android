package com.example.presentmaam

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.presentmaam.api.RetrofitInstance
import com.example.presentmaam.fragments.AllAttendanceFragment
import com.example.presentmaam.fragments.ProfileFragment
import com.example.presentmaam.fragments.ScanFragment
import com.example.presentmaam.models.Student
import com.example.presentmaam.utils.Constants
import com.example.presentmaam.utils.Utils
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val studentId = Utils.getValueFromSharedPreferences(this, Constants.STUDENT_ID_KEY)?.toInt()

        if (studentId == null) {
            Toast.makeText(this, "No student found! Please login first", Toast.LENGTH_LONG).show()
            finish()
        }
        scope.launch {
            val studentResponse = RetrofitInstance.getDataApi.getStudentDetails(studentId.toString())
            if (studentResponse.isSuccessful) {
                val studentDetails = studentResponse.body()?.data ?: run {
                    Student(studentId = 1, name = "test", email = "test@test.com", password = "password", rollNo = "test", department = "Computer Engineering", batch = "A", photoUrl = "https://static.generated.photos/vue-static/face-generator/landing/wall/14.jpg", currentYear = "BE", phoneNumber = "1234567890", division = "A", cpassword = null, createdAt = null)
                }
                withContext(Dispatchers.Main) {
                    Constants.student = studentDetails
                }
                val attendanceResponse = RetrofitInstance.getDataApi.getAllAttendance(
                    studentDetails.department, studentDetails.currentYear, studentDetails.division)
                withContext(Dispatchers.Main) {
                    if (attendanceResponse.isSuccessful) {
                        val attendanceList = attendanceResponse.body()?.data ?: run {
                            ArrayList()
                        }
                        Constants.allAttendance = attendanceList
                        handleStateChange()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            attendanceResponse.message(),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            } else {
                Toast.makeText(this@MainActivity, studentResponse.message(),Toast.LENGTH_LONG).show()
                finish()
            }
            val attendanceCount = RetrofitInstance.getDataApi.getAttendanceCount(studentId.toString())
            withContext(Dispatchers.Main) {
                if (attendanceCount.isSuccessful) {
                    Constants.attendanceCount = attendanceCount.body()
                    handleStateChange()
                } else {
                    Toast.makeText(this@MainActivity, attendanceCount.message(), Toast.LENGTH_LONG)
                        .show()
                    finish()
                }
            }
        }

        getPermissions()
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

    private fun handleStateChange() {
        if (Constants.student == null || Constants.allAttendance == null || Constants.attendanceCount == null) {
            return
        }
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener {
            lateinit var selectedFragment: Fragment
            when (it.itemId) {
                R.id.allAttendance -> {
                    selectedFragment = AllAttendanceFragment()
                }
                R.id.scan -> {
                    selectedFragment = ScanFragment()
                }
                R.id.profile -> {
                    selectedFragment = ProfileFragment()
                }
            }
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, selectedFragment).commit()
            true
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, AllAttendanceFragment()).commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        Constants.allAttendance = null
        Constants.student = null
        scope.cancel()
    }
}
