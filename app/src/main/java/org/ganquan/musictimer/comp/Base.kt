package org.ganquan.musictimer.comp

import android.view.ViewGroup

fun ViewGroup.setChildrenVisibility(visibility: Int, id: Int? = null) {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if(child is ViewGroup) child.setChildrenVisibility(visibility, id)
        if ((id == null || child.id == id) && child.visibility != visibility)
            child.visibility = visibility
    }
}