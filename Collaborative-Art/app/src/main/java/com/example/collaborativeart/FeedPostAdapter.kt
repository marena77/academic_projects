package com.example.collaborativeart

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FeedPostAdapter(private val globalViewModel: GlobalViewModel) : ListAdapter<FeedPost, FeedPostAdapter.PostViewHolder>(PostDifferenceCallback) {

    class PostViewHolder(itemView: View, val globalViewModel: GlobalViewModel): RecyclerView.ViewHolder(itemView) {
        private val authorTextView: TextView = itemView.findViewById(R.id.feed_textview_author)
        private val memoirTextView: TextView = itemView.findViewById(R.id.feed_textview_memoir)
        private val imageView: ImageView = itemView.findViewById(R.id.feed_image_view)
        private val fav: ImageView = itemView.findViewById(R.id.imageView2)


        private var currentPost: FeedPost? = null

        fun bind(post: FeedPost) {
            currentPost = post

            memoirTextView.text = post.memoir
            authorTextView.text = post.user
            var database = FirebaseDatabase.getInstance()
                .reference
            var decoded = Base64.decode(post.image, Base64.DEFAULT)
            var bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            imageView.setImageBitmap(bmp)
            var doubleClickLastTime = 0L
            imageView.setOnClickListener {
                if(System.currentTimeMillis() - doubleClickLastTime < 300){ // User has double tapped
                    doubleClickLastTime = 0
                    // TODO
                    // Add currentPost to the currentUser's list of favorites
                    database.child("users").child(globalViewModel.currentUser.value!!).child("favorites").get().addOnSuccessListener {
                        var oldList = it.value as List<String>
                        database.child("users").child(globalViewModel.currentUser.value!!).child("favorites").setValue(oldList + mutableListOf(currentPost!!.postID))
                    }

                    fav.visibility = View.VISIBLE

                }else{ // User has single tapped
                    doubleClickLastTime = System.currentTimeMillis()
                }
            }
            fav.setOnClickListener { // User has clicked the favorite button
                if (fav.visibility == View.VISIBLE){ // Post is favorited already
                    // TODO
                    // Remove favorited post from database
                    database.child("users").child(globalViewModel.currentUser.value!!).child("favorites").get().addOnSuccessListener {
                        var oldList = it.value as List<String>
                        var newList = mutableListOf<String>()

                        oldList.forEach {
                            if (!it.equals(currentPost!!.postID)) {
                                newList.add(it)
                            }
                        }
                        database.child("users").child(globalViewModel.currentUser.value!!).child("favorites").setValue(newList)
                    }

                    fav.visibility = View.INVISIBLE
                } else {
                    // ignore
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.feed_item_layout, parent, false)
        return PostViewHolder(view, globalViewModel)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

}

object PostDifferenceCallback : DiffUtil.ItemCallback<FeedPost>() {
    override fun areItemsTheSame(oldItem: FeedPost, newItem: FeedPost): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: FeedPost, newItem: FeedPost): Boolean {
        return oldItem.image.equals(newItem.image) && oldItem.memoir.equals(newItem.memoir) && oldItem.user.equals(newItem.user)
    }
}