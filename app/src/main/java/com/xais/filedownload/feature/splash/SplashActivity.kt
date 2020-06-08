package com.xais.filedownload.feature.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xais.filedownload.feature.main.MainActivity

/**
 * Created by prajwal on 6/8/20.
 */

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigateToMainPage()
    }

    private fun navigateToMainPage() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}