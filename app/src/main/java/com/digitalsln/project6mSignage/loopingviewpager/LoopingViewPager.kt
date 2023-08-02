package com.digitalsln.project6mSignage.loopingviewpager

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.digitalsln.project6mSignage.R
import com.digitalsln.project6mSignage.network.PlaylistManager
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import com.digitalsln.project6mSignage.tvLauncher.utilities.Constants


/**
 * A ViewPager that auto-scrolls, and supports infinite scroll.
 * For infinite Scroll, you may use LoopingPagerAdapter.
 */
class LoopingViewPager : ViewPager {
    protected var isInfinite = true
    protected var isAutoScroll = false
    protected var wrapContent = true

    //    private var staticDurations = longArrayOf(2000, 2000, 2000, 2000, 2000, 2000, 2000,2000, 2000)
    private var staticDurations = LongArray(2000) { i -> ((1 + 10) * 1000).toLong() }
    var counter = 0
    private lateinit var playlistManager: PlaylistManager
    private lateinit var slideDurations: LongArray
    private val TAG = "TvTimer"
    var duration = 0L

    //AutoScroll
    private var interval = 5000
    private var currentPagePosition = 0
    private var isAutoScrollResumed = false
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private val autoScrollRunnable = Runnable {
        if (adapter == null || !isAutoScroll || adapter?.count ?: 0 < 2) return@Runnable
        if (!isInfinite && adapter?.count ?: 0 - 1 == currentPagePosition) {
            currentPagePosition = 0
        } else {
            currentPagePosition++
        }
        setCurrentItem(currentPagePosition, true)
    }

    //For Indicator
    var onIndicatorProgress: ((selectingPosition: Int, progress: Float) -> Unit)? = null
    private var previousScrollState = SCROLL_STATE_IDLE
    private var scrollState = SCROLL_STATE_IDLE

    constructor(context: Context) : super(context) {
        playlistManager = PlaylistManager(context)
        init()
    }

    constructor(
        context: Context, attrs: AttributeSet?
    ) : super(context, attrs) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.LoopingViewPager, 0, 0)
        try {
            isInfinite = a.getBoolean(R.styleable.LoopingViewPager_isInfinite, false)
            isAutoScroll = a.getBoolean(R.styleable.LoopingViewPager_autoScroll, false)
            wrapContent = a.getBoolean(R.styleable.LoopingViewPager_wrap_content, true)
            interval = a.getInt(R.styleable.LoopingViewPager_scrollInterval, 5000)
            isAutoScrollResumed = isAutoScroll
        } finally {
            a.recycle()
        }
        playlistManager = PlaylistManager(context)
        init()
    }

    private fun fillData() {
        slideDurations = staticDurations
        var nativeScreenCode = AppPreference(context).retrieveValueByKey(
            Constants.nativeScreenCode,
            Constants.defaultNativeScreenCode
        )
        val responseSize = AppPreference(context).retrieveValueByKey(
            "$nativeScreenCode-${Constants.playlistSize}",
            Constants.defaultPlaylistSize
        )
        if (responseSize.toInt() != null && responseSize.toInt() != 0) {
            for (i in 0 until responseSize.toInt()) {
                slideDurations[i] =
                    AppPreference(context).retrieveIntervalValueByKey(
                        "$nativeScreenCode-$i-${Constants.interval}",
                        Constants.defaultInterval
                    ).toLong() * 1000

                var key = "$nativeScreenCode-$i-${Constants.interval}"
                var key2 = AppPreference(context).retrieveIntervalValueByKey(
                    "$nativeScreenCode-$i-${Constants.interval}",
                    Constants.defaultInterval
                ).toLong() * 1000
                Log.d("abhi","keys: $key2 : $responseSize")
            }
        } else {
            slideDurations = staticDurations
        }
    }

    protected fun init() {
        fillData()
        addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {
                if (onIndicatorProgress == null) return
                onIndicatorProgress?.invoke(
                    getRealPosition(position), positionOffset
                )
            }

            override fun onPageSelected(position: Int) {
                currentPagePosition = position
                if (isAutoScrollResumed) {
                    autoScrollHandler.removeCallbacks(autoScrollRunnable)
                    if (position > 0) {
                        duration = slideDurations[position - 1]
                        Log.d("abhi","duration : $duration")
                    } else {
                        duration = slideDurations[position]
                        Log.d("abhi","duration : $duration")
                    }

                    autoScrollHandler.postDelayed(
                        autoScrollRunnable, duration
                    )
                    if (counter == slideDurations.size - 1) {
                        counter = 0
                    } else {
                        counter++
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                previousScrollState = scrollState
                scrollState = state
                if (state == SCROLL_STATE_IDLE) {
                    //Below are code to achieve infinite scroll.
                    //We silently and immediately flip the item to the first / last.
                    if (isInfinite) {
                        if (adapter == null) return
                        val itemCount = adapter?.count ?: 0
                        if (itemCount < 2) {
                            return
                        }
                        val index = currentItem
                        if (index == 0) {
                            setCurrentItem(itemCount - 2, true) //Real last item
                        } else if (index == itemCount - 1) {
                            setCurrentItem(1, true) //Real first item
                        }
                    }
                }
            }
        })
        if (isInfinite) setCurrentItem(1, false)
    }

    override fun setAdapter(adapter: PagerAdapter?) {
        super.setAdapter(adapter)
        if (isInfinite) setCurrentItem(1, false)
    }

    fun resumeAutoScroll() {
        isAutoScrollResumed = true
        autoScrollHandler.postDelayed(autoScrollRunnable, interval.toLong())
    }

    fun pauseAutoScroll() {
        isAutoScrollResumed = false
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }//Dummy first item is selected. Indicator should be at the first one//Dummy last item is selected. Indicator should be at the last one

    /**
     * A method that helps you integrate a ViewPager Indicator.
     * This method returns the expected count of indicators.
     */
    val indicatorCount: Int
        get() = if (adapter is LoopingPagerAdapter<*>) {
            (adapter as LoopingPagerAdapter<*>).listCount
        } else {
            adapter?.count ?: 0
        }

    /**
     * This function needs to be called if dataSet has changed,
     * in order to reset current selected item and currentPagePosition and autoPageSelectionLock.
     */
    fun reset() {
        currentPagePosition = if (isInfinite) {
            setCurrentItem(1, false)
            1
        } else {
            setCurrentItem(0, false)
            0
        }
    }

    fun setInterval(interval: Int) {
        this.interval = interval
        resetAutoScroll()
    }

    private fun resetAutoScroll() {
        pauseAutoScroll()
        resumeAutoScroll()
    }

    private fun getRealPosition(position: Int): Int {
        if (!isInfinite || adapter == null) return position
        return if (position == 0) {
            adapter!!.count - 1 - 2 //First item is a dummy of last item
        } else if (position > adapter!!.count - 2) {
            0 //Last item is a dummy of first item
        } else {
            position - 1
        }
    }
}