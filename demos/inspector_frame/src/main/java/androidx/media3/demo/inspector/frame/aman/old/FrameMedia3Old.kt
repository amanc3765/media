package androidx.media3.demo.inspector.frame.aman.old

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.demo.inspector.frame.TAG
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.inspector.frame.FrameExtractor
import kotlinx.coroutines.guava.await
import java.util.Locale

@OptIn(UnstableApi::class)
class FrameMedia3Old(
    private val mediaPath: String,
    private val context: Context,
    private val useHardware: Boolean = false
) {

    private val testName = "FrameMedia3Old" + if (useHardware) "-HW" else "-SW"
    private val numIter = 10

    suspend fun startTest(): Long {
        val meanTimeUs = extractFrameMedia3() / numIter
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

    private suspend fun extractFrameMedia3(): Long {
        val startTimeNs = System.nanoTime()

        try {
            var frameTimeMs = 0L
            for (i in 0..<numIter) {
                getExtractorMedia3().use { extractor ->
                    val frame = extractor.getFrame(frameTimeMs).await()
                    checkNotNull(frame.bitmap)
//                    Log.d(
//                        TAG, "$testName $mediaPath ${frame.presentationTimeMs} ${frame.decoderName}"
//                    )
                }
                frameTimeMs += 10000L
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        return (System.nanoTime() - startTimeNs) / 1000
    }

    private fun getExtractorMedia3(): FrameExtractor {
        val mediaItem = MediaItem.fromUri(mediaPath)
        val codecSelector =
            if (useHardware) MediaCodecSelector.DEFAULT else MediaCodecSelector.PREFER_SOFTWARE
        return FrameExtractor.Builder(context, mediaItem).setMediaCodecSelector(codecSelector)
            .setSeekParameters(SeekParameters.PREVIOUS_SYNC).build()
    }
}