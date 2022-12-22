package com.example.collaborativeart

data class User (val username: String = "", val password: String = ""){

    // A new user has no posts or friends
    private var posts : MutableList<Post> = mutableListOf()
    private var friends : MutableList<String> = mutableListOf()
    private var favorites : MutableList<String> = mutableListOf()

    // Post Methods
    fun getPosts() : List<Post> {
        return posts.toList()
    }

    fun addPost(post : Post) : Boolean {
        return posts.add(post)
    }

    fun clearPosts(){
        posts = mutableListOf()
    }

    // Friends List Methods
    fun getFriends() : List<String> {
        return friends.toList()
    }

    fun addFriend(newFriend : String)  : Boolean {
        return friends.add(newFriend)
    }

    fun addFavorite(newFav : String) : Boolean {
        return favorites.add(newFav)
    }

    fun getFavorites() : List<String> {
        return favorites.toList()
    }

    fun clearFriends() : Unit {
        friends = mutableListOf()
    }



}