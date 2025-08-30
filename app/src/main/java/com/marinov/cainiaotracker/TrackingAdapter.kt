package com.marinov.cainiaotracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView

enum class AdapterMode {
    HOME, ARCHIVED
}

class TrackingAdapter(
    private var items: List<TrackingItem>,
    private val mode: AdapterMode,
    private val onArchiveButtonClick: (TrackingItem) -> Unit,
    private val onDeleteClick: (TrackingItem) -> Unit,
    private val onUrlClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<TrackingAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: AppCompatTextView = itemView.findViewById(R.id.tv_title)
        val tvCode: AppCompatTextView = itemView.findViewById(R.id.tv_tracking_code)
        val btnArchive: ImageButton = itemView.findViewById(R.id.btn_archive)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tracking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.name
        holder.tvCode.text = item.code

        holder.btnDelete.setOnClickListener {
            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                onDeleteClick(items[holder.adapterPosition])
            }
        }
        holder.btnArchive.setOnClickListener {
            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                onArchiveButtonClick(items[holder.adapterPosition])
            }
        }

        when (mode) {
            AdapterMode.HOME -> {
                holder.btnArchive.setImageResource(R.drawable.ic_button_archive)
                holder.itemView.setOnClickListener {
                    onUrlClick?.invoke(item.code)
                }
            }
            AdapterMode.ARCHIVED -> {
                holder.btnArchive.setImageResource(R.drawable.ic_unarchive)
                holder.itemView.setOnClickListener {
                    Toast.makeText(holder.itemView.context, "Para rastrear a encomenda, desarquive-a.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<TrackingItem>) {
        items = newList
        notifyDataSetChanged()
    }
}

