package com.example.collaborativeart

// In the FeedPost, image is kept as base64 encoded string
data class FeedPost (val user: String, val memoir: String, val image: String, val postID: String){

}