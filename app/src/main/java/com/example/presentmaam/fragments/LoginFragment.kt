package com.example.presentmaam.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.example.presentmaam.MainActivity
import com.example.presentmaam.R
import com.example.presentmaam.api.RetrofitInstance
import com.example.presentmaam.databinding.FragmentLoginBinding
import com.example.presentmaam.utils.Constants
import com.example.presentmaam.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var fragmentManager: FragmentManager
    private lateinit var context: Context

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment

        context = requireContext()
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.register.setOnClickListener {
            if (!fragmentManager.isDestroyed) {
                fragmentManager.beginTransaction().apply {
                    replace(R.id.frameLayout, RegistrationFragment.getInstance(fragmentManager))
                    addToBackStack(null)
                    commit()
                }
            }
        }

        binding.login.setOnClickListener {
            val email: String = binding.email.text.toString()
            val password: String = binding.password.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                return@setOnClickListener
            }
            Constants.scope.launch {
                val body = HashMap<String, String>()
                body["email"] = email
                body["password"] = password
                val response = RetrofitInstance.authApi.loginStudent(body)
                if (response.isSuccessful) {
                    val studentId = response.body()?.studentId
                    val context = context
                    if (studentId != null) {
                        withContext(Dispatchers.Main) {
                            Utils.updateSharedPreferences(
                                context,
                                Constants.STUDENT_ID_KEY,
                                studentId.toString()
                            )
                            val stat = Utils.getValueFromSharedPreferences(context, Constants.STUDENT_ID_KEY)
                            Log.d("Ashish", stat.toString())
                            val message = response.body()?.message
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            val intent = Intent(activity, MainActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private var loginFragment: LoginFragment? = null
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param fragmentManager of type FragmentManager
         * @return A new instance of fragment LoginFragment.
         */
        @JvmStatic
        fun getInstance(fragmentManager: FragmentManager) : LoginFragment {
            return loginFragment?.let {
                it.fragmentManager = fragmentManager
                return it
            } ?: run {
                loginFragment = LoginFragment().apply {
                    this.fragmentManager = fragmentManager
                }
                return loginFragment as LoginFragment
            }
        }
    }
}