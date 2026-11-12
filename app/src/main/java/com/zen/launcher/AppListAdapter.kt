package com.zen.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zen.launcher.databinding.ItemAppBinding

class AppListAdapter(
    private val onClick: (AppEntry, View) -> Unit,
    private val onLongClick: ((AppEntry, View) -> Unit)? = null,
) : ListAdapter<AppEntry, AppListAdapter.Holder>(Diff) {

    class Holder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = getItem(position)
        holder.binding.root.apply {
            text = app.label
            setOnClickListener { onClick(app, it) }
            setOnLongClickListener(onLongClick?.let { l -> View.OnLongClickListener { l(app, it); true } })
        }
    }

    private object Diff : DiffUtil.ItemCallback<AppEntry>() {
        override fun areItemsTheSame(a: AppEntry, b: AppEntry) = a.key == b.key
        override fun areContentsTheSame(a: AppEntry, b: AppEntry) = a == b
    }
}
