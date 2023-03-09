package com.example.firebasesns

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

//    var auth: FirebaseAuth? = null
//    var authListener: FirebaseAuth.AuthStateListener? = null
//    var googleSignInClient: GoogleSignInClient? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken(getString(R.string.default_web_client_id))
//            .requestEmail()
//            .build()
//
//        googleSignInClient = GoogleSignIn.getClient(this, gso)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == 100) {
//            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
//            if (result.isSuccess) {
//                val account = result.signInAccount
//                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
//                FirebaseAuth.getInstance().signInWithCredential(credential)
//            }
//        }
//    }
//
//    fun signIn() {
//        val signInIntent = googleSignInClient?.signInIntent
//        startActivityForResult(signInIntent, 100)
//    }
}