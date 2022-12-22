package com.example.collaborativeart

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.collaborativeart.databinding.FragmentLoginBinding
import com.example.collaborativeart.databinding.MainActivityBinding
import com.google.firebase.provider.FirebaseInitProvider
import androidx.lifecycle.*
import com.google.android.material.snackbar.Snackbar

// This class probably shouldn't change very much, if at all
class MainActivity : AppCompatActivity() {

    private lateinit var binding : MainActivityBinding
    private lateinit var globalViewModel: GlobalViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseInitProvider()

        binding = MainActivityBinding.inflate(layoutInflater)

        globalViewModel = ViewModelProvider(this)[GlobalViewModel::class.java]


        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()

    }




}