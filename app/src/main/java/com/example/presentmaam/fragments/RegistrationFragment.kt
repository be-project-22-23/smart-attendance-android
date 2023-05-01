package com.example.presentmaam.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.presentmaam.FaceActivity
import com.example.presentmaam.databinding.FragmentRegistrationBinding
import com.example.presentmaam.models.Student
import com.example.presentmaam.utils.Constants

/**
 * A simple [Fragment] subclass.
 * Use the [RegistrationFragment.getInstance] factory method to
 * create an instance of this fragment.
 */
class RegistrationFragment : Fragment() {

    private lateinit var fragmentManager: FragmentManager
    private lateinit var binding: FragmentRegistrationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentRegistrationBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.login.setOnClickListener {
            if (!fragmentManager.isDestroyed) {
                fragmentManager.popBackStack()
            }
        }

        binding.register.setOnClickListener {
            val name: String = binding.name.text.toString()
            val email: String = binding.email.text.toString()
            val password: String = binding.password.text.toString()
            val cpassword: String = binding.cpassword.text.toString()
            val rollNo: String = binding.rollNo.text.toString()
            val department: String = binding.department.selectedItem.toString()
            val batch: String = binding.batch.selectedItem.toString()
            val phoneNumber: String = binding.phoneNo.text.toString()
            val division: String = binding.division.selectedItem.toString()
            val currentYear: String = binding.currentYear.selectedItem.toString()
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || cpassword.isEmpty() || password != cpassword
                || rollNo.isEmpty() || department.isEmpty() || batch.isEmpty() || phoneNumber.isEmpty() || currentYear.isEmpty()
                || division.isEmpty()) {
                return@setOnClickListener
            }
            val student = Student(name = name, email = email, password = password, cpassword = cpassword,
                rollNo = rollNo, department = department, batch = batch, phoneNumber = phoneNumber,
                currentYear = currentYear, studentId = null, createdAt = null, photoUrl = null, division = division)
            Constants.student = student
            val intent = Intent(activity, FaceActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        private var registrationFragment: RegistrationFragment? = null
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param fragmentManager of type [FragmentManager]
         * @return A new instance of fragment RegistrationFragment.
         */
        @JvmStatic
        fun getInstance(fragmentManager: FragmentManager) : RegistrationFragment {
            return registrationFragment?.let {
                it.fragmentManager = fragmentManager
                return it
            } ?: run {
                registrationFragment = RegistrationFragment().apply {
                    this.fragmentManager = fragmentManager
                }
                return registrationFragment as RegistrationFragment
            }
        }
    }
}