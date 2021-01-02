package com.swnishan.materialdatetimepicker.timepicker.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.annotation.IntRange
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.swnishan.materialdatetimepicker.R
import com.swnishan.materialdatetimepicker.common.Utils
import com.swnishan.materialdatetimepicker.timepicker.TimePeriodAdapter
import com.swnishan.materialdatetimepicker.timepicker.TimePickerAdapter
import kotlinx.android.synthetic.main.view_time_picker.view.*
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import kotlin.math.absoluteValue

class MaterialTimePickerView: ConstraintLayout{

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(
        context,
        attributeSet,
        R.attr.materialTimePickerViewStyle
    )
    constructor(context: Context, attributeSet: AttributeSet?, defAttributeSet: Int) : super(
        context,
        attributeSet,
        defAttributeSet
    ){
        setCustomAttributes(attributeSet, defAttributeSet, R.style.Widget_MaterialTimePicker)
        setHours()
        toggleTimeTimePeriodView()
        initTimeSelectionView()
        scrollToTime()
    }

    init {
        inflate(context, R.layout.view_time_picker, this)
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setPadding(0,Utils.dimenToPx(context,R.dimen.time_picker_view_padding_top),0, Utils.dimenToPx(context,R.dimen.time_picker_view_padding_bottom))
    }

    private fun setCustomAttributes(
        attributeSet: AttributeSet?,
        defAttributeSet: Int,
        defStyle: Int
    ) {
        context.obtainStyledAttributes(
            attributeSet,
            R.styleable.MaterialTimePickerView,
            defAttributeSet,
            defStyle
        ).apply {
            val highlighterColor = this.getColor(
                R.styleable.MaterialTimePickerView_materialTimePickerHighlighterColor,
                ContextCompat.getColor(context, R.color.O100)
            )

            val highlighterHeight = this.getDimension(
                R.styleable.MaterialTimePickerView_materialTimePickerHighlighterHeight,
                60f
            )

            val background = this.getDrawable(
                R.styleable.MaterialTimePickerView_android_background
            )

            textAppearance = this.getResourceId(
                R.styleable.MaterialTimePickerView_materialTimePickerTextAppearance,
                R.style.TextAppearance_MaterialTimePicker
            )

            timeConvention = TimeConvention.values()[this.getInt(
                R.styleable.MaterialTimePickerView_materialTimePickerTimeConvention,
                0
            )]

            viewCenter.setBackgroundColor(highlighterColor)
            viewCenter.layoutParams.height=highlighterHeight.toInt()
            TextViewCompat.setTextAppearance(tvHourTimeSeparator, textAppearance)

            if (background is ColorDrawable) {
                val backgroundColor = background.color
                viewTopShadeHour.setBackgroundColor(backgroundColor)
                viewTopShadeMinute.setBackgroundColor(backgroundColor)
                viewTopShadeTimePeriod.setBackgroundColor(backgroundColor)
                viewBottomShadeHour.setBackgroundColor(backgroundColor)
                viewBottomShadeMinute.setBackgroundColor(backgroundColor)
                viewBottomShadeTimePeriod.setBackgroundColor(backgroundColor)
                setBackgroundColor(backgroundColor)
            }

        }.recycle()
    }

    private var textAppearance:Int= R.style.TextAppearance_MaterialTimePicker
    private val hours24 = (0..23).toList()
    private val hours12 = (1..12).toList()
    private val minute = (0..59).toList()

    private val hourAdapter = TimePickerAdapter(hours24, textAppearance)
    private val minuteAdapter = TimePickerAdapter(minute, textAppearance)

    private val hourSnapHelper = LinearSnapHelper()
    private val minuteSnapHelper = LinearSnapHelper()
    private val timePeriodSnapHelper = LinearSnapHelper()

    private var pickerTime: LocalTime = LocalTime.now()
    private var onTimePickedListener: OnTimePickedListener? = null
    private var timeConvention: TimeConvention=TimeConvention.HOURS_24

    internal fun setTimeConvention(timeConvention: TimeConvention){
        this.timeConvention=timeConvention
        setHours()
        toggleTimeTimePeriodView()
        scrollToTime()
    }

    internal fun onTimePicked() {
        onTimePickedListener?.onTimePicked(
            OffsetDateTime.now().withHour(getHour()).withMinute(getMinute()).withSecond(0)
                .withNano(0).toInstant().toEpochMilli()
        )
    }

    fun getHour():Int{
        val hourView=hourSnapHelper.findSnapView(rvHours.layoutManager)?:return 0
        val hour=rvHours.getChildAdapterPosition(hourView)
        return when(timeConvention){
            TimeConvention.HOURS_24 -> hour % hours24.size
            TimeConvention.HOURS_12 -> {
                return (hour % hours12.size) + if (getTimePeriod() == TimePeriod.PM) 12 else 0
            }
        }
    }

    fun getMinute(): Int {
        val minuteView=hourSnapHelper.findSnapView(rvMinute.layoutManager)?:return 0
        return rvMinute.getChildAdapterPosition(minuteView)%minute.size
    }

    private fun getTimePeriod(): TimePeriod {
        val timePeriodView = hourSnapHelper.findSnapView(rvTimePeriod.layoutManager) ?: return TimePeriod.PM
        val timePeriodPosition = rvTimePeriod.getChildAdapterPosition(timePeriodView)
        return TimePeriod.values()[timePeriodPosition]
    }

    private fun setHours(){
        val hours = when (timeConvention) {
            TimeConvention.HOURS_24 -> hours24
            TimeConvention.HOURS_12 -> hours12
        }
        hourAdapter.setTimes(hours)
    }

    private fun toggleTimeTimePeriodView(){
        rvTimePeriod.isVisible=timeConvention==TimeConvention.HOURS_12
    }

    private fun initTimeSelectionView() {
        rvHours.apply {
            setHasFixedSize(true)
            adapter = hourAdapter
            layoutManager = LinearLayoutManager(context)
            hourSnapHelper.attachToRecyclerView(this)
            addListeners()
        }

        rvMinute.apply {
            setHasFixedSize(true)
            adapter = minuteAdapter
            layoutManager = LinearLayoutManager(context)
            minuteSnapHelper.attachToRecyclerView(this)
            addListeners()
        }

        rvTimePeriod.apply {
            setHasFixedSize(true)
            adapter=TimePeriodAdapter(
                listOf(TimePeriod.AM.name, TimePeriod.PM.name),
                textAppearance
            )
            layoutManager=LinearLayoutManager(context)
            timePeriodSnapHelper.attachToRecyclerView(this)
            addListeners()
        }
    }

    private fun animateShadeView(view: View, duration: Long, alpha: Float): Boolean {
        when(view.id){
            R.id.rvHours -> {
                listOf(viewTopShadeHour, viewBottomShadeHour).forEach {
                    it.animate().alpha(alpha).setDuration(duration)
                        .withEndAction { it.alpha = alpha }.start()
                }
            }
            R.id.rvMinute -> {
                listOf(viewTopShadeMinute, viewBottomShadeMinute).forEach {
                    it.animate().alpha(alpha).setDuration(duration)
                        .withEndAction { it.alpha = alpha }.start()
                }
            }
            R.id.rvTimePeriod -> {
                listOf(viewTopShadeTimePeriod, viewBottomShadeTimePeriod).forEach {
                    it.animate().alpha(alpha).setDuration(duration)
                        .withEndAction { it.alpha = alpha }.start()
                }
            }
        }
        return true
    }

    private fun RecyclerView.addListeners(){
        addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == ACTION_DOWN) animateShadeView(rv, 300, .2f)
                else if (e.action == ACTION_UP && rv.scrollState == SCROLL_STATE_IDLE) animateShadeView(
                    rv,
                    1000,
                    .7f
                )
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    SCROLL_STATE_DRAGGING -> animateShadeView(recyclerView, 300, .3f)
                    SCROLL_STATE_IDLE -> animateShadeView(recyclerView, 1000, .7f)
                }
            }
        })
    }

    private fun scrollToTime() {
        var scrollPosition= getScrollPosition(getHoursBasedOnClockType().size, pickerTime.hour)
        if(timeConvention==TimeConvention.HOURS_12)scrollPosition-=1
        rvHours.scrollToPosition(scrollPosition)
        rvMinute.scrollToPosition(getScrollPosition(minute.size, pickerTime.minute))
        val position = if(pickerTime.hour>=12) 1 else 0
        rvTimePeriod.scrollToPosition(position)
    }

    private fun getHoursBasedOnClockType(): List<Int> {
        return when(timeConvention){
            TimeConvention.HOURS_24 -> hours24
            TimeConvention.HOURS_12 -> hours12
        }
    }

    /**
     * Here we get the scroll position with relative to middle position of list of items
     * since we set the adapter count as Int.MAX_VALUE
     */
    private fun getScrollPosition(listSize: Int, time: Int): Int {
        var scrollPosition = Int.MAX_VALUE / 2
        val position = scrollPosition % listSize

        val diff = (time - position).absoluteValue
        if (time > position) scrollPosition += diff else scrollPosition -= diff

        return scrollPosition
    }

    fun setOnTimePickedListener(onTimePickedListener: OnTimePickedListener?) {
        this.onTimePickedListener = onTimePickedListener
    }

    fun setTime(hour: Int, @IntRange(from = 0, to = 60) minute: Int) {
        val setHour=when(timeConvention){
            TimeConvention.HOURS_24->hour
            TimeConvention.HOURS_12->hour%12+if (getTimePeriod() == TimePeriod.PM) 12 else 0
        }
        pickerTime = pickerTime.withHour(setHour).withMinute(minute)
        scrollToTime()
    }

    interface OnTimePickedListener {
        fun onTimePicked(time: Long)
    }

    enum class TimeConvention{
        HOURS_24, HOURS_12
    }

    enum class TimePeriod{
        AM,PM
    }
}