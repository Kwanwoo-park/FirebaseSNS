package com.example.firebasesns

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity: AppCompatActivity() {
    private var auth: FirebaseAuth? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private val GOOGLE_LOGIN_CODE = -1
    private lateinit var progress_bar: ProgressBar
    private lateinit var email_edittext: EditText
    private lateinit var password_edittext: EditText

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == GOOGLE_LOGIN_CODE) {
            val results = Auth.GoogleSignInApi.getSignInResultFromIntent(result.data!!)

            if (results!!.isSuccess) {
                var account = results.signInAccount
                firebaseAuthWithGoogle(account!!)
            }
            else {
                progress_bar.visibility = View.GONE
            }
        }
        else {
            Log.d("pkw", "openActivityResultLauncher: sign in unsuccess ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        val google_sign_in_button: Button = findViewById(R.id.google_sign_in_button)
        val email_login_button: Button = findViewById(R.id.email_login_button)
        progress_bar = findViewById(R.id.progress_bar)
        email_edittext = findViewById(R.id.email_edittext)
        password_edittext = findViewById(R.id.password_edittext)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        google_sign_in_button.setOnClickListener{ googleLogin() }
        email_login_button.setOnClickListener { emailLogin() }
    }

    override fun onStart() {
        super.onStart()

        moveMainPage(auth?.currentUser)
    }

    private fun moveMainPage(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, getString(R.string.signin_complete), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun googleLogin() {
        progress_bar.visibility = View.VISIBLE
        var signInIntent = googleSignInClient?.signInIntent
        val activityLauncher = resultLauncher
        activityLauncher.launch(signInIntent)
    }

    fun createAndLoginEmail() {
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener {task ->
                progress_bar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.signup_complete), Toast.LENGTH_SHORT).show()
                    moveMainPage(auth?.currentUser)
                }
                else if (task.exception?.message.isNullOrEmpty()) {
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                }
                else {
                    signInEmail()
                }
            }
    }

    private fun emailLogin() {
        if (email_edittext.text.toString().isNullOrEmpty() || password_edittext.text.toString().isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.signout_fail_null), Toast.LENGTH_SHORT).show()
        }
        else {
            progress_bar.visibility = View.VISIBLE
            createAndLoginEmail()
        }
    }

    private fun signInEmail() {
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener { task ->
                progress_bar.visibility = View.GONE

                if (task.isSuccessful) {
                    moveMainPage(auth?.currentUser)
                }
                else{
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                progress_bar.visibility = View.GONE

                if (task.isSuccessful) {
                    moveMainPage(auth?.currentUser)
                }
            }
    }
}