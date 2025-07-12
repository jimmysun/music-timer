package org.ganquan.musictimer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.ganquan.musictimer.tools.Utils
import org.ganquan.musictimer.tools.Utils.Companion.int2String

private const val SHARED_PREFER_KEY: String = "normalTimeList"

class NormalTimeAdapter(private val items: MutableList<MutableList<Int>>) : RecyclerView.Adapter<MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        Utils.sharedPrefer(parent.context, SHARED_PREFER_KEY, items)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.normal_time_list_layout, parent, false)
        return MyViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        var startH: Int = items[position][0]
        var startM: Int = items[position][1]
        var endH: Int = startH
        var endM: Int = startM+items[position][2]
        if(endM > 60) {
            endH += 1
            endM -= 60
        }
        holder.normalTime.text = "${int2String(startH)}:${int2String(startM)} - ${int2String(startH)}:${int2String(endM)}"

        holder.deleteButton.visibility = items[position][3]
        holder.deleteButton.setOnClickListener {
            items.removeAt(position)

            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size - position)
        }
    }

    override fun getItemCount(): Int = items.size

    companion object {
        val sharedPreferKey = SHARED_PREFER_KEY
    }
}

class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val deleteButton: ImageButton = itemView.findViewById(R.id.normal_time_del)
    val normalTime: TextView = itemView.findViewById(R.id.normal_time)
}