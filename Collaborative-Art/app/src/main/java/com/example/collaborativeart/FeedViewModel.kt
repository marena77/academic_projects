package com.example.collaborativeart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// Don't think I'll end up needing this
class FeedViewModel : ViewModel() {

    private val _posts = MutableLiveData< List<Post> >()
    val posts : LiveData<List<Post>>
        get() = _posts



}