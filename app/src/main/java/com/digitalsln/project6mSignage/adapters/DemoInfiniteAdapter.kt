package com.digitalsln.project6mSignage.adapters

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import com.digitalsln.project6mSignage.R
import com.digitalsln.project6mSignage.loopingviewpager.LoopingPagerAdapter
import com.digitalsln.project6mSignage.model.FileDescriptors


class DemoInfiniteAdapter(
    itemList: ArrayList<FileDescriptors>,
    isInfinite: Boolean
) : LoopingPagerAdapter<Int>(itemList, isInfinite) {


    override fun getItemViewType(listPosition: Int): Int {
        return if (itemList!![listPosition].contentType == 3) 3
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
            Log.d("TvTimer", "listIf :: $listPosition :: ${itemList!![listPosition].slideFilePath}")
            imageView.visibility = View.VISIBLE
            if (itemList!![listPosition].slideFilePath != null && itemList!![listPosition].isFileExist) {
                videoView.visibility = View.VISIBLE
                videoView.setVideoURI(Uri.parse(itemList!![listPosition].slideFilePath))
                videoView.setOnPreparedListener {
                    it.start()
                    it.isLooping = true
//                    videoView!!.start()
                }
            } else {
//                videoView.stopPlayback()
                videoView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                imageView.setImageResource(R.drawable.loading)
//                loading.visibility = View.VISIBLE
            }
        } else if (viewType == 2) {
            videoView = convertView.findViewById<View>(R.id.video) as VideoView
            imageView = convertView.findViewById<View>(R.id.image) as ImageView
            Log.d("TvTimer", "listElseIf :: $listPosition")
//            videoView.stopPlayback()
            videoView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            if (itemList!![listPosition].slideFilePath != null && itemList!![listPosition].isFileExist) {
                imageView.setImageURI(Uri.parse(itemList!![listPosition].slideFilePath))
            } else {
//                imageView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                imageView.setImageResource(R.drawable.loading)
            }

        } else {
            videoView = convertView.findViewById<View>(R.id.video) as VideoView
            imageView = convertView.findViewById<View>(R.id.image) as ImageView
            Log.d("TvTimer", "listElse :: $listPosition")
//            videoView!!.stopPlayback()
            videoView!!.visibility = View.GONE
            imageView!!.visibility = View.VISIBLE
            imageView!!.setImageResource(R.drawable.loading)
        }
    }

    fun setFileDescriptors(fileDescriptors: ArrayList<FileDescriptors>) {
        if (this.itemList!!.size != fileDescriptors.size) {
            this.itemList = fileDescriptors
            notifyDataSetChanged()
        }
    }

    companion object {
        private const val VIEW_TYPE_NORMAL = 100
        private const val VIEW_TYPE_SPECIAL = 101
    }
}