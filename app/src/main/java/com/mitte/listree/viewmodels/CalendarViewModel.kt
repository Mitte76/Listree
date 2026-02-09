package com.mitte.listree.viewmodels

import android.app.Application
import android.util.Log
import androidx.credentials.Credential
import androidx.lifecycle.AndroidViewModel
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mitte.listree.data.calendar.CalendarRepository

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CalendarRepository(application)

    fun onCredentialReceived(credential: Credential) {
        if (credential is GoogleIdTokenCredential) {
            val googleIdToken = credential.idToken
            Log.d("CalendarViewModel", "Google ID Token: $googleIdToken")
        } else {
            Log.d("CalendarViewModel", "Not a Google ID token credential")
        }
    }
}
