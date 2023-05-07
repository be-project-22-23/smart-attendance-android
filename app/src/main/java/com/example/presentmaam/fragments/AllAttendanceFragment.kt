package com.example.presentmaam.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.presentmaam.AttendanceActivity
import com.example.presentmaam.adapter.AttendanceAdapter
import com.example.presentmaam.databinding.FragmentAllAttendanceBinding
import com.example.presentmaam.models.Attendance
import com.example.presentmaam.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.util.concurrent.CancellationException

class AllAttendanceFragment : Fragment(), AttendanceAdapter.OnItemClickListener {

    private lateinit var binding: FragmentAllAttendanceBinding
    private var feedList: List<Attendance> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAllAttendanceBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedList = Constants.allAttendance?.toMutableList() ?: run {
            ArrayList()
        }
        if (feedList.isEmpty()) {
            binding.allAttendance.visibility = View.GONE
            binding.noAttendance.visibility = View.VISIBLE
        } else {
            val adapter = AttendanceAdapter(feedList, this)
            binding.allAttendance.adapter = adapter
            binding.allAttendance.setHasFixedSize(true)
            val layoutManager = LinearLayoutManager(requireContext())
            binding.allAttendance.layoutManager = layoutManager
            val divider = DividerItemDecoration(requireContext(), layoutManager.orientation)
            binding.allAttendance.addItemDecoration(divider)
            binding.noAttendance.visibility = View.GONE
        }
    }

    override fun onItemClicked(position: Int) {
        Constants.attendance = Constants.allAttendance?.get(position)
        val intent = Intent(requireActivity(), AttendanceActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        feedList = emptyList()
        binding.allAttendance.adapter = null
    }
}