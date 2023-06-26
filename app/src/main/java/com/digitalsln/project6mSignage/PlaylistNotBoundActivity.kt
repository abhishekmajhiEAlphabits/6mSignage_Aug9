package com.digitalsln.project6mSignage

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import com.digitalsln.project6mSignage.tvLauncher.utilities.Constants

class PlaylistNotBoundActivity : AppCompatActivity() {
    private var screenCodeTxt: TextView? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_not_bound)

        screenCodeTxt = findViewById(R.id.screenCodeTxt)
        var nativeScreenCode = AppPreference(applicationContext).retrieveValueByKey(
            Constants.nativeScreenCode,
            Constants.defaultNativeScreenCode
        )
        if (nativeScreenCode != null) {
            screenCodeTxt!!.text = nativeScreenCode
        }
        else{
            screenCodeTxt!!.text = "Error"
        }
    }
}