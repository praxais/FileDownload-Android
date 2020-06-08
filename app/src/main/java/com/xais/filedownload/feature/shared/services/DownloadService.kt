package com.xais.filedownload.feature.shared.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.xais.filedownload.R
import com.xais.filedownload.feature.shared.receivers.DownloadReceiver
import com.xais.filedownload.utils.constants.BundleConstants
import com.xais.filedownload.utils.util.FileUtils
import com.xais.filedownload.utils.util.Logger
import java.io.File

/**
 * Created by prajwal on 6/8/20.
 */

class DownloadService : Service() {
    private var filePath: String? = null
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private var notificationId: Int? = null
    private var notificationChannel = "download"
    private var downloadUrl: String? = null
    private var fileName: String? = null
    private var downloadId: Int? = null
    private var downloadProgress: Int? = null

    companion object {
        var instance: DownloadService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onLowMemory() {
        super.onLowMemory()
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        downloadUrl = intent?.getStringExtra(BundleConstants.url)
        fileName = downloadUrl?.substringAfterLast("/")
        initDownload()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        instance = null
        stopSelf()
    }

    private fun initDownload() {
        // Enabling database for resume support even after the service is killed
        val config = PRDownloaderConfig.newBuilder()
            .setDatabaseEnabled(true)
            .setReadTimeout(30_000)
            .setConnectTimeout(30_000)
            .build()
        PRDownloader.initialize(this, config)

        initNotification()
        downloadFile()
    }

    private fun getAction(actionName: String?): PendingIntent? {
        val intent = Intent(this, DownloadReceiver::class.java)
        intent.action = actionName
        return PendingIntent.getBroadcast(this, 0, intent, 0)
    }

    private fun initNotification(isPaused: Boolean = false, isErrorOrComplete: Boolean = false) {
        //clear notifications if any
        notificationManager?.cancelAll()
        notificationId = 1
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationBuilder = NotificationCompat.Builder(this, notificationChannel)
            .setSmallIcon(R.drawable.ic_app_transparent)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle(fileName)
            .setAutoCancel(false)
            .setChannelId(notificationChannel)

        if (!isErrorOrComplete) {
            //add actions to notification
            if (isPaused) {
                //add resume action if download is paused
                notificationBuilder?.addAction(
                    R.drawable.ic_app_transparent, getString(R.string.resume),
                    getAction(getString(R.string.action_resume))
                )
            } else {
                //add pause action if downloading
                notificationBuilder?.addAction(
                    R.drawable.ic_app_transparent, getString(R.string.pause),
                    getAction(getString(R.string.action_pause))
                )
            }

            //add cancel action to stop the download
            notificationBuilder?.setOngoing(true)
                ?.setProgress(100, downloadProgress ?: 0, false)
                ?.setContentText("${downloadProgress ?: 0}%")
                ?.addAction(
                    R.drawable.ic_app_transparent, getString(R.string.cancel),
                    getAction(getString(R.string.action_cancel))
                )
        } else {
            //setOngoing notification type to false if download error or completed
            notificationBuilder?.setOngoing(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannel, notificationChannel, NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "no sound"
            channel.setSound(null, null)
            channel.enableLights(false)
            channel.lightColor = ContextCompat.getColor(this, R.color.colorPrimary)
            channel.enableVibration(false)
            notificationManager?.createNotificationChannel(channel)
        }

        notificationManager?.notify(notificationId ?: 0, notificationBuilder?.build())
    }

    private fun downloadFile() {
        filePath = FileUtils.getCacheFileDirectory(this) + File.separator + fileName
        //download builder
        downloadId = PRDownloader.download(
            downloadUrl, FileUtils.getCacheFileDirectory(this), fileName
        ).build()
            .setOnStartOrResumeListener {
                //download started or resumed
                initNotification(false)
            }.setOnPauseListener {
                //download paused
                initNotification(true)
            }.setOnCancelListener {
                //download cancelled(stop)
                notificationManager?.cancelAll()
                //clear temporary file if download canceled or error occured
                PRDownloader.cleanUp(1)
            }.setOnProgressListener { progress ->
                //get progress of the download in percentage
                downloadProgress = progress.currentBytes.toDouble()
                    .div(progress.totalBytes.toDouble())
                    .times(100)
                    .toInt()
                //notify notification of the download progress
                updateNotificationProgress()
            }.start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    //notify success notification
                    showSuccessNotification()
                    //send local broadcast of download succeed
                    val intent = Intent(BundleConstants.downloadStatusAction)
                    intent.putExtra(BundleConstants.downloadStatus, true)
                    intent.putExtra(BundleConstants.downloadFilePath, filePath)
                    LocalBroadcastManager.getInstance(this@DownloadService).sendBroadcast(intent)
                    stopSelf()
                }

                override fun onError(error: Error?) {
                    Logger.d("DownloadService", "error-${error?.serverErrorMessage}")
                    //notify error notification
                    showErrorNotification()
                    //send local broadcast of download failed
                    val intent = Intent(BundleConstants.downloadStatusAction)
                    intent.putExtra(BundleConstants.downloadStatus, false)
                    LocalBroadcastManager.getInstance(this@DownloadService).sendBroadcast(intent)
                    stopSelf()
                }
            })
    }

    //notify notification of the download progress
    private fun updateNotificationProgress() {
        notificationBuilder?.setOngoing(true)
        notificationBuilder?.setProgress(100, downloadProgress ?: 0, false)
        notificationBuilder?.setContentTitle(fileName)
        notificationBuilder?.setContentText("${downloadProgress ?: 0}%")
        notificationManager?.notify(notificationId ?: 0, notificationBuilder?.build())
    }

    //notify notification of the successful download
    private fun showSuccessNotification() {
        initNotification(isErrorOrComplete = true)
        notificationBuilder?.setOngoing(false)
        notificationBuilder?.setContentText(fileName)
        notificationBuilder?.setContentTitle(getString(R.string.download_complete))
        notificationManager?.notify(notificationId ?: 0, notificationBuilder?.build())
        notificationManager = null
        notificationBuilder = null
    }

    //notify notification of the unsuccessful download
    private fun showErrorNotification() {
        initNotification(isErrorOrComplete = true)
        notificationManager?.cancelAll()
        notificationBuilder?.setOngoing(false)
        notificationBuilder?.setContentText(fileName)
        notificationBuilder?.setContentTitle(getString(R.string.download_error))
        notificationManager?.notify(notificationId ?: 0, notificationBuilder?.build())
        notificationManager = null
        notificationBuilder = null
    }

    //pause download
    fun pauseDownload() {
        downloadId?.let { downloadId ->
            PRDownloader.pause(downloadId)
        }
    }

    //resume download
    fun resumeDownload() {
        downloadId?.let { downloadId ->
            PRDownloader.resume(downloadId)
        }
    }

    //stop download
    fun stopDownload() {
        downloadId?.let { downloadId ->
            PRDownloader.cancel(downloadId)
        }
        stopSelf()
    }
}