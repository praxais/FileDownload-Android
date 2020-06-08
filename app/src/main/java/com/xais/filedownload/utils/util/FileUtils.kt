package com.xais.filedownload.utils.util

import android.content.Context
import com.xais.filedownload.utils.constants.Constants
import java.io.File

/**
 * Created by prajwal on 6/8/20.
 */

object FileUtils {
    fun getCacheFileDirectory(context: Context?): String? =
        context?.externalCacheDir?.absolutePath + File.separator + Constants.fileDirectory
}