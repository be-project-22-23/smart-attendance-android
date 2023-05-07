package com.example.presentmaam.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.presentmaam.LoginActivity
import com.example.presentmaam.R
import com.example.presentmaam.databinding.FragmentProfileBinding
import com.example.presentmaam.utils.Constants
import com.example.presentmaam.utils.Utils

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(Constants.student!!) {
            binding.name.text = "Name: ${this.name}"
            binding.batch.text = "Batch: ${this.batch}"
            binding.rollNo.text = "RollNo. ${this.rollNo}"
            binding.dept.text = "Department: ${this.department}"
            binding.division.text = "Division: ${this.division}"
            binding.email.text = "Email: ${this.email}"
            binding.year.text = "Current Year: ${this.currentYear}"
            binding.phoneNumber.text = "Phone Number: ${this.phoneNumber}"
            Glide.with(requireContext())
                .load(this.photoUrl?.split(" ")?.get(0))
                .placeholder(R.drawable.baseline_person_24)
                .into(binding.profilePhoto)
        }

        binding.logout.setOnClickListener {
            Utils.removeValueFromSharedPreferences(requireContext(), Constants.STUDENT_ID_KEY)
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}