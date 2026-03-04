package androidx.media3.demo.inspector.frame

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Util

class MainActivity : AppCompatActivity() {

    private val mediaFolderPath = "/sdcard/Download/dataset/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun requestMediaPermission() {
        while (true) {
            val permissionRequested = Util.maybeRequestReadStoragePermission(
                this, MediaItem.fromUri(mediaFolderPath)
            )
            if (permissionRequested) {
                Thread.sleep(5000)
            } else {
                break
            }
        }
    }
}
