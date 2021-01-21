package com.swnishan.materialdatetimepicker.timepicker

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.swnishan.materialdatetimepicker.R
import com.swnishan.materialdatetimepicker.common.SlowLinearLayoutManager
import com.swnishan.materialdatetimepicker.common.Utils
import com.swnishan.materialdatetimepicker.common.adapter.PickerAdapter
import com.swnishan.materialdatetimepicker.common.view.BaseMaterialDateTimePickerView
import kotlinx.android.synthetic.main.view_time_picker.view.*
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import kotlin.math.min

class MaterialTimePickerView: BaseMaterialDateTimePickerView{

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
        updateHoursAdapter()
        toggleTimePeriodView()
        initTimeSelectionView()
        scrollToTime()
    }

    init {
        inflate(context, R.layout.view_time_picker, this)
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setPadding(0,Utils.dimenToPx(context,R.dimen.date_picker_view_padding_top),0, Utils.dimenToPx(context,R.dimen.date_picker_view_padding_bottom))
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
                R.styleable.MaterialTimePickerView_highlighterColor,
                ContextCompat.getColor(context, R.color.O100)
            )

            val highlighterHeight = this.getDimension(
                R.styleable.MaterialTimePickerView_highlighterHeight,
                50f
            )

            val background = this.getDrawable(
                R.styleable.MaterialTimePickerView_android_background
            )

            textAppearance = this.getResourceId(
                R.styleable.MaterialTimePickerView_android_textAppearance,
                R.style.TextAppearance_MaterialTimePicker
            )

            timeConvention = TimeConvention.values()[this.getInt(
                R.styleable.MaterialTimePickerView_timeConvention,
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
    private val hours24 = (0..23).mapIndexed { index, value -> TimeModel.Hour(index, String.format("%02d", value), value) }
    private val hours12 = (1..12).mapIndexed { index, value -> TimeModel.Hour(index, String.format("%02d", value), value) }
    private val minutes = (0..59).mapIndexed { index, value -> TimeModel.Minute(index, String.format("%02d", value), value) }

    private val hourSnapHelper = LinearSnapHelper()
    private val minuteSnapHelper = LinearSnapHelper()
    private val timePeriodSnapHelper = LinearSnapHelper()

    private val hourAdapter = PickerAdapter(hours24, textAppearance){position-> onItemClicked(position, rvHours) }
    private val minuteAdapter = PickerAdapter(minutes, textAppearance){position-> onItemClicked(position, rvMinute) }

    private var pickerTime: LocalTime = LocalTime.now().withHour(23)
    private var onTimePickedListener: OnTimePickedListener? = null
    private var timeConvention: TimeConvention = TimeConvention.HOURS_24
    private var timePeriod: TimePeriod = if(pickerTime.hour>=12) TimePeriod.PM else TimePeriod.AM

    internal fun setTimeConvention(timeConvention: TimeConvention){
        this.timeConvention=timeConvention
        updateHoursAdapter()
        toggleTimePeriodView()
        scrollToTime()
    }

    internal fun onTimePicked() {
        onTimePickedListener?.onTimePicked(getTime())
    }

    fun getHour():Int{
        val position=hourSnapHelper.getSnapPosition(rvHours)
        return when(timeConvention){
            TimeConvention.HOURS_24 -> position % hours24.size
            TimeConvention.HOURS_12 -> {
                return (hourAdapter.getModelAtPosition(position) as TimeModel.Hour).hour + if (getTimePeriod() == TimePeriod.PM) 12 else 0
            }
        }
    }

    fun getMinute(): Int = minuteSnapHelper.getSnapPosition(rvMinute)%minutes.size

    fun getTimePeriod(): TimePeriod = TimePeriod.values()[timePeriodSnapHelper.getSnapPosition(rvTimePeriod)]

    fun getTime()=OffsetDateTime.now().withHour(pickerTime.hour).withMinute(pickerTime.minute).withSecond(0)
        .withNano(0).toInstant().toEpochMilli()

    private fun updateHoursAdapter(){
        val hours = when (timeConvention) {
            TimeConvention.HOURS_24 -> hours24
            TimeConvention.HOURS_12 -> hours12
        }
        hourAdapter.updateItems(hours)
    }

    private fun toggleTimePeriodView(){
        rvTimePeriod.isVisible=timeConvention== TimeConvention.HOURS_12
    }

    private fun initTimeSelectionView() {
        rvHours.apply {
            setHasFixedSize(true)
            adapter = hourAdapter
            layoutManager = SlowLinearLayoutManager(context)
            hourSnapHelper.attachToRecyclerView(this)
            addListeners()
        }

        rvMinute.apply {
            setHasFixedSize(true)
            adapter = minuteAdapter
            layoutManager = SlowLinearLayoutManager(context)
            minuteSnapHelper.attachToRecyclerView(this)
            addListeners()
        }

        rvTimePeriod.apply {
            setHasFixedSize(true)
            adapter= PickerAdapter(
                listOf(TimePeriod.AM, TimePeriod.PM).mapIndexed { index, timePeriod -> TimeModel.TimePeriod(index, timePeriod.name, timePeriod.ordinal) },
                textAppearance,
                PickerAdapter.ScrollOptions.SCROLL_ITEM_LIMIT
            ){position-> onItemClicked(position, rvTimePeriod) }
            layoutManager=SlowLinearLayoutManager(context)
            timePeriodSnapHelper.attachToRecyclerView(this)
            addListeners()
        }
    }

    private fun animateShadeView(view: View, duration: Long, alpha: Float) {
        when(view.id){
            R.id.rvHours -> super.animateShadeView(listOf(viewTopShadeHour, viewBottomShadeHour),duration, alpha)
            R.id.rvMinute -> super.animateShadeView(listOf(viewTopShadeMinute, viewBottomShadeMinute),duration, alpha)
            R.id.rvTimePeriod -> super.animateShadeView(listOf(viewTopShadeTimePeriod, viewBottomShadeTimePeriod),duration, alpha)
        }
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

                if(newState==SCROLL_STATE_IDLE){
                    when(recyclerView.id){
                        R.id.rvHours-> pickerTime=pickerTime.withHour(getHour())
                        R.id.rvMinute-> pickerTime=pickerTime.withMinute(getMinute())
                        R.id.rvTimePeriod-> timePeriod=getTimePeriod()
                    }
                }
            }
        })
    }

    private fun scrollToTime() {
        scrollToHour()
        scrollToMinute()
        scrollToTimePeriod()
    }

    private fun scrollToHour(){
        val scrollPosition= getScrollPosition(hourAdapter,getHoursBasedOnClockType(),getHourModel(pickerTime.hour))
        rvHours.scrollToPosition(scrollPosition)
    }

    private fun scrollToMinute(){
        rvMinute.scrollToPosition(getScrollPosition(minuteAdapter, minutes, getMinuteModel(pickerTime.minute)))
    }

    private fun scrollToTimePeriod(){
        val position = if(timePeriod==TimePeriod.PM) 1 else 0
        rvTimePeriod.scrollToPosition(position)
    }

    private fun getHoursBasedOnClockType(): List<TimeModel.Hour> {
        return when(timeConvention){
            TimeConvention.HOURS_24 -> hours24
            TimeConvention.HOURS_12 -> hours12
        }
    }

    private fun getHourModel(hour: Int)=when(timeConvention){
        TimeConvention.HOURS_24 -> hours24.firstOrNull { it.hour == hour % 24 }
            ?: throw ArrayIndexOutOfBoundsException("Cannot find given Hour in given 24 hours range (size: ${hours24.size} index: $hour)")
        TimeConvention.HOURS_12 -> hours12.firstOrNull { it.hour == if(hour==0 || hour==12) 12 else hour % 12 }
            ?: throw ArrayIndexOutOfBoundsException("Cannot find given Hour in given 12 hours range (size: ${hours12.size} index: $hour)")
    }

    private fun getMinuteModel(minute: Int) = minutes.firstOrNull { it.minute == minute }
        ?: throw ArrayIndexOutOfBoundsException("Cannot find given Minute in given minutes range (size: ${minutes.size} index: $minute)")

    fun setOnTimePickedListener(onTimePickedListener: OnTimePickedListener?) {
        this.onTimePickedListener = onTimePickedListener
    }

    fun setHour(@IntRange(from =0 ,to=23)hour: Int){
        setTimePeriod(if(hour>=12)TimePeriod.PM else TimePeriod.AM)
        val setHour=when(timeConvention){
            TimeConvention.HOURS_24 ->hour
            TimeConvention.HOURS_12 ->hour%12+if (timePeriod == TimePeriod.PM) 12 else 0
        }
        pickerTime = pickerTime.withHour(setHour)
        scrollToHour()
    }

    fun setMinute(@IntRange(from = 0, to = 60) minute: Int) {
        pickerTime = pickerTime.withMinute(minute)
        scrollToMinute()
    }

    fun setTimePeriod(timePeriod: TimePeriod){
        if (timePeriod == this.timePeriod) return
        this.timePeriod = timePeriod
        pickerTime=when{
            pickerTime.hour < 12 && timePeriod == TimePeriod.PM->pickerTime.plusHours(12)
            pickerTime.hour >= 12 && timePeriod == TimePeriod.AM->pickerTime.minusHours(12)
            else->pickerTime
        }
        scrollToTimePeriod()
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