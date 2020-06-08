package com.xais.filedownload.feature.shared.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xais.filedownload.R
import com.xais.filedownload.feature.shared.services.DownloadService

/**
 * Created by prajwal on 6/8/20.
 */

class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            context?.getString(R.string.action_pause) -> onPauseClicked()
            context?.getString(R.string.action_resume) -> onResumeClicked()
            context?.getString(R.string.action_cancel) -> onCancelClicked()
        }
    }

    private fun onPauseClicked() {
        DownloadService.instance?.pauseDownload()
    }

    private fun onResumeClicked() {
        DownloadService.instance?.resumeDownload()
    }

    private fun onCancelClicked() {
        DownloadService.instance?.stopDownload()
    }
}