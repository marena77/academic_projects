package com.example.collaborativeart

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.collaborativeart.databinding.FragmentLoginBinding
import com.google.firebase.database.*
import com.example.collaborativeart.User
import com.google.firebase.database.ktx.getValue

class FragmentLogin: Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private lateinit var userDatabase: DatabaseReference
    private val globalViewModel by activityViewModels<GlobalViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.loginButton.setOnClickListener {
            attemptLogin()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userDatabase =
            FirebaseDatabase.getInstance()
                .getReference("users")

    }

    // Some code can go inside here (Firebase stuff) to create/verify login credentials
    private fun attemptLogin(){
        with(binding){

            if (!isPasswordValid(password.text.toString())){
                password.error = "Password must be >5 characters"
                if (email.error == null){
                    email.requestFocus()
                }
            }
            if (email.error == null && password.error == null){

                val id = email.text.toString().replace(".","")
                val user = User(id, password.text.toString())
                userDatabase.child("users").get().addOnSuccessListener {
                    userDatabase.child(id).get().addOnSuccessListener {
                        if (it.value == null) {
                            // this user-id doesn't exist so add it to the database
                            user.addFavorite("dummy")
                            user.addFriend(id)

                            //println(user.getFavorites())
                            userDatabase.child(id).setValue(user)
                            globalViewModel.updateUser(id)
                            login_success()
                        } else {
                            // user-id does exist so verify inputted password matches password in it.value
                            // the user object is returned as a hashmap. who knew.
                            val stored_pass =(it.getValue() as HashMap<String?, String?>).get("password")
                            if (password.text.toString() == stored_pass) {
                                globalViewModel.updateUser(email.text.toString())
                                login_success()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Incorrect Password!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }.addOnFailureListener {
                        println(it.message)
                    }
                }.addOnFailureListener {
                    println(it.message)
                }

           }

        }
        //login_success()
    }


    // Can make this more rigorous
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    private fun login_success() {

        globalViewModel.startNavBar(activity?.supportFragmentManager!!)
        findNavController().navigate(
            FragmentLoginDirections.actionFragmentLoginToMapsFragment("User")
        )
        //globalViewModel.navigateToMap()

    }
}