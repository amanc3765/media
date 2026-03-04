package androidx.media3.demo.inspector.frame;

import static androidx.media3.effect.Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import androidx.media3.common.MediaItem;
import androidx.media3.effect.Presentation;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.inspector.frame.FrameExtractor;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FrameExtractorPerformanceTest {
  private static final String TAG = "FrameExtractorPerf";
  private static final String ASSET_FILE_NAME = "android-screens-lavf-56.36.100-aac-avc-main-1280x720.mkv";
  private static final int BITMAP_COUNT = 8;
  private static final int TARGET_WIDTH = 50;
  private static final int TARGET_HEIGHT = 50;

  @Test
  public void compareFrameExtractionPerformance() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();

    // --- Setup: Get file duration using MediaMetadataRetriever ---
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    try (AssetFileDescriptor afd = context.getAssets().openFd(ASSET_FILE_NAME)) {
      retriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
    }
    String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
    long durationMs = Long.parseLong(durationStr);

    List<Long> targetPositionsMs = new ArrayList<>();
    for (int i = 0; i < BITMAP_COUNT; i++) {
      targetPositionsMs.add(i * durationMs / (BITMAP_COUNT - 1));
    }
    Log.i(TAG, "Video duration: " + durationMs + " ms. Target positions: " + targetPositionsMs);

    // --- Benchmark 1: FrameExtractor (Software) ---
    runFrameExtractorBenchmark(context, targetPositionsMs, MediaCodecSelector.PREFER_SOFTWARE, "Software");

    // --- Benchmark 2: FrameExtractor (Hardware) ---
    runFrameExtractorBenchmark(context, targetPositionsMs, MediaCodecSelector.DEFAULT, "Hardware");

    // --- Benchmark 3: MediaMetadataRetriever ---
    long startMmr = System.nanoTime();
    for (long positionMs : targetPositionsMs) {
      long timeUs = positionMs * 1000;
      Bitmap bitmap = retriever.getScaledFrameAtTime(
          timeUs,
          MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
          TARGET_WIDTH,
          TARGET_HEIGHT);
      checkNotNull(bitmap);
    }
    long durationMmr = Duration.ofNanos(System.nanoTime() - startMmr).toMillis();
    Log.i(TAG, "MediaMetadataRetriever took: " + durationMmr + " ms for " + BITMAP_COUNT + " frames.");

    retriever.release();
  }

  private void runFrameExtractorBenchmark(
      Context context,
      List<Long> targetPositionsMs,
      MediaCodecSelector mediaCodecSelector,
      String decoderTypeLoggerStr)
      throws Exception {
    long startFe = System.nanoTime();
    try (FrameExtractor frameExtractor = new FrameExtractor.Builder(context,
        MediaItem.fromUri("asset:///" + ASSET_FILE_NAME))
        .setSeekParameters(SeekParameters.CLOSEST_SYNC)
        .setMediaCodecSelector(mediaCodecSelector)
        .setEffects(
            ImmutableList.of(
                Presentation.createForWidthAndHeight(
                    TARGET_WIDTH, TARGET_HEIGHT, LAYOUT_SCALE_TO_FIT_WITH_CROP)))
        .build()) {
      List<ListenableFuture<FrameExtractor.Frame>> futures = new ArrayList<>();
      for (long positionMs : targetPositionsMs) {
        futures.add(frameExtractor.getFrame(positionMs));
      }
      // Wait for all frames at once with a generous timeout (30s) to account for
      // initialization
      List<FrameExtractor.Frame> frames = Futures.allAsList(futures).get(30, SECONDS);
      for (FrameExtractor.Frame frame : frames) {
        checkNotNull(frame.bitmap);
      }
      long durationFe = Duration.ofNanos(System.nanoTime() - startFe).toMillis();
      Log.i(TAG,
          "FrameExtractor (" + decoderTypeLoggerStr + ") took: " + durationFe + " ms for " + BITMAP_COUNT + " frames.");
      if (!frames.isEmpty()) {
        Log.i(TAG, "Frame used decoder: " + frames.get(0).decoderName);
      }
    }
  }
}
