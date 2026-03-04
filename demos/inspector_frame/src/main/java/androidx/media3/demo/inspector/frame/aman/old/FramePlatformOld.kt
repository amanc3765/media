package androidx.media3.demo.inspector.frame.aman.old

import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.OPTION_PREVIOUS_SYNC
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.media3.demo.inspector.frame.TAG
import java.util.Locale

@RequiresApi(29)
class FramePlatformOld(private val mediaPath: String) {

    private val testName = "FramePlatformOld"
    private val numIter = 10

    fun startTest(): Long {
        val meanTimeUs = extractFramePlatform() / numIter
        Log.i(
            TAG, String.format(
                Locale.ROOT,
                "[$testName] %30s %7d us",
                mediaPath,
                meanTimeUs,
            )
        )
        return meanTimeUs
    }

    private fun extractFramePlatform(): Long {
        val startTimeNs = System.nanoTime()

        try {
            var frameTimeMs = 0L
            for (i in 0..<numIter) {
                getRetrieverPlatform().use { retriever ->
                    val bitmap = retriever.getFrameAtTime(frameTimeMs * 1000L, OPTION_PREVIOUS_SYNC)
                    checkNotNull(bitmap)
                }
                frameTimeMs += 10000L
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        return (System.nanoTime() - startTimeNs) / 1000
    }

    private fun getRetrieverPlatform(): MediaMetadataRetriever {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(mediaPath)
        return retriever
    }
}