package com.example.presentmaam

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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val studentId = Utils.getValueFromSharedPreferences(this, Constants.STUDENT_ID_KEY)?.toInt()

        if (studentId == null) {
            Toast.makeText(this, "No student found! Please login first", Toast.LENGTH_LONG).show()
            finish()
        }
        Constants.scope.launch {
            val attendanceResponse = RetrofitInstance.getDataApi.getAllAttendance()
            if (attendanceResponse.isSuccessful) {
                val attendanceList = attendanceResponse.body()?.data ?: run {
                    ArrayList()
                }
                Constants.allAttendance = attendanceList
            } else {
                Toast.makeText(this@MainActivity, attendanceResponse.message(),Toast.LENGTH_LONG).show()
                finish()
            }
            val studentResponse = RetrofitInstance.getDataApi.getStudentDetails(studentId.toString())
            if (studentResponse.isSuccessful) {
                val studentDetails = studentResponse.body()?.data ?: run {
                    Student(studentId = 1, name = "test", email = "test@test.com", password = "password", rollNo = "test", department = "Computer Engineering", batch = "A", photoUrl = "https://static.generated.photos/vue-static/face-generator/landing/wall/14.jpg", currentYear = "BE", phoneNumber = "1234567890", division = "A", cpassword = null, createdAt = null)
                }
                Constants.student = studentDetails
            } else {
                Toast.makeText(this@MainActivity, studentResponse.message(),Toast.LENGTH_LONG).show()
                finish()
            }
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
}
