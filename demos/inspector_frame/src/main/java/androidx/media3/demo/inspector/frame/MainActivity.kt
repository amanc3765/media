package androidx.media3.demo.inspector.frame

import android.os.Build
import android.os.Bundle
import android.os.Trace
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.media3.demo.inspector.frame.aman.FrameMedia3
import androidx.media3.demo.inspector.frame.aman.FramePlatform
import androidx.media3.demo.inspector.frame.aman.old.FrameMedia3Old
import androidx.media3.demo.inspector.frame.aman.old.FramePlatformOld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val TAG = "FrameExtractorPerf"

@RequiresApi(Build.VERSION_CODES.Q)
class MainActivity : ComponentActivity() {

    //    private val mediaFolderPath = "/sdcard/Download/dataset/1_duration"
    private val mediaFolderPath = "/sdcard/Download/dataset/2_fps"
//    private val mediaFolderPath = "/sdcard/Download/dataset/3_bitrate"
//    private val mediaFolderPath = "/sdcard/Download/dataset/4_resolution"
//    private val mediaFolderPath = "/sdcard/Download/dataset/5_codec"
//    private val mediaFolderPath = "/sdcard/Download/dataset/6_container"
//    private val mediaFolderPath = "/sdcard/Download/dataset/7_faststart"

    private lateinit var mediaFilesList: MutableList<String>
    private var platformTimeMap = mutableMapOf<String, MutableList<Long>>()
    private var media3TimeMap = mutableMapOf<String, MutableList<Long>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.Default) {
            runPerfTest("Frame")
            runPerfTest("FrameOld")
            PerfTestUtils.alert(this@MainActivity)
        }
    }

    private suspend fun runPerfTest(testName: String) {
        mediaFilesList = PerfTestUtils.initializeMediaFiles(this, mediaFolderPath)
        // Platform
        runTestPlatform(testName, 1, 1)
        // Media3 Software decoder
        runTestMedia3(testName, 1, 1, useHardware = false)
        PerfTestUtils.comparePerf("$testName-SW", mediaFilesList, platformTimeMap, media3TimeMap)
        // Media3 Hardware decoder
        runTestMedia3(testName, 1, 1, useHardware = true)
        PerfTestUtils.comparePerf("$testName-HW", mediaFilesList, platformTimeMap, media3TimeMap)
    }

    private fun runTestPlatform(testName: String, numWarmupRuns: Int, numTestRuns: Int) {
        platformTimeMap.clear()

        for (i in 1..numWarmupRuns) {
            Log.i(TAG, " ------------- Warmup Run $i ------------- ")
            Trace.beginSection("Warmup Run $i")
            mediaFilesList.shuffle()
            mediaFilesList.forEach { mediaPath ->
                System.gc()
                Thread.sleep(50)
                when (testName) {
                    "Frame" -> FramePlatform(mediaPath).startTest()
                    "FrameOld" -> FramePlatformOld(mediaPath).startTest()
                    else -> {}
                }
            }
            Trace.endSection()
        }

        for (i in 1..numTestRuns) {
            Log.i(TAG, " ------------- Test Run $i ------------- ")
            Trace.beginSection("Test Run $i")
            mediaFilesList.shuffle()
            mediaFilesList.forEach { mediaPath ->
                System.gc()
                Thread.sleep(50)
                val meanTimeUs = when (testName) {
                    "Frame" -> FramePlatform(mediaPath).startTest()
                    "FrameOld" -> FramePlatformOld(mediaPath).startTest()
                    else -> 0L
                }
                platformTimeMap.getOrPut(mediaPath) { mutableListOf() }.add(meanTimeUs)
            }
            Trace.endSection()
        }

        Log.i(TAG, " ------------- Result ------------- ")
        platformTimeMap.forEach { (mediaPath, timeList) ->
            val meanTimeMs = (PerfTestUtils.calculateMedian(timeList) / 1000)
            Log.d(TAG, "[Platform$testName] $mediaPath: $meanTimeMs ms")
        }
    }

    private suspend fun runTestMedia3(
        testName: String, numWarmupRuns: Int, numTestRuns: Int, useHardware: Boolean = false
    ) {
        media3TimeMap.clear()

        for (i in 1..numWarmupRuns) {
            Log.i(TAG, " ------------- Warmup Run $i ------------- ")
            Trace.beginSection("Warmup Run $i")
            mediaFilesList.shuffle()
            mediaFilesList.forEach { mediaPath ->
                System.gc()
                Thread.sleep(50)
                when (testName) {
                    "Frame" -> FrameMedia3(mediaPath, this@MainActivity, useHardware).startTest()
                    "FrameOld" -> FrameMedia3Old(
                        mediaPath, this@MainActivity, useHardware
                    ).startTest()

                    else -> {}
                }
            }
            Trace.endSection()
        }

        for (i in 1..numTestRuns) {
            Log.i(TAG, " ------------- Test Run $i ------------- ")
            Trace.beginSection("Test Run $i")
            mediaFilesList.shuffle()
            mediaFilesList.forEach { mediaPath ->
                System.gc()
                Thread.sleep(50)
                val meanTimeUs = when (testName) {
                    "Frame" -> FrameMedia3(mediaPath, this@MainActivity, useHardware).startTest()
                    "FrameOld" -> FrameMedia3Old(
                        mediaPath, this@MainActivity, useHardware
                    ).startTest()

                    else -> 0L
                }
                media3TimeMap.getOrPut(mediaPath) { mutableListOf() }.add(meanTimeUs)
            }
            Trace.endSection()
        }

        val actualTestName = testName + if (useHardware) "-HW" else "-SW"
        Log.i(TAG, " ------------- Result ------------- ")
        media3TimeMap.forEach { (mediaPath, timeList) ->
            val meanTimeMs = (PerfTestUtils.calculateMedian(timeList) / 1000)
            Log.d(TAG, "[Media3$actualTestName] $mediaPath: $meanTimeMs ms")
        }
    }
}