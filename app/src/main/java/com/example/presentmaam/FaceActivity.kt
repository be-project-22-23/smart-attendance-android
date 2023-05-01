package com.example.presentmaam

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.CameraBridgeViewBase
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
    private lateinit var mat1: Mat
    private var faceMats = ArrayList<Mat>()
    private var cascadeClassifier: CascadeClassifier? = null
    private var isFront = 1
    private var isInstructionShown = false
    private val listOfUrls = StringBuilder()
    private var numberOfFaces = 0
    private lateinit var context: Context

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
            if (faceMats.isNotEmpty()) {
                val combinedBitmap = convertMatToBitMap(faceMats[0])
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
        if (numberOfFaces >= 5) {
            postData()
            return
        }
        if (isInstructionShown) {
            showCameraActivity()
        } else {
            hideCameraActivity()
            numberOfFaces++
        }
    }

    private fun postData() {
         Constants.scope.launch {
             Constants.student?.photoUrl = listOfUrls.toString()
             if (Constants.student != null) {
                 val response = RetrofitInstance.authApi.registerStudent(Constants.student!!)
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
            point1.x += 20.0
            point2.x -= 20.0
            rectangle(mat1, point1,point2, Scalar(255.0,0.0,0.0),2)
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
            point1.x += 20.0
            point2.x -= 20.0
            val rectCrop = Rect(point1.x.toInt(), point1.y.toInt(), (point2.x - point1.x + 1).toInt(), (point2.y - point1.y + 1).toInt())
            val faceMat = mat.submat(rectCrop)
            faceMats.add(faceMat)
        }
    }

    override fun onPause() {
        super.onPause()
        hideCamera()
    }

    private fun convertMatToBitMap(rgb: Mat): Bitmap? {
        var bmp: Bitmap? = null
        try {
            bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888)
            Imgproc.cvtColor(rgb, rgb, Imgproc.COLOR_RGB2GRAY)
            Utils.matToBitmap(rgb, bmp)
        } catch (e: CvException) {
            Log.d(TAG, e.message.toString())
        }
        return bmp
    }

    private fun showCameraActivity() {
        binding.demoLayout.visibility = View.GONE
        binding.cameraLayout.visibility = View.VISIBLE
        showCamera()
    }

    private fun hideCameraActivity() {
        hideCamera()
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
            bitmap.getResizedBitmap(250)?.compress(Bitmap.CompressFormat.JPEG, 70, byteOutputStream)
            val data = byteOutputStream.toByteArray()
            val uploadTask = faceImage.putBytes(data)

            uploadTask
                .addOnSuccessListener { // Image uploaded successfully
                    faceImage.downloadUrl.addOnSuccessListener { uri ->
                        listOfUrls.append(uri.toString())
                        Log.d(TAG , uri.toString())
                        Toast
                            .makeText(
                                this@FaceActivity,
                                "Image Uploaded!!",
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
}