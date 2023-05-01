package com.example.presentmaam.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.presentmaam.databinding.LayoutAttendanceBinding
import com.example.presentmaam.models.Attendance
import com.example.presentmaam.utils.Utils

class AttendanceAdapter(private val attendanceItems: List<Attendance>, private val itemClickListener: OnItemClickListener) : Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    inner class AttendanceViewHolder(val binding: LayoutAttendanceBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = LayoutAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AttendanceViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return attendanceItems.size
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        with(holder) {
            with(attendanceItems[position]) {
                binding.attendanceId.text = this.attendanceId.toString()
                binding.attendanceBatch.text = this.classBatch
                binding.attendanceDept.text = this.classDept
                binding.attendanceSubject.text = this.classSubject
                binding.attendanceDivision.text = this.classDivision.toString()
                binding.attendanceYear.text = this.classYear
                binding.attendanceStart.text = Utils.getDate(this.startTime, "dd/MM/yyyy hh:mm")
                binding.attendanceDuration.text = Utils.getMinutes(this.duration)
            }
        }
        holder.itemView.setOnClickListener {
            itemClickListener.onItemClicked(position)
        }
    }

    interface OnItemClickListener {
        fun onItemClicked(position: Int)
    }
}