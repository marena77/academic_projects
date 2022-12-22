package com.example.collaborativeart

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.collaborativeart.databinding.FragmentMapsBinding
import java.util.*

import android.Manifest
import android.location.Location
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.core.content.getSystemService
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await


class MapsFragment : Fragment() {


    /** Use navArgs delegate to access user name argument. */
    private val args by navArgs<MapsFragmentArgs>()
    private lateinit var binding: FragmentMapsBinding


    private val gviewModel: GlobalViewModel by activityViewModels()
    private val locationViewModel : LocationViewModel by activityViewModels()
    private lateinit var userDatabase: DatabaseReference

    private var lat = 0.0
    private var long = 0.0

    private lateinit var map: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapsBinding.inflate(inflater, container, false)
        userDatabase =
            FirebaseDatabase.getInstance()
                .getReference()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.map.onCreate(savedInstanceState)
        binding.map.onResume()



        binding.fab.setOnClickListener {

            // Disable button.
            it.isEnabled = false

            // load posts from database
            runBlocking {
                placeMarkers(getPosts())
            }

            it.isEnabled = true
        }
        // Asynchronously load the map.
        binding.map.getMapAsync { map ->

            // Map is now loaded so save it as a property
            // and adjust the camera.
            this.map = map


            // Now that map is loaded, enable the search FAB.
            binding.fab.isEnabled = true

            // Hide progress spinner that is, by default, visible in layout.
            binding.progress.isVisible = false


        }
        setupObserversAndListeners()
        locationViewModel.requestLastLocation()
    }


    private fun placeMarkers(posts: List<GlobalViewModel.Posts>) {
        if (posts.isEmpty()) {
            // Remove all markers.
            map.clear()
        } else {

            map.moveCamera(
                CameraUpdateFactory.newLatLng(LatLng(lat, long))
            )
            posts.forEach {
                // Add a new marker for this earthquake
                map.addMarker(
                    MarkerOptions()
                        // Set the Marker's position
                        .position(LatLng(it.latitude, it.longitude))
                        // Set the title of the Marker's information window
                        .title("${it.user}: " + it.memoir)
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED
                            )
                        )
                        // Set the color for the Marker
                )
                Log.i("********MARKER ADDED**** ", "${it.latitude}, ${it.longitude}")
            }
        }
    }

    private suspend fun getPosts() : ArrayList<GlobalViewModel.Posts> {
        var def_posts: ArrayList<GlobalViewModel.Posts> = arrayListOf()
        val data = userDatabase.child("posts").get().await()
        data.children.forEach {
            it.children.forEach {
                val map =(it.getValue() as HashMap<String, Any>)
                def_posts += (GlobalViewModel.Posts(
                    map.get("lat").toString().toDouble(),
                    map.get("long").toString().toDouble(),
                    map.get("user").toString(),
                    map.get("body").toString()
                ))
            }
        }

        return def_posts
    }

        private fun View.showSnackbar(
            msg: String,
            length: Int,
            actionMessage: CharSequence? = null,
            action: (View) -> Unit = {}
        ) {
            with(Snackbar.make(this, msg, length)) {
                if (actionMessage != null) {
                    setAction(actionMessage) { action(view) }
                }
                show()
            }
        }

    private fun setupObserversAndListeners() {
        // Listen for view model status changes and reflect those changes in the UI.
        locationViewModel.statusLiveData.observe(viewLifecycleOwner) { status ->
            println("Location change was observed!")
            when (status) {
                // User must have revoked permission.
                LocationViewModel.LocStatus.Permission -> gviewModel.navigateToPerm(activity?.supportFragmentManager!!, "Post")


                // Display the new location (possibly null).
                is LocationViewModel.LocStatus.LastLocation -> {
                    if (status.location != null) {
                        lat = status.location!!.latitude
                        long = status.location!!.longitude
                    }
                }

                // Report what when wrong.
                is LocationViewModel.LocStatus.Error -> {
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

    }