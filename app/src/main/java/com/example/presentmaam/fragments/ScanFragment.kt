package com.example.presentmaam.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.example.presentmaam.AttendanceActivity
import com.example.presentmaam.api.RetrofitInstance
import com.example.presentmaam.databinding.FragmentScanBinding
import com.example.presentmaam.utils.Constants
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.CancellationException

class ScanFragment : Fragment() {

    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var cameraSource: CameraSource
    private lateinit var callback: SurfaceHolder.Callback
    private lateinit var binding: FragmentScanBinding
    private var isRequestMade = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentScanBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        barcodeDetector = BarcodeDetector.Builder(requireContext())
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()

        cameraSource = CameraSource.Builder(requireContext(), barcodeDetector)
            .setRequestedPreviewSize(640, 480)
            .setAutoFocusEnabled(true)
            .build()

        callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            android.Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        cameraSource.start(binding.surfaceView.holder)
                    } else {
                        requestPermissions(
                            arrayOf(android.Manifest.permission.CAMERA),
                            100
                        )
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        }

        binding.surfaceView.holder.addCallback(callback)

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                if (isRequestMade) {
                    return
                }
                val qrCodes = detections.detectedItems
                if (qrCodes.size() != 0) {
                    val qrCode = qrCodes.valueAt(0)
                    val value = qrCode.rawValue
                    Constants.attendance = Constants.allAttendance?.first {
                        it.attendanceId == value.toInt()
                    }
                    if (Constants.attendance == null) {
                        return
                    }
                    isRequestMade = true
                    Log.d("ScanFragment", "QR code value: ${qrCode.rawValue} and corresponding attendance is ${Constants.attendance}")
                    barcodeDetector.release()
                    cameraSource.stop()
                    cameraSource.release()
                    binding.surfaceView.holder.removeCallback(callback)
                    val intent = Intent(requireActivity(), AttendanceActivity::class.java)
                    startActivity(intent)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeDetector.release()
        cameraSource.stop()
        cameraSource.release()
        binding.surfaceView.holder.removeCallback(callback)
    }
}