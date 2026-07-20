package com.narrativedj.app

import android.view.View
import android.widget.AdapterView

internal fun AdapterView<*>.setOnItemSelectedListenerCompat(onSelected: (Int) -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onSelected(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }
}
