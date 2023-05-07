package com.example.presentmaam

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.presentmaam.api.RetrofitInstance
import com.example.presentmaam.databinding.ActivityFaceBinding
import com.example.presentmaam.utils.Constants
import com.example.presentmaam.utils.Utils.getResizedBitmap
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.rectangle
import org.opencv.objdetect.CascadeClassifier
import org.opencv.objdetect.Objdetect
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class FaceActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private val TAG = "FaceActivity"
    private lateinit var binding: ActivityFaceBinding
    private var mat1: Mat? = null
    private var faceMat: Mat? = null
    private var cascadeClassifier: CascadeClassifier? = null
    private var isFront = 1
    private var isInstructionShown = false
    private val listOfUrls = StringBuilder()
    private var numberOfFaces = 0
    private lateinit var context: Context
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        context = this
        binding.cameraView.setCvCameraViewListener(this)
        binding.cameraView.setCameraIndex(isFront)
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
        binding.cameraView.setCameraPermissionGranted()
        binding.flipCamera.setOnClickListener {
            if (faceMat != null) {
                faceMat = adjustBrightness(faceMat!!)
                val combinedBitmap = convertMatToBitMap(faceMat!!)
                hideCamera()
                binding.imageView.setImageBitmap(combinedBitmap)
                binding.imageView.visibility = View.VISIBLE
                binding.loading.visibility = View.VISIBLE
                uploadImageToFireStoreStorage(combinedBitmap)
            }
        }
        binding.nextBtn.setOnClickListener{
            isInstructionShown = true
            handleInstructionAndCameraSwitch()
        }
        handleInstructionAndCameraSwitch()
    }

    private fun handleInstructionAndCameraSwitch() {
        if (isInstructionShown) {
            showCameraActivity()
        } else {
            if (numberOfFaces == 5) {
                postData()
                return
            }
            hideCameraActivity()
            numberOfFaces++
        }
    }

    private fun postData() {
         scope.launch {
             Constants.student?.photoUrl = listOfUrls.toString()
             Log.d(TAG, Constants.student.toString())
             if (Constants.student != null) {
                 Log.d(TAG, Constants.student.toString())
                 val response = RetrofitInstance.authApi.registerStudent(Constants.student!!)
                 Log.d(TAG, response.toString())
                 if (response.isSuccessful) {
                     val studentId = response.body()?.studentId
                     if (studentId != null) {
                         com.example.presentmaam.utils.Utils.updateSharedPreferences(
                             this@FaceActivity,
                             Constants.STUDENT_ID_KEY,
                             studentId.toString()
                         )
                         withContext(Dispatchers.Main) {
                             val message = response.body()?.message
                             Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                             val intent = Intent(this@FaceActivity, MainActivity::class.java)
                             intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                             startActivity(intent)
                         }
                     }
                 }
             }
        }
    }

    override fun onResume() {
        super.onResume()
        showCamera()
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
        rectangle(mat1, point1, point2, Scalar(255.0, 0.0, 0.0), 2)
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

    override fun onPause() {
        super.onPause()
        hideCamera()
    }
    private fun showCameraActivity() {
        binding.demoLayout.visibility = View.GONE
        binding.cameraLayout.visibility = View.VISIBLE
        showCamera()
    }

    private fun hideCameraActivity() {
        hideCamera()
        if (numberOfFaces >= Constants.resourceIds.size) {
            handleInstructionAndCameraSwitch()
        }
        binding.loading.visibility = View.GONE
        binding.demoImage.setImageResource(Constants.resourceIds[numberOfFaces])
        binding.demoText.text = Constants.imageStatements[numberOfFaces]
        binding.demoLayout.visibility = View.VISIBLE
        binding.cameraLayout.visibility = View.GONE
    }

    private fun showCamera() {
        binding.loading.visibility = View.GONE
        binding.imageView.visibility = View.GONE
        binding.loading.visibility = View.GONE
        binding.cameraView.enableView()
        binding.cameraView.visibility = View.VISIBLE
        binding.flipCamera.visibility = View.VISIBLE
    }

    private fun hideCamera() {
        binding.loading.visibility = View.VISIBLE
        binding.imageView.visibility = View.VISIBLE
        binding.loading.visibility = View.VISIBLE
        binding.cameraView.disableView()
        binding.cameraView.visibility = View.GONE
        binding.flipCamera.visibility = View.GONE
    }

    private fun uploadImageToFireStoreStorage(bitmap: Bitmap?) {
        if (bitmap != null) {

            val storage = Firebase.storage
            val storageRef = storage.reference
            val fileName = "student/${UUID.randomUUID()}.jpg"
            val faceImage = storageRef.child(fileName)

            val byteOutputStream = ByteArrayOutputStream()
            bitmap.getResizedBitmap(250)?.compress(Bitmap.CompressFormat.JPEG, 90, byteOutputStream)
            val data = byteOutputStream.toByteArray()
            val uploadTask = faceImage.putBytes(data)

            uploadTask
                .addOnSuccessListener { // Image uploaded successfully
                    faceImage.downloadUrl.addOnSuccessListener { uri ->
                        listOfUrls.append("$uri ")
                        Log.d(TAG , uri.toString())
                        Toast
                            .makeText(
                                this@FaceActivity,
                                "Image Uploaded!! Only ${Constants.resourceIds.size - numberOfFaces}",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                        isInstructionShown = false
                        handleInstructionAndCameraSwitch()
                    }
                }.addOnFailureListener { e -> // Error, Image not uploaded
                    Toast
                        .makeText(
                            this@FaceActivity,
                            "Failed " + e.message,
                            Toast.LENGTH_SHORT
                        )
                        .show()
                    isInstructionShown = false
                    handleInstructionAndCameraSwitch()
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
        scope.cancel()
    }
}