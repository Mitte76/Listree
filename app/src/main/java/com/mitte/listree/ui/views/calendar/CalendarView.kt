package com.mitte.listree.ui.views.calendar

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mitte.listree.R
import kotlinx.coroutines.launch

@Composable
fun CalendarView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val auth = FirebaseAuth.getInstance()
    val serverClientId = remember { context.getString(R.string.default_web_client_id) }
    val credentialManager = remember { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.background(Color.White)) {
        Button(onClick = {
            coroutineScope.launch {
                try {
                    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(signInWithGoogleOption)
                        .build()

                    val result = credentialManager.getCredential(activity, fallbackRequest)
                    val credential = result.credential
                    if (credential is GoogleIdTokenCredential) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("CalendarView", "Firebase interactive sign-in SUCCESS")
                                } else {
                                    Log.e("CalendarView", "Firebase interactive sign-in FAILED", task.exception)
                                }
                            }
                    }
                } catch (e: Exception) {
                    Log.e("CalendarView", "Sign-in with Google failed", e)
                }
            }
        }) {
            Text("Sign in with Google")
        }
    }
}
