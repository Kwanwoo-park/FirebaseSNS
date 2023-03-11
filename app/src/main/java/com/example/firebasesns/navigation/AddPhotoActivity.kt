package com.example.firebasesns.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.firebasesns.R
import com.example.firebasesns.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity: AppCompatActivity() {
    private val PICK_IMAGE_FROM_ALBUM = -1
    private lateinit var photoUri: Uri
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var addphoto_image: ImageView
    private lateinit var addphoto_btn_upload: Button
    private lateinit var progress_bar: ProgressBar
    private lateinit var addphoto_edit_explain: EditText

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("pkw", "${result.data?.data}, ${result.resultCode}")
        if (result.resultCode == PICK_IMAGE_FROM_ALBUM) {
            print(result.data?.data)
            photoUri = result.data?.data!!
            addphoto_image.setImageURI(photoUri)
        }
        else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        addphoto_image = findViewById(R.id.addphoto_image)
        addphoto_btn_upload = findViewById(R.id.addphoto_btn_upload)
        progress_bar = findViewById(R.id.progress_bar)
        addphoto_edit_explain = findViewById(R.id.addphoto_edit_explain)

        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"

        val actionLauncher = resultLauncher
        actionLauncher.launch(photoPickerIntent)

        addphoto_image.setOnClickListener {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            actionLauncher.launch(photoPickerIntent)
        }

        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }

    fun contentUpload() {
        progress_bar.visibility = View.VISIBLE

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timestamp + "_.png"
        val storageRef = storage.reference.child("images").child(imageFileName)
        storageRef.putFile(photoUri).addOnSuccessListener { taskSnapshot ->
            progress_bar.visibility = View.GONE

            Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_SHORT).show()
            val uri = taskSnapshot.uploadSessionUri

            val contentDTO = ContentDTO()

            contentDTO.imageUrl = uri!!.toString()
            contentDTO.uid = auth?.currentUser?.uid
            contentDTO.explain = addphoto_edit_explain.text.toString()
            contentDTO.userId = auth?.currentUser?.email
            contentDTO.timestamp = System.currentTimeMillis()

            firestore?.collection("images")?.document()?.set(contentDTO)

            setResult(Activity.RESULT_OK)
            finish()
        }.addOnFailureListener {
            progress_bar.visibility = View.GONE

            Toast.makeText(this, getString(R.string.upload_fail), Toast.LENGTH_SHORT).show()
        }
    }
}