package com.geronimo.geki

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class NonScrollingLinearLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun canScrollVertically(): Boolean {
        return false
    }

    override fun canScrollHorizontally(): Boolean {
        return false
    }
}


