package com.not_example.network_1ch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.not_example.network_1ch.databinding.MessageBinding

class MessageAdapter : ListAdapter<MessageUiData, MessageAdapter.MessageViewHolder>(MESSAGES_COMPARATOR) {
    companion object {
        private val MESSAGES_COMPARATOR = object : DiffUtil.ItemCallback<MessageUiData>() {
            override fun areItemsTheSame(oldItem: MessageUiData, newItem: MessageUiData): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: MessageUiData, newItem: MessageUiData): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }

    data class MessageViewHolder(
        private val binding: MessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: MessageUiData) {
            binding.message = msg
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder(
            MessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun idByPosition(position: Int): Long {
        return getItem(position).id
    }
}