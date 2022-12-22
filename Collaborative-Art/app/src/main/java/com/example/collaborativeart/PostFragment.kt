package com.example.collaborativeart

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.icu.text.SimpleDateFormat
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.collaborativeart.databinding.FragmentPostBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import androidx.core.content.getSystemService
import androidx.fragment.app.viewModels
import com.google.firebase.database.FirebaseDatabase
import com.example.collaborativeart.LocationViewModel.LocStatus.*
import androidx.navigation.fragment.findNavController
import java.io.*
import java.util.*


class PostFragment : Fragment() {

    private lateinit var binding: FragmentPostBinding
    private lateinit var postDatabase: DatabaseReference
    private val gviewModel: GlobalViewModel by activityViewModels()
    private val locationViewModel : LocationViewModel by activityViewModels()

    // Storage for camera image URI components
    private val CAPTURED_PHOTO_PATH_KEY = "mCurrentPhotoPath"
    private val CAPTURED_PHOTO_URI_KEY = "mCapturedImageURI"

    // Required for camera operations in order to save the image file on resume.
    private var mCurrentPhotoPath: String? = null
    private var mCapturedImageURI: Uri? = null

    private var lat : Double = 0.0
    private var long : Double = 0.0

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                takePhoto()
            } else {
                Toast.makeText(
                    requireActivity(), getString(R.string.need_permission_string),
                    Toast.LENGTH_SHORT
                ).show()
                relaunchPermissionRequest()
            }
        }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        if (mCurrentPhotoPath != null) {
            savedInstanceState.putString(CAPTURED_PHOTO_PATH_KEY, mCurrentPhotoPath)
        }
        if (mCapturedImageURI != null) {
            savedInstanceState.putString(CAPTURED_PHOTO_URI_KEY, mCapturedImageURI.toString())
        }
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(CAPTURED_PHOTO_PATH_KEY)) {
                mCurrentPhotoPath = savedInstanceState.getString(CAPTURED_PHOTO_PATH_KEY);
            }
            if (savedInstanceState.containsKey(CAPTURED_PHOTO_URI_KEY)) {
                mCapturedImageURI = Uri.parse(savedInstanceState.getString(CAPTURED_PHOTO_URI_KEY));
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPostBinding.inflate(inflater, container, false)
        binding.imageView.setOnClickListener {
            requestPermission()
        }
        binding.button2.setOnClickListener {
            attemptPost()
        }
        return binding.root


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postDatabase =
            FirebaseDatabase.getInstance()
                .getReference("posts")
        setupObserversAndListeners()
        locationViewModel.requestLastLocation()
    }

    private fun requestPermission() {
        val granted =
            ContextCompat.checkSelfPermission(requireContext(), Companion.permish) ==
                    PackageManager.PERMISSION_GRANTED
        when {
            granted -> {
                takePhoto()
            }
            shouldShowRequestPermissionRationale(Companion.permish) -> {
                binding.root.showSnackbar(
                    R.string.need_permission_string,
                    Snackbar.LENGTH_INDEFINITE,
                    android.R.string.ok
                ) {
                    requestPermissionLauncher.launch(Companion.permish)
                }
            }
            // User hasn't been asked for permission yet.
            else -> {
                // TODO: launch the permission request launcher.
                requestPermissionLauncher.launch(Companion.permish)
            }
        }
    }

    private fun takePhoto() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile: File? = try {
            createImageFileInAppDir()
        } catch (ex: IOException) {
            // Error occurred while creating the File
            null
        }

        photoFile?.also { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                requireActivity(),
                BuildConfig.APPLICATION_ID + ".provider",
                file
            )
            mCapturedImageURI = photoURI

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }
        if (photoFile != null) {
            mCurrentPhotoPath = photoFile.path
        }
        startActivityForResult(cameraIntent, 200)
    }

    private fun setupObserversAndListeners() {
        // Listen for view model status changes and reflect those changes in the UI.
        locationViewModel.statusLiveData.observe(viewLifecycleOwner) { status ->
            println("Location change was observed!")
            when (status) {
                // User must have revoked permission.
                Permission -> gviewModel.navigateToPerm(activity?.supportFragmentManager!!, "Post")


                // Display the new location (possibly null).
                is LastLocation -> {
                    if (status.location != null) {
                        lat = status.location!!.latitude
                        long = status.location!!.longitude
                    }
                }

                // Report what when wrong.
                is Error -> {
                    binding.root.showSnackbar(
                        getString(status.errorResId),
                        Snackbar.LENGTH_INDEFINITE
                    )
                }
                else -> {
                    println("Got started or stopped for location status")
                }
            }
        }
    }


    private fun attemptPost() {
        if (mCapturedImageURI != null && binding.memoir.text.isNotBlank()) {
            // first get permission for location data


            // TODO
            // This will have to change to current user at some point
            val bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(File(mCurrentPhotoPath)))
            val post = Post(gviewModel.currentUser.value!!.toString(), lat, long, binding.memoir.text.toString())

            // Here I create post child and put post metadata
            val postRef = postDatabase.child(gviewModel.currentUser.value!!)
            val id = postRef.push().key.toString()
            postRef.child(id).setValue(post)

            // Now to upload image as a child
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val d = Base64.getEncoder().encodeToString(baos.toByteArray())
            val imageId = postRef.child(id).push().key.toString()
            postRef.child(id).child(imageId).setValue(d)

            // clear post fields
            binding.memoir.text.clear()
            binding.imageView.setImageResource(android.R.drawable.ic_menu_camera)

        } else {
            Toast.makeText(
                requireActivity(), "Fill out the fields first!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 200 && data != null) {
            binding.imageView.setImageURI(mCapturedImageURI)
        }
    }

    @Throws(IOException::class)
    private fun createImageFileInAppDir(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imagePath = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return   File(imagePath, "BMP_${timeStamp}_" + ".bmp")
    }

    private fun relaunchPermissionRequest() {
        requestPermissionLauncher.launch(Companion.permish)
    }


    companion object {
        val permish = android.Manifest.permission.CAMERA;
    }

}