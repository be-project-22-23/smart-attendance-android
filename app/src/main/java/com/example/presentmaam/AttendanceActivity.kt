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
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody.Part.*
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvException
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Point
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
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission")
class AttendanceActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private val TAG = "AttendanceActivity"
    private lateinit var binding: ActivityAttendanceBinding
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var currentLocation: Location? = null
    private var state: State = State.LOCATION
    private var mat1: Mat? = null
    private var faceMat: Mat? = null
    private var cascadeClassifier: CascadeClassifier? = null
    private var isFront = 1
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initFaceDetector()
        setLoading(View.VISIBLE)
        handleStateChange()
        binding.flipCamera.setOnClickListener {
            if (faceMat != null) {
                faceMat = adjustBrightness(faceMat!!)
                val combinedBitmap = convertMatToBitMap(faceMat!!)
                setFaceView(false)
                setLoading(View.VISIBLE)
                verifyFace(combinedBitmap)
            }
        }
        binding.success.setOnClickListener {
            finish()
        }
    }

    private fun initFaceDetector() {
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
    }

    private fun verifyFace(bitmap: Bitmap?) {
        val profileUrl = Constants.student?.photoUrl
        if (bitmap == null || profileUrl == null) {
            return
        }
        val storage = Firebase.storage
        val storageRef = storage.reference
        val fileName = "student/${UUID.randomUUID()}.jpg"
        val faceImage = storageRef.child(fileName)

        val byteOutputStream = ByteArrayOutputStream()
        bitmap.getResizedBitmap(250)?.compress(Bitmap.CompressFormat.JPEG, 70, byteOutputStream)
        val data = byteOutputStream.toByteArray()
        val uploadTask = faceImage.putBytes(data)

        uploadTask
            .addOnSuccessListener { // Image uploaded successfully
                faceImage.downloadUrl.addOnSuccessListener { uri ->
                    Log.d(TAG , uri.toString())
                    Toast.makeText(this@AttendanceActivity, "Image Uploaded!!", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        val body = HashMap<String, String>()
                        body["profileUrl"] = profileUrl
                        body["faceUrl"] = uri.toString()
                        val response = RetrofitInstance.attendanceApi.verifyFace(body)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                state = State.FINAL
                                handleStateChange()
                            } else {
                                Toast.makeText(this@AttendanceActivity, "Unable to verify face! PLease try again or contact your teacher", Toast.LENGTH_LONG).show()
                                finish()
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e -> // Error, Image not uploaded
                Toast.makeText(this@AttendanceActivity, "Failed " + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleStateChange() {
        if (state == State.LOCATION) {
            setLocationView(View.VISIBLE)
            verifyLocation()
        } else if (state == State.FACE) {
            setLocationView(View.GONE)
            setFaceView(true)
        } else {
            setLocationView(View.GONE)
            setFaceView(false)
            markAttendance()
        }
    }

    private fun markAttendance() {
        scope.launch {
            val body = HashMap<String, String>()
            body["attendanceId"] = Constants.attendance?.attendanceId.toString()
            body["studentId"] = Constants.student?.studentId.toString()
            val response = RetrofitInstance.attendanceApi.markAttendance(body)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@AttendanceActivity,
                        "Attendance Marked Successfully",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.success.visibility = View.VISIBLE
                } else {
                    Toast.makeText(applicationContext, response.body()?.message ?: "Too late to mark Attendance!", Toast.LENGTH_LONG).show()
                    finish()
                }
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
        mat1?.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat? {
        mat1 = inputFrame?.rgba()
        mat1?.let {
            val facesMat = extractFaces(it)
            if (facesMat.toArray().isNotEmpty()) {
                cropAndSave(facesMat, it.clone())
                addRectangle(facesMat, it)
            }
        }

        if(isFront == 1) {
            Core.rotate(mat1, mat1, Core.ROTATE_180)
            Core.flip(mat1, mat1,0)
        }
        return mat1 ?: inputFrame?.rgba()
    }

    private fun addRectangle(faces: MatOfRect, mat1: Mat) {
        if (faces.empty()) {
            return
        }
        val face = faces.toArray()[0]
        val point1 = face.tl()
        val point2 = face.br()
        point1.y += 70.0
        point2.y -= 70.0
        // Ensure that the modified points are within the bounds of the input mat1
        point1.x = 0.0.coerceAtLeast(point1.x)
        point1.y = 0.0.coerceAtLeast(point1.y)
        point2.x = point2.x.coerceAtMost(mat1.cols().toDouble() - 1)
        point2.y = point2.y.coerceAtMost(mat1.rows().toDouble() - 1)
        Imgproc.rectangle(mat1, point1, point2, Scalar(255.0, 0.0, 0.0), 2)
    }

    private fun extractFaces(mat: Mat): MatOfRect {
        Core.rotate(mat, mat, Core.ROTATE_90_COUNTERCLOCKWISE)
        if (cascadeClassifier == null) {
            return MatOfRect()
        }
        val faces = MatOfRect()
        cascadeClassifier?.detectMultiScale(mat, faces, 2.0, 1, Objdetect.CASCADE_FIND_BIGGEST_OBJECT or Objdetect.CASCADE_SCALE_IMAGE, Size(500.0, 500.0), Size(900.0, 900.0))
        Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE)
        return faces
    }

    private fun cropAndSave(faces: MatOfRect, mat: Mat) {
        Core.rotate(mat, mat, Core.ROTATE_180)
        Core.flip(mat, mat, 0)
        val arrayOfFaces = faces.toArray()
        if (arrayOfFaces.isNotEmpty()) {
            val face = arrayOfFaces[0]
            val point1 = face.tl()
            val point2 = face.br()
            point1.y += 70.0
            point2.y -= 70.0
            // Ensure that the modified points are within the bounds of the input mat1
            point1.x = 0.0.coerceAtLeast(point1.x)
            point1.y = 0.0.coerceAtLeast(point1.y)
            point2.x = point2.x.coerceAtMost(mat.cols().toDouble() - 1)
            point2.y = point2.y.coerceAtMost(mat.rows().toDouble() - 1)

            // Ensure that the ROI is within the dimensions of the input Mat
            val roi = Rect(point1.x.toInt(), point1.y.toInt(), (point2.x - point1.x + 1).toInt(), (point2.y - point1.y + 1).toInt())
            roi.x = 0.coerceAtLeast(roi.x)
            roi.y = 0.coerceAtLeast(roi.y)
            roi.width = roi.width.coerceAtMost(mat.cols() - roi.x)
            roi.height = roi.height.coerceAtMost(mat.rows() - roi.y)

            // Extract the ROI from the input Mat
            val faceMat = mat.submat(roi)
            this.faceMat = faceMat
        }
    }

    private fun convertMatToBitMap(rgb: Mat): Bitmap? {
        var bmp: Bitmap? = null
        try {
            Imgproc.cvtColor(rgb, rgb, Imgproc.COLOR_RGB2GRAY)
            Core.rotate(rgb, rgb, Core.ROTATE_90_CLOCKWISE)
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
        val locationRequestBuilder = LocationRequest.Builder(LocationRequest.PRIORITY_HIGH_ACCURACY, 1000)

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequestBuilder.build())

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        var isLocationCallMade = false
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let {
                    currentLocation = it
                    val latitude = currentLocation?.latitude
                    val longitude = currentLocation?.longitude
                    Log.d(TAG, "$latitude - $longitude")
                    if (!isLocationCallMade) {
                        isLocationCallMade = true
                        scope.launch {
                            val body = HashMap<String, String>()
                            body["latitude"] = latitude.toString()
                            body["longitude"] = longitude.toString()
                            body["attendanceId"] =
                                Constants.attendance?.attendanceId.toString()
                            val locationResponse =
                                RetrofitInstance.attendanceApi.verifyLocation(body)
                            if (locationResponse.isSuccessful) {
                                withContext(Dispatchers.Main) {
                                    removeListeners()
                                    state = State.FACE
                                    setLoading(View.GONE)
                                    handleStateChange()
                                }
                            } else {
                                isLocationCallMade = false
                            }
                        }
                    }
                } ?: {
                    Log.d(TAG, "Location information isn't available.")
                }
            }
        }
        task.addOnSuccessListener {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient?.requestLocationUpdates(
                locationRequestBuilder.build(),
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@AttendanceActivity, 123)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun removeListeners() {
        val removeTask = locationCallback?.let {
            fusedLocationProviderClient?.removeLocationUpdates(
                it
            )
        }
        removeTask?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Location Callback removed.")
            } else {
                Log.d(TAG, "Failed to remove Location Callback.")
            }
        }
    }

    private fun adjustBrightness(inputMat: Mat): Mat {
        val grayMat = Mat()
        val hsvMat = Mat()
        val hsvChannels = ArrayList<Mat>()
        val outputMat = inputMat.clone()
        val sumPixels = inputMat.width() * inputMat.height()
        val brightnessHistogram = IntArray(256)
        var cumulativeSum = 0
        var minBrightness = 0

        // Convert the input Mat to grayscale
        Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Convert the grayscale Mat to HSV
        Imgproc.cvtColor(inputMat, hsvMat, Imgproc.COLOR_BGR2HSV)

        // Split the HSV Mat into its channels
        Core.split(hsvMat, hsvChannels)

        // Calculate the brightness histogram
        for (i in 0 until grayMat.rows()) {
            for (j in 0 until grayMat.cols()) {
                val brightness = grayMat.get(i, j)[0].toInt()
                brightnessHistogram[brightness]++
            }
        }

        // Find the cumulative sum of the brightness histogram
        for (i in 0 until brightnessHistogram.size) {
            cumulativeSum += brightnessHistogram[i]
            if (cumulativeSum * 100 / sumPixels > 0.1) {
                minBrightness = i
                break
            }
        }

        // Calculate the brightness scaling factor
        val scalingFactor = (255 - minBrightness) / 255.0

        // Adjust the brightness of the dark pixels
        for (i in 0 until hsvChannels[2].rows()) {
            for (j in 0 until hsvChannels[2].cols()) {
                val brightness = hsvChannels[2].get(i, j)[0].toInt()
                if (brightness < minBrightness) {
                    hsvChannels[2].put(i, j, brightness * scalingFactor)
                }
            }
        }

        // Merge the HSV channels
        Core.merge(hsvChannels, hsvMat)

        // Convert the HSV Mat to BGR
        Imgproc.cvtColor(hsvMat, outputMat, Imgproc.COLOR_HSV2BGR)

        return outputMat
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.cameraView.disableView()
        binding.cameraView.requestLayout()
        cascadeClassifier = null
        removeListeners()
        locationCallback?.let { fusedLocationProviderClient?.removeLocationUpdates(it) }
        currentLocation = null
        scope.cancel()
    }

    private enum class State {
        LOCATION,
        FACE,
        FINAL
    }
}