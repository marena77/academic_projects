package com.example.collaborativeart

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.collaborativeart.databinding.FragmentFavoritesBinding
import com.example.collaborativeart.databinding.FragmentFeedBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class FavoritesFragment : Fragment() {

    companion object {
        fun newInstance() = FavoritesFragment()
    }

    private lateinit var binding : FragmentFavoritesBinding
    private lateinit var userDatabase: DatabaseReference

    private lateinit var feedPostAdapter : FeedPostAdapter

    private val globalViewModel by activityViewModels<GlobalViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        //feedViewModel = ViewModelProvider(this)[FeedViewModel::class.java]

        userDatabase =
            FirebaseDatabase.getInstance()
                .reference

        feedPostAdapter = FeedPostAdapter(globalViewModel)
        val recyclerView: RecyclerView = binding.favoritesRecyclerView

        recyclerView.adapter = feedPostAdapter

        addFavorites()


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    private fun addFavorites() {
        var dummy_list = mutableListOf<FeedPost>()
        runBlocking {
            val all_posts = userDatabase.child("posts").get().await()
            val friends = userDatabase.child("users").child(globalViewModel.currentUser.value!!).child("friends").get().await().getValue() as List<String>
            val favorites = userDatabase.child("users").child(globalViewModel.currentUser.value!!).child("favorites").get().await().getValue() as List<String>
            // for all posts
            all_posts.children.forEach {
                if (friends.contains(it.key!!)) {
                    it.children.forEach {
                        if (favorites.contains(it.key!!)) {
                            var post = (it.getValue() as HashMap<String, Any>)
                            var image = post.get(post.keys.elementAt(0))
                            dummy_list.add(
                                FeedPost(
                                    post.get("user") as String,
                                    post.get("body") as String,
                                    image as String,
                                    it.key!!
                                )
                            )
                        }
                    }
                }
            }
        }
        feedPostAdapter.submitList(dummy_list)
    }
}