package com.example.collaborativeart

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.collaborativeart.databinding.FragmentNavBarBinding

class NavBarFragment : Fragment() {

    private lateinit var binding : FragmentNavBarBinding
    private val globalViewModel by activityViewModels<GlobalViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = FragmentNavBarBinding.inflate(inflater, container, false)

        binding.navLayoutMap.setOnClickListener {

            globalViewModel.navigateToMap(activity?.supportFragmentManager!!)
//            findNavController().navigate(
//                FragmentLoginDirections.actionFragmentLoginToMapsFragment("User")
//            )
        }

        binding.navLayoutFeed.setOnClickListener {
            globalViewModel.navigateToFeed(activity?.supportFragmentManager!!)
        }

        binding.navLayoutPost.setOnClickListener {
            globalViewModel.navigateToPost(activity?.supportFragmentManager!!)
        }

        binding.navLayoutFavorites.setOnClickListener {
            globalViewModel.navigateToFavorites(activity?.supportFragmentManager!!)
        }

        return binding.root
    }

    companion object {
        fun newInstance() = NavBarFragment()
    }
}