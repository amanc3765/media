package androidx.media3.demo.inspector.frame

import android.app.Activity
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Util
import java.io.File
import java.util.Locale

object PerfTestUtils {

    fun initializeMediaFiles(activity: Activity, mediaFolderPath: String): MutableList<String> {
        requestMediaPermission(activity, mediaFolderPath)
        var mediaFilesList : MutableList<String> = mutableListOf()

        val folder = File(mediaFolderPath)
        if (folder.exists() && folder.isDirectory) {
            mediaFilesList =
                folder.listFiles()?.filter { it.isFile }?.map { it.absolutePath }?.toMutableList()
                    ?: mutableListOf()
        } else {
            Log.e(TAG, "Folder does not exist or is not a directory: $mediaFolderPath")
        }

//        mediaFilesList =
//            mutableListOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")

        if (mediaFilesList.isEmpty()) {
            Log.e(TAG, "No media files found in $mediaFolderPath. Aborting tests.")
            return mediaFilesList
        }
        mediaFilesList.forEach {
            Log.d(TAG, it)
        }
        return mediaFilesList
    }

    private fun requestMediaPermission(activity: Activity, mediaFolderPath: String) {
        while (true) {
            val permissionRequested = Util.maybeRequestReadStoragePermission(
                activity, MediaItem.fromUri(mediaFolderPath)
            )
            if (permissionRequested) {
                Thread.sleep(5000)
            } else {
                break
            }
        }
    }

    fun alert(context: Context) {
        val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(context.applicationContext, notification)
        for (i in 1..20) {
            r.play()
        }
    }

    fun comparePerf(
        testName: String,
        mediaFilesList: List<String>,
        platformTimeMap: Map<String, List<Long>>,
        media3TimeMap: Map<String, List<Long>>
    ) {
        Log.i(TAG, " ------------- Comparison Result ------------- ")
        mediaFilesList.forEach { mediaPath ->
            val platformTimeList = platformTimeMap[mediaPath]
            val media3TimeList = media3TimeMap[mediaPath]

            if (platformTimeList != null && media3TimeList != null) {
                val platformMeanTimeMs = (calculateMedian(platformTimeList) / 1000)
                val media3MeanTimeMs = (calculateMedian(media3TimeList) / 1000)

                val diffPercent = if (platformMeanTimeMs > 0) {
                    ((media3MeanTimeMs - platformMeanTimeMs).toDouble() / platformMeanTimeMs) * 100
                } else {
                    0.0
                }
                val logMessage = String.format(
                    Locale.ROOT,
                    "[$testName | %55s] %4d ms %4d ms %7.1f %%",
                    mediaPath,
                    platformMeanTimeMs,
                    media3MeanTimeMs,
                    diffPercent
                )
                val logLevel = if (diffPercent > 0) Log.ERROR else Log.DEBUG
                Log.println(logLevel, TAG, logMessage)
            }
        }
    }

    fun calculateMedian(list: List<Long>): Long {
        if (list.isEmpty()) return 0
        val sortedList = list.sorted()
        val size = sortedList.size
        return if (size % 2 == 1) {
            sortedList[size / 2]
        } else {
            (sortedList[size / 2 - 1] + sortedList[size / 2]) / 2
        }
    }
}
