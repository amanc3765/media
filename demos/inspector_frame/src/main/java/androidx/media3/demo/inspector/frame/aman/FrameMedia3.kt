package androidx.media3.demo.inspector.frame.aman

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.inspector.frame.FrameExtractor
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.guava.await
import java.util.Locale
import androidx.media3.demo.inspector.frame.TAG

@OptIn(UnstableApi::class)
class FrameMedia3(
    private val mediaPath: String,
    private val context: Context,
    private val useHardware: Boolean = false
) {
    private val testName = "FrameMedia3" + if (useHardware) "-HW" else "-SW"
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
            getExtractorMedia3().use { extractor ->
                var frameTimeMs = 0L
                val futures = mutableListOf<ListenableFuture<FrameExtractor.Frame>>()
                for (i in 0..<numIter) {
                    futures.add(extractor.getFrame(frameTimeMs))
                    frameTimeMs += 10000L
                }
                for (future in futures) {
                    val frame = future.await()
                    checkNotNull(frame.bitmap)
//                    Log.d(
//                        TAG, "$testName $mediaPath ${frame.presentationTimeMs} ${frame.decoderName}"
//                    )
                }
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