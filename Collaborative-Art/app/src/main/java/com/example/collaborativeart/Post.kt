package com.example.collaborativeart

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

// Feel free to change the data types as needed
// Author might be best tied to a unique ID rather than string
// especially when we want to search for friends
data class Post (val user : String, val lat : Double, val long : Double, val body : String){

    companion object{
        fun equals (postA : Post, postB :Post) : Boolean {

            if(postA.user == postB.user
                && postA.body == postB.body){
                return true;
            }

            return false;
        }
    }

}