package com.example.collaborativeart

import android.app.AlertDialog
import android.content.DialogInterface
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.collaborativeart.databinding.FragmentFeedBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class FeedFragment : Fragment() {

    companion object {
        fun newInstance() = FeedFragment()
    }

    private lateinit var binding : FragmentFeedBinding
    private lateinit var userDatabase: DatabaseReference

    private lateinit var feedPostAdapter : FeedPostAdapter

    private val globalViewModel by activityViewModels<GlobalViewModel>()
    //private lateinit var feedViewModel : FeedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentFeedBinding.inflate(inflater, container, false)
        //feedViewModel = ViewModelProvider(this)[FeedViewModel::class.java]

        userDatabase =
            FirebaseDatabase.getInstance()
                .reference

        feedPostAdapter = FeedPostAdapter(globalViewModel)
        val recyclerView: RecyclerView = binding.feedRecyclerView

        recyclerView.adapter = feedPostAdapter

        addPosts()

        binding.addFriend.setOnClickListener {
            addFriendAlert()
        }

        binding.removeFriend.setOnClickListener {
            removeFriendAlert()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    private fun addFriendAlert() {
        var ad = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.hint = "Type friend's username to add here!"
        input.inputType = InputType.TYPE_CLASS_TEXT
        ad.setView(input)

        ad.setPositiveButton("Ok") { dialog, which ->
            addFriend(input.text.toString())
        }
        ad.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        ad.show()

    }

    private fun removeFriendAlert() {
        var ad = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.hint = "Type friend's username to remove here!"
        input.inputType = InputType.TYPE_CLASS_TEXT
        ad.setView(input)

        ad.setPositiveButton("Ok") { dialog, which ->
            removeFriend(input.text.toString())
        }
        ad.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        ad.show()
    }

    private fun addFriend(username: String) {
        userDatabase.child("users").get().addOnSuccessListener {
            userDatabase.child("users").child(username).get().addOnSuccessListener {
                if (it.value == null) {
                    Toast.makeText(
                        requireContext(),
                        "That user doesn't exist!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    userDatabase.child("users").child(globalViewModel.currentUser.value!!).child("friends").get().addOnSuccessListener {
                        // it is the old friends list. I update it and then set a new value.
                        var oldList = it.value as List<String>
                        userDatabase.child("users").child(globalViewModel.currentUser.value!!).child("friends").setValue(oldList + mutableListOf(username))
                    }
                }
            }
        }
    }

    private fun removeFriend(username: String) {
        userDatabase.child("users").get().addOnSuccessListener {
            userDatabase.child("users").child(username).get().addOnSuccessListener {
                if (it.value == null) {
                    Toast.makeText(
                        requireContext(),
                        "That user doesn't exist!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    userDatabase.child("users").child(globalViewModel.currentUser.value!!).child("friends").get().addOnSuccessListener {
                        // it is the old friends list. I update it and then set a new value.
                        var oldList = it.value as List<String>
                        var newList = mutableListOf<String>()
                        oldList.forEach {
                            if (!it.equals(username)) {
                                newList.add(it)
                            }
                        }
                        userDatabase.child("users").child(globalViewModel.currentUser.value!!).child("friends").setValue(newList)
                    }
                }
            }
        }
    }

    private fun addPosts() {
        var dummy_list = mutableListOf<FeedPost>()
        runBlocking {
            val all_posts = userDatabase.child("posts").get().await()
            val friends = userDatabase.child("users").child(globalViewModel.currentUser.value!!).child("friends").get().await().getValue() as List<String>
            // for all posts
            all_posts.children.forEach {
                // if the poster is in our friends list
                if (friends.contains(it.key!!)) {
                    it.children.forEach {
                        var post = (it.getValue() as HashMap<String, Any>)
                        var image = post.get(post.keys.elementAt(0))
                        dummy_list.add(FeedPost(post.get("user") as String, post.get("body") as String, image as String, it.key!!))
                    }
                }
            }
        }

        feedPostAdapter.submitList(dummy_list)
    }

}
