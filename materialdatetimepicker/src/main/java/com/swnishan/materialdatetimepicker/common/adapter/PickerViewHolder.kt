package com.swnishan.materialdatetimepicker.common.adapter

import android.view.View
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.swnishan.materialdatetimepicker.common.PickerModel


internal class PickerViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    fun bind(model: PickerModel, textAppearance: Int) {
        (view as TextView).text = model.displayValue
        TextViewCompat.setTextAppearance(view, textAppearance)
    }
}