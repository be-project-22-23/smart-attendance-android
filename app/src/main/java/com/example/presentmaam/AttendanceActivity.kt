package com.example.presentmaam

import android.annotation.SuppressLint
import android.content.IntentSender
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.presentmaam.api.RetrofitInstance
import com.example.presentmaam.databinding.ActivityAttendanceBinding
import com.example.presentmaam.utils.Constants
import com.example.presentmaam.utils.Utils.getResizedBitmap
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import okhttp3.MultipartBody.Part.*
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvException
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import org.opencv.objdetect.Objdetect
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission")
class AttendanceActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private val TAG = "AttendanceActivity"
    private lateinit var binding: ActivityAttendanceBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null
    private var state: State = State.LOCATION
    private lateinit var mat1: Mat
    private var faceMats = ArrayList<Mat>()
    private var cascadeClassifier: CascadeClassifier? = null
    private var isFront = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            val inputStream = resources.openRawResource(R.raw.haarcascade_frontalface_alt)
            val cascadeDir = getDir("cascade", MODE_PRIVATE)
            val mCascadeFile = File(cascadeDir, "face_detector.xml")
            val os = FileOutputStream(mCascadeFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                os.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            os.close()
            cascadeClassifier = CascadeClassifier(mCascadeFile.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        setLoading(View.VISIBLE)
        handleStateChange()
        binding.flipCamera.setOnClickListener {
            if (faceMats.isNotEmpty()) {
                var combinedBitmap : Bitmap? = null
                for (faceMat in faceMats) {
                    Core.rotate(faceMat, faceMat, Core.ROTATE_90_CLOCKWISE)
                    Imgproc.cvtColor(faceMat, faceMat, Imgproc.COLOR_RGBA2RGB)
                    val bitmap = convertMatToBitMap(faceMat)
                    combinedBitmap = bitmap
                    break
                }
                setFaceView(false)
                setLoading(View.VISIBLE)
                verifyFace(combinedBitmap)
            }
        }
    }

    private fun verifyFace(bitmap: Bitmap?) {
        if (bitmap == null) {
            return
        }
        var isVerified = false
        Constants.student?.photoUrl?.split(" ")?.forEach {profileUrl ->
            val storage = Firebase.storage
            val storageRef = storage.reference
            val fileName = "student/${UUID.randomUUID()}.jpeg"
            val faceImage = storageRef.child(fileName)

            val byteOutputStream = ByteArrayOutputStream()
            bitmap.getResizedBitmap(250)?.compress(Bitmap.CompressFormat.JPEG, 70, byteOutputStream)
            val data = byteOutputStream.toByteArray()
            val uploadTask = faceImage.putBytes(data)

            uploadTask
                .addOnSuccessListener { // Image uploaded successfully
                    faceImage.downloadUrl.addOnSuccessListener { uri ->
                        Log.d(TAG , uri.toString())
                        Toast
                            .makeText(
                                this@AttendanceActivity,
                                "Image Uploaded!!",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                            Constants.scope.launch {
                                val body = HashMap<String, String>()
                                body["profileUrl"] = profileUrl
                                body["faceUrl"] = uri.toString()
                                val response = RetrofitInstance.attendanceApi.verifyFace(body)
                                if (response.isSuccessful) {
                                    isVerified = true
                                    return@launch
                                }
                            }
                            if (isVerified) {
                                return@addOnSuccessListener
                            }
                    }
                    if (isVerified) {
                        return@addOnSuccessListener
                    }
                }.addOnFailureListener { e -> // Error, Image not uploaded
                    Toast
                        .makeText(
                            this@AttendanceActivity,
                            "Failed " + e.message,
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            if (isVerified) {
                state = State.FINAL
                handleStateChange()
                return@forEach
            }
        }
    }

    private fun handleStateChange() {
        if (state == State.LOCATION) {
            setFaceView(false)
            setLocationView(View.VISIBLE)
            verifyLocation()
        } else if (state == State.FACE) {
            setLocationView(View.GONE)
            setFaceView(true)
        } else {
            setLocationView(View.GONE)
            setFaceView(false)
            setLocationView(View.VISIBLE)
            markAttendance()
        }
    }

    private fun markAttendance() {
        Constants.scope.launch {
            val body = HashMap<String, String>()
            body["attendanceId"] = Constants.attendance?.attendanceId.toString()
            body["studentId"] = Constants.student?.studentId.toString()
            val response = RetrofitInstance.attendanceApi.markAttendance(body)
            if (response.isSuccessful) {
                setLocationView(View.GONE)
                binding.success.visibility = View.VISIBLE
            }
        }
    }

    private fun setFaceView(isVisible: Boolean) {
        if (isVisible) {
            binding.layoutFace.visibility = View.VISIBLE
            binding.cameraView.setCvCameraViewListener(this)
            binding.cameraView.setCameraIndex(isFront)
            binding.cameraView.setCameraPermissionGranted()
            binding.cameraView.enableView()
        } else {
            binding.cameraView.disableView()
            binding.layoutFace.visibility = View.GONE
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mat1 = Mat(width, height, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        mat1.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        mat1 = inputFrame!!.rgba()
        val facesMat : MatOfRect = extractFaces(mat1)
        cropAndSave(facesMat, mat1.clone())
        addRectangle(facesMat, mat1)

        if(isFront == 1) {
            Core.rotate(mat1, mat1, Core.ROTATE_180)
            Core.flip(mat1, mat1,0)
        }
        return mat1
    }

    private fun addRectangle(faces: MatOfRect, mat1: Mat?) {
        for(face in faces.toArray()) {
            val point1 = face.tl()
            val point2 = face.br()
            point1.x -= 40
            point1.y += 20
            point2.x += 10
            point2.y += 40
            Imgproc.rectangle(mat1, point1, point2, Scalar(255.0, 0.0, 0.0), 2)
        }
    }

    private fun extractFaces(mat: Mat): MatOfRect {
        Core.rotate(mat, mat, Core.ROTATE_90_COUNTERCLOCKWISE)
        val faces = MatOfRect()
        cascadeClassifier?.detectMultiScale(mat, faces, 2.0, 1, Objdetect.CASCADE_FIND_BIGGEST_OBJECT or Objdetect.CASCADE_SCALE_IMAGE, Size(300.0, 300.0), Size(900.0, 900.0))
        Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE)
        return faces
    }

    private fun cropAndSave(faces: MatOfRect, mat: Mat) {
        faceMats = ArrayList()
        Core.rotate(mat, mat, Core.ROTATE_180)
        Core.flip(mat, mat, 0)
        for(face in faces.toArray()) {
            val point1 = face.tl()
            val point2 = face.br()
            point1.x -= 40
            point1.y += 20
            point2.x += 10
            point2.y += 40
            val rectCrop = Rect(point1.x.toInt(), point1.y.toInt(), (point2.x - point1.x + 1).toInt(), (point2.y - point1.y + 1).toInt())
            val faceMat = mat.submat(rectCrop)
            faceMats.add(faceMat)
        }
    }

    private fun convertMatToBitMap(rgb: Mat): Bitmap? {
        var bmp: Bitmap? = null
        try {
            bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rgb, bmp)
        } catch (e: CvException) {
            Log.d(TAG, e.message.toString())
        }
        return bmp
    }


    private fun setLoading(visibility: Int) {
        binding.loadingBar.visibility = visibility
    }

    @SuppressLint("SetTextI18n")
    private fun setLocationView(visibility: Int) {
        binding.loadingText.text = "Please wait while we access and verify your location"
        binding.layoutLocation.visibility = visibility
    }

    private fun verifyLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(60))
            .setMaxUpdateDelayMillis(TimeUnit.MINUTES.toMillis(2))
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let {
                    currentLocation = it
                    val latitude = currentLocation?.latitude
                    val longitude = currentLocation?.longitude
                    Log.d(TAG, "$latitude - $longitude")
                    Constants.scope.launch {
                        val body = HashMap<String, String>()
                        body["latitude"] = latitude.toString()
                        body["longitude"] = longitude.toString()
                        body["attendanceId"] = Constants.attendance?.attendanceId.toString()
                        val locationResponse = RetrofitInstance.attendanceApi.verifyLocation(body)
                        if (locationResponse.isSuccessful) {
                            removeListeners()
                            state = State.FACE
                            setLoading(View.GONE)
                            handleStateChange()
                        }
                    }
                } ?: {
                    Log.d(TAG, "Location information isn't available.")
                }
            }
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)

        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
    }

    private fun removeListeners() {
        val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        removeTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Location Callback removed.")
            } else {
                Log.d(TAG, "Failed to remove Location Callback.")
            }
        }
    }

    private enum class State {
        LOCATION,
        FACE,
        FINAL
    }
}