package com.digitalsln.project6mSignage

import android.app.DownloadManager
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.animation.Interpolator
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.digitalsln.project6mSignage.adapters.DemoInfiniteAdapter
import com.digitalsln.project6mSignage.loopingviewpager.FixedSpeedScroller
import com.digitalsln.project6mSignage.loopingviewpager.LoopingViewPager
import com.digitalsln.project6mSignage.model.FileDescriptors
import com.digitalsln.project6mSignage.network.PlaylistManager
import com.digitalsln.project6mSignage.receivers.DownloadsReceiver
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference

class SlideShowActivity : AppCompatActivity() {

    private lateinit var viewPager: LoopingViewPager
    private var adapter: DemoInfiniteAdapter? = null
    private lateinit var downloadsReceiver: DownloadsReceiver
    private lateinit var fileDescriptors: ArrayList<FileDescriptors>
    private lateinit var playlistManager: PlaylistManager

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slide_show)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        viewPager = findViewById(R.id.viewpager)

        try {
            getStoragePermission()

            //api call
            Thread(Runnable {
                kotlin.run {
                    playlistManager = PlaylistManager(this)
                    playlistManager.getPlayListData()
                }
            }).start()

            //init fileDescriptors arrayList
            fileDescriptors = ArrayList<FileDescriptors>()
            fileDescriptors.clear()

            fileDescriptors.add(FileDescriptors(100, 2, "", false, 10))
            fileDescriptors.add(FileDescriptors(100, 2, "", false, 10))

            try {
                val mScroller = ViewPager::class.java.getDeclaredField("mScroller")
                mScroller.isAccessible = true
                val interpolator = ViewPager::class.java.getDeclaredField("sInterpolator")
                interpolator.isAccessible = true
                val scroller = FixedSpeedScroller(
                    viewPager.context,
                    interpolator[null] as Interpolator
                )
                mScroller.set(viewPager, scroller)
            } catch (e: NoSuchFieldException) {
            } catch (e: IllegalArgumentException) {
            } catch (e: IllegalAccessException) {
            }

            adapter = DemoInfiniteAdapter(fileDescriptors, true)
            viewPager.adapter = adapter

            downloadsReceiver = DownloadsReceiver()
            registerDownloadReceiver()

            Log.d(TAG, "descriptorMain :: $fileDescriptors")

            getMediaFilePaths()
            setupObservers()
        } catch (e: Exception) {
            Log.d(TAG, "error in onCreate :: $e")
        }
    }

    private fun setupObservers() {
        downloadsReceiver.downloadState.observe(this, Observer {
            Log.d(TAG, "inside observer")
            getMediaFilePaths()
            adapter!!.setFileDescriptors(fileDescriptors)
        })
        playlistManager.fileDescriptorData.observe(this, Observer {
            Log.d(TAG, "inside file observer")
            getMediaFilePaths()
            adapter!!.setFileDescriptors(fileDescriptors)
        })
    }

    private fun registerDownloadReceiver() {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(downloadsReceiver, filter)
    }

    private fun getMediaFilePaths() {
        fileDescriptors = playlistManager.getDownloadedFilePath()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getStoragePermission(): Boolean {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission is granted")
            //File write logic here
            return true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1234
            )
            return false
        }
    }


    override fun onResume() {
        try {
            setupObservers()
        } catch (e: Exception) {
            Log.d(TAG, "error :: $e")
        }
        super.onResume()
    }

    override fun onDestroy() {
        unregisterReceiver(downloadsReceiver)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SlideShowActivity"
    }
}