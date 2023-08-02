package com.digitalsln.project6mSignage.adapters

import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import com.digitalsln.project6mSignage.R
import com.digitalsln.project6mSignage.loopingviewpager.LoopingPagerAdapter
import com.digitalsln.project6mSignage.model.FileDescriptors
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DemoInfiniteAdapter(
    itemList: ArrayList<FileDescriptors>,
    isInfinite: Boolean
) : LoopingPagerAdapter<Int>(itemList, isInfinite) {

    override fun getItemViewType(listPosition: Int): Int {
            return if (itemList!![listPosition]?.contentType == 3) 3
            else 2
    }

    override fun inflateView(
        viewType: Int,
        container: ViewGroup,
        listPosition: Int
    ): View {
        return LayoutInflater.from(container.context)
            .inflate(R.layout.item_page, container, false)
    }

    override fun bindView(
        convertView: View,
        listPosition: Int,
        viewType: Int
    ) {
        var videoView: VideoView? = null
        var imageView: ImageView? = null
        if (viewType == 3) {
            videoView = convertView.findViewById<View>(R.id.video) as VideoView
            imageView = convertView.findViewById<View>(R.id.image) as ImageView
//            Log.d("TvTimer", "listIf :: $listPosition :: ${itemList!![listPosition].slideFilePath}")
            Log.d(
                "TvTimer",
                "Playing video :: FilePosition : $listPosition and FilePath : ${itemList!![listPosition].slideFilePath}"
            )
            imageView.visibility = View.GONE
            if (itemList!![listPosition].slideFilePath != null && itemList!![listPosition].isFileExist) {
                videoView.visibility = View.VISIBLE
                Log.d("TvTimer","File exist : -  ${itemList!![listPosition].slideFilePath}")
                videoView.setVideoURI(Uri.parse(itemList!![listPosition].slideFilePath))
//                val mediaController = MediaController(videoView.context)
//                mediaController.setAnchorView(videoView)


                videoView.setOnPreparedListener {
                    videoView!!.start()
                    it.start()
                    videoView!!.setBackgroundColor(Color.TRANSPARENT)
//                    Handler().postDelayed({
//                        videoView!!.start()
//                        videoView!!.setBackgroundColor(Color.TRANSPARENT)
//                    }, duration.toLong() * 1000)
                }
                videoView!!.setOnCompletionListener {
                    videoView!!.start()
                    videoView!!.setBackgroundColor(Color.TRANSPARENT)
                }

            } else {
                Log.d("TvTimer","Video File Not Exist..");
                videoView = convertView.findViewById<View>(R.id.video) as VideoView
                imageView = convertView.findViewById<View>(R.id.image) as ImageView
//                videoView.stopPlayback()
                videoView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                imageView.setImageResource(R.drawable.loading)
            }
        } else if (viewType == 2) {
            Log.d("TvTimer","viewType 2");
            videoView = convertView.findViewById<View>(R.id.video) as VideoView
            imageView = convertView.findViewById<View>(R.id.image) as ImageView
//            Log.d("TvTimer", "listElseIf :: $listPosition")
//            Log.d(
//                "TvTimer",
//                "Playing image :: FilePosition : $listPosition and FilePath : ${itemList!![listPosition].slideFilePath}"
//            )
//            videoView.stopPlayback()
            videoView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            if (itemList!![listPosition].slideFilePath != null && itemList!![listPosition].isFileExist) {
                Log.d("TvTimer","ImageFile path exist");
                imageView.setImageURI(Uri.parse(itemList!![listPosition].slideFilePath))
            } else {
                Log.d("TvTimer","ImageFile path NOT exist");
//                imageView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                imageView.setImageResource(R.drawable.loading)
            }

        } else {
            Log.d("TvTimer","We are in else part now ....");
            videoView = convertView.findViewById<View>(R.id.video) as VideoView
            imageView = convertView.findViewById<View>(R.id.image) as ImageView
//            Log.d("TvTimer", "listElse :: $listPosition")
            Log.d(
                "TvTimer",
                "Loading Screen :: FilePosition : $listPosition and FilePath : ${itemList!![listPosition].slideFilePath}"
            )
//            videoView!!.stopPlayback()
            videoView!!.visibility = View.GONE
            imageView!!.visibility = View.VISIBLE
            imageView!!.setImageResource(R.drawable.loading)
        }
    }

    fun setFileDescriptors(fileDescriptors: ArrayList<FileDescriptors>) {
        //        var size = this.itemList!!.size;
        //        var fileDescriptorSize = fileDescriptors.size;
        //        Log.d("kamothi","item list size $size && fileDescriptor Size is $fileDescriptorSize");
        //        if (this.itemList!!.size != fileDescriptors.size) {
        //            this.itemList = fileDescriptors
        //            notifyDataSetChanged()
        //        }
    }

    companion object {
        private const val VIEW_TYPE_NORMAL = 100
        private const val VIEW_TYPE_SPECIAL = 101
    }
}