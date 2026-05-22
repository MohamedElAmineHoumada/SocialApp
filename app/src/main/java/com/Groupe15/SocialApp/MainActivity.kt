package com.Groupe15.SocialApp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.firestore.FirebaseFirestore

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Comme vous utilisez des Fragments (LoginFragment, FeedFragment) et un NavGraph XML,
        // vous devez utiliser setContentView pour charger activity_main.xml.
        setContentView(R.layout.activity_main)
    }

    //test de feed
    private fun createTestPost(){

        val db = FirebaseFirestore.getInstance()

        val post = hashMapOf(
            "text" to "hello social media",
            "username" to "Soukaina"
        )

        db.collection("posts")
            .add(post)

    }
}
