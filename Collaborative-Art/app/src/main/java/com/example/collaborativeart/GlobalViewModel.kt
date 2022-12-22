package com.example.collaborativeart

import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import android.location.LocationListener
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.time.Duration
import kotlin.properties.Delegates
import androidx.core.content.getSystemService
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import androidx.fragment.app.viewModels
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger
import androidx.fragment.app.activityViewModels
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.collections.ArrayList


// This can hold application-wide values to be used across different fragments
// eg. current user object
class GlobalViewModel(application: Application) : AndroidViewModel(application) {


    val defaultUser : User = User("testudo", "password")

    data class Posts(
        val latitude: Double,
        val longitude: Double,
        val user: String,
        val memoir: String
    )



    lateinit var mapFragment : MapsFragment
    lateinit var feedFragment : FeedFragment
    lateinit var postFragment : PostFragment
    lateinit var favoritesFragment : FavoritesFragment
    lateinit var navFragment: NavBarFragment
    lateinit var permFragment: PermissionFragment

    init{
        mapFragment = MapsFragment()
        feedFragment = FeedFragment()
        postFragment = PostFragment()
        favoritesFragment = FavoritesFragment()
        navFragment = NavBarFragment()
        permFragment = PermissionFragment()
    }

    private val _currentUser = MutableLiveData<String>()
    val currentUser : LiveData<String>
        get() = _currentUser

    private val _mainFragment = MutableLiveData<Fragment>()
    val mainFragment : LiveData<Fragment>
        get() = _mainFragment

    private val _oldFragment = MutableLiveData<String>()
    val oldFragment : LiveData<String>
        get() = _oldFragment

    fun startNavBar(fragmentManager: FragmentManager){

        if(fragmentManager == null)
            { throw Exception("Could not navigate - fragmentManager was NULL") }

        fragmentManager.beginTransaction()
            .replace(R.id.navbar_fragment_container, navFragment)
            .commitNow()

        _mainFragment.value = mapFragment

    }

    fun navigateToMap(fragmentManager: FragmentManager){

        if(fragmentManager == null)
            { throw Exception("Could not navigate - fragmentManager was NULL") }

        val transaction = fragmentManager.beginTransaction()

        transaction
            .replace(R.id.main_fragment_container, mapFragment)
            .commitNow()

        _mainFragment.value = mapFragment


        Log.i("PROJECT_TAG", "Navigating to Map")
    }

    fun navigateToFeed(fragmentManager: FragmentManager){

        if(fragmentManager == null)
            { throw Exception("Could not navigate - fragmentManager was NULL") }

        val transaction = fragmentManager.beginTransaction()

        transaction
            .replace(R.id.main_fragment_container, feedFragment)
            .commitNow()

        _mainFragment.value = feedFragment

        Log.i("PROJECT_TAG", "Navigating to Feed")

    }

    fun navigateToPost(fragmentManager: FragmentManager){
        if(fragmentManager == null)
            { throw Exception("Could not navigate - fragmentManager was NULL") }

        val transaction = fragmentManager.beginTransaction()

        transaction
            .replace(R.id.main_fragment_container, postFragment)
            .commitNow()

        _mainFragment.value = postFragment


        Log.i("PROJECT_TAG", "Navigating to Post")


    }

    fun navigateToFavorites(fragmentManager: FragmentManager?){

        if(fragmentManager == null)
            { throw Exception("Could not navigate - fragmentManager was NULL") }

        val transaction = fragmentManager.beginTransaction()

        transaction
            .replace(R.id.main_fragment_container, favoritesFragment)
            .commitNow()

        _mainFragment.value = favoritesFragment

        Log.i("PROJECT_TAG", "Navigating to Favorites")

    }

    fun navigateToPerm(fragmentManager: FragmentManager, old : String){

        if(fragmentManager == null)
        { throw Exception("Could not navigate - fragmentManager was NULL") }

        val transaction = fragmentManager.beginTransaction()

        transaction
            .replace(R.id.main_fragment_container, permFragment)
            .commitNow()

        _mainFragment.value = permFragment
        _oldFragment.value = old

        Log.i("PROJECT_TAG", "Navigating to Permissions")
    }

    fun updateUser(username: String) {
        _currentUser.value = username
    }

}