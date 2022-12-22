package com.example.collaborativeart

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import kotlin.properties.Delegates

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    val MIN_ACCURACY = 5f
    val MAX_AGE = Duration.ofMinutes(2).toMillis()
    private val MIN_DISTANCE = 10f
    private val MAX_WAIT_TIME = Duration.ofMinutes(2).toMillis()
    private val POLLING_FREQ = Duration.ofSeconds(10).toMillis()
    /** Public immutable live data for [Status] observers. */
    val statusLiveData: LiveData<LocStatus>
        get() = _statusLiveData

    /**
     * An observable delegate posts a location status update each
     * time the last location changes.
     */
    var lastLocation: Location? by Delegates.observable(null) { _, old, new ->
        if (old != new) {
            // Notify LiveData observers about new location value.
            _statusLiveData.postValue(LocStatus.LastLocation(new))
        }
    }
        private set

    /**
     * An observable delegate that restarts location requests
     * each time the minimum accuracy is changed.
     */
    var minAccuracy by Delegates.observable(MIN_ACCURACY) { _, old, new ->
        if (old != new) {
            // Change in minimum accuracy requires restarting updates.
            log("Requesting location using a new minimum accuracy of $new.")
            requestLastLocation()
        }
    }

    /**
     * An observable delegate that restarts location requests
     * each time the minimum age is changed.
     */
    var maxAge by Delegates.observable(MAX_AGE) { _, old, new ->
        if (old != new) {
            // Change in maximum age requires restarting updates.
            log("Requesting location using a new maximum age of $new.")
            requestLastLocation()
        }
    }

    /**
     * Location extension that determines if the location
     * is within the minimum accuracy distance in meters.
     */
    val Location.isAccurate: Boolean
        get() = accuracy <= minAccuracy

    /**
     * Location extension that determines if the location
     * age is less than the maximum age limit.
     */
    val Location.isStale: Boolean
        get() = age > maxAge

    /** Location extension property that calculates the location age. */
    val Location.age: Long
        get() = System.currentTimeMillis() - time

    /**
     * An observable delegate that notifies LiveData observers each time
     * location updates are started and when the are cancelled.
     */
    var isRequestingUpdates by Delegates.observable(false) { _, old, new ->
        if (old != new) {
            _statusLiveData.postValue(
                if (new) LocStatus.Started else LocStatus.Stopped
            )
        }
    }
        private set

    /**
     * Sealed class used to send session updates to LiveData observers.
     */
    sealed class LocStatus {
        object Started : LocStatus()
        object Stopped : LocStatus()
        object Permission : LocStatus()
        data class LastLocation(val location: Location?) : LocStatus()
        data class Error(val errorResId: Int, val e: Exception? = null) : LocStatus()
    }

    /**
     * Private Status MutableLiveDate for posting to observers.
     */
    private var _statusLiveData = MutableLiveData<LocStatus>()

    /** Convenient Context access */
    private val appContext: Context
        get() = getApplication()

    private val locationManager by lazy {
        requireNotNull(application.getSystemService<LocationManager>())
    }

    private var locationListener = object : LocationListener {
        // Called back when location changes
        override fun onLocationChanged(location: Location) {
            log("Received a location update form ${location.provider.uppercase()} provider.")
            location.log()

            // Determine whether new location is
            // better than the last known location.
            lastLocation.let { lastLocation ->
                if (lastLocation == null || location.accuracy <= lastLocation.accuracy) {
                    // Keep track of this new better location estimate.
                    this@LocationViewModel.lastLocation = location

                    // Update display
                    _statusLiveData.postValue(LocStatus.LastLocation(location))

                    // If location is accurate enough stop listening
                    if (location.isAccurate) {
                        cancelUpdates(
                            "Cancelling location updates - accuracy achieved."
                        )
                    }
                }
            }
        }


    }

    /**
     * Checks if user has removed the required permissions
     * and if so a Status.Permission update will be posted
     * to LiveData observers.
     */
    private val havePermission: Boolean
        get() = appContext.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /** Keeps track of currently running coroutine countdown timer. */
    private var cancelJob: Job? = null

    /**
     * Attempts to get last known location and if successful
     * posts that location to LiveData observers. If this
     * last know location is not accurate enough or is stale,
     * then location update listeners are installed to try
     * to acquire a better location result.
     */
    fun requestLastLocation() {
        // Always clear log and last know location before
        // starting a new location request operation.
        cancelUpdates()
        lastLocation = null

        viewModelScope.launch(Dispatchers.IO) {
            lastLocation = getBestLastKnownLocation()?.also {
                _statusLiveData.postValue(LocStatus.LastLocation(it))
            }.also {
                // If the no location was available or if that
                // reading is inaccurate or too old, then register
                // for location updates for a better result.
                if (it == null || !it.isAccurate || it.isStale) {
                    if (!havePermission) {
                        _statusLiveData.postValue(LocStatus.Permission)
                    } else {
                        startLocationUpdates()
                    }
                }
            }
        }
    }

    /**
     * Cancel updates when view model is being destroyed.
     */
    override fun onCleared() {
        cancelUpdates()
        super.onCleared()
    }

    /**
     * Installs a location update listener for NETWORK and GPS
     * providers and also starts a coroutine timer to remove the
     * listener after * [MAX_WAIT_TIME] milliseconds.
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        cancelUpdates("Restarting updates")

        // Register for network location updates
        listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER
        ).forEach { provider ->
            locationManager.getProviderProperties(provider)?.let {
                log("Requesting location updates from ${provider.uppercase()} provider.")
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    POLLING_FREQ,
                    MIN_DISTANCE,
                    locationListener,
                    Looper.getMainLooper()
                )
                isRequestingUpdates = true
            }
        }

        if (isRequestingUpdates) {
            // Start a coroutine timeout that will remove
            // location listeners after MAX_WAIT_TIME elapses.
            cancelJob = viewModelScope.launch {
                delay(MAX_WAIT_TIME)
                cancelUpdates(
                    "Cancelling location updates - wait time expired."
                )
            }
        }
    }

    /**
     * Get the last known location from all providers return best
     * reading that is as accurate as minAccuracy and was taken no
     * longer then minAge milliseconds ago. Returns null if no
     * location is found or if permissions have not been granted
     * or have been revoked.
     */
    @SuppressLint("MissingPermission")
    private fun getBestLastKnownLocation(): Location? {
        var best: Location? = null

        // Cycle through all providers.
        locationManager.allProviders.forEach { provider ->
            if (!havePermission) {
                _statusLiveData.postValue(LocStatus.Permission)
                return null
            }

            log("Getting last known location from ${provider.uppercase()} provider.")

            locationManager.getLastKnownLocation(provider)?.let { location ->

                log("Got a last known location from ${location.provider.uppercase()} provider.")
                location.log()

                when {
                    // Ignore locations that are not accurate enough.
                    location.accuracy > (best?.accuracy ?: Float.MAX_VALUE) -> {
                        log(
                            "Location is less accurate than best " +
                                    "${requireNotNull(best).provider.uppercase()} location."
                        )
                    }

                    // Ignore locations that have the same accuracy.
                    location.accuracy == best?.accuracy ->
                        log(
                            "Location has same accuracy as best " +
                                    "${best?.provider?.uppercase()} location."
                        )

                    // We have a first or better location so save it.
                    else -> {
                        best?.let {
                            log(
                                "Location has better accuracy than " +
                                        "${it.provider.uppercase()} location."
                            )
                        }

                        // First location or the best location so save it.
                        best = location
                    }
                }
            } ?: run {
                log("${provider.uppercase()} provider has no last known location.")
            }
        }

        // If a location was acquired and it meets the
        // accuracy and age requirements, then return it.
        return when {
            (best?.accuracy ?: Float.MAX_VALUE) > MIN_ACCURACY -> {
                log("Last known location has insufficient accuracy.")
                null
            }

            System.currentTimeMillis() - (best?.time ?: 0L) > MAX_AGE -> {
                log("Last known location is stale.")
                null
            }

            best == null -> {
                log("Unknown last location.")
                null
            }

            else -> {
                log("Location is accurate and current:")
                best?.apply { log() }
            }
        }
    }

    /**
     * Stop all updates.
     */
    fun cancelUpdates(reason: String? = null) {
        if (isRequestingUpdates) {
            locationManager.removeUpdates(locationListener)
            isRequestingUpdates = false
            if (reason != null) {
                log(reason)
            }
            cancelJob?.apply {
                if (isActive) {
                    cancel()
                }
                cancelJob = null
            }
        }
    }

    /**
     * Location extensions function for logging location information.
     */
    private fun Location.log() {
        val timeString =
            SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(Date(time))

        log("     Accuracy: $accuracy")
        log("     Time: $timeString")
        log("     Latitude: $latitude")
        log("     Longitude: $longitude")
        log("     Provider: ${provider.uppercase()}")
    }

    /**
     * Call Logger to route messages to LoggingFragment.
     */
    private fun log(msg: String) {
        println(msg)
    }
}