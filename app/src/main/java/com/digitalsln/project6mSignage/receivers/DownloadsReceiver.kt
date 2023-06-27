package com.digitalsln.project6mSignage.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DownloadsReceiver : BroadcastReceiver() {
    private val _downloadState = MutableLiveData<String>()
    val downloadState: LiveData<String> = _downloadState
    private val TAG ="TvTimer"

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "download complete")
        _downloadState.value = "file downloaded"

    }
}