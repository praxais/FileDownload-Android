package com.xais.filedownload.feature.main

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.xais.filedownload.R
import com.xais.filedownload.feature.shared.services.DownloadService
import com.xais.filedownload.utils.constants.BundleConstants
import com.xais.filedownload.utils.constants.PermissionConstants
import com.xais.filedownload.utils.util.Logger
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.EasyPermissions

/**
 * Created by prajwal on 6/8/20.
 */

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setup()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        //permission granted by user
        initListeners()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, "Please enable permission.", Toast.LENGTH_SHORT).show()
    }

    private fun setup() {
        //ensure storage permission is granted and continue
        ensureStoragePermission()
    }

    private fun ensureStoragePermission() {
        val perms = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (EasyPermissions.hasPermissions(this, *perms)) {
            //permission already granted
            initListeners()
        } else {
            //ask for permission
            EasyPermissions.requestPermissions(
                this, getString(R.string.storage_permission),
                PermissionConstants.permissionStorage, *perms
            )
        }
    }

    private fun initListeners() {
        //initBroadCastReceiverForDownload
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(BundleConstants.downloadStatusAction))

        btnDownload?.setOnClickListener {
            val url = edtUrl?.text?.toString()
            if (url?.isBlank() == true) {
                return@setOnClickListener
            }

            val intent = Intent(this, DownloadService::class.java)
            intent.putExtra(BundleConstants.url, url)
            startService(intent)
        }
    }

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BundleConstants.downloadStatusAction) {
                Toast.makeText(applicationContext, "Download error", Toast.LENGTH_SHORT).show()
                return
            }
            val downloadStatus = intent.getBooleanExtra(BundleConstants.downloadStatus, false)
            if (!downloadStatus) {
                Toast.makeText(applicationContext, "Download error", Toast.LENGTH_SHORT).show()
                return
            }

            Toast.makeText(applicationContext, "Download Finished", Toast.LENGTH_SHORT).show()

            val filePath = intent.getStringExtra(BundleConstants.downloadFilePath)
            Logger.d("FileDownload", "Path->${filePath}")
        }
    }
}
