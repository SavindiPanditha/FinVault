package com.example.imilipocket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.imilipocket.databinding.ItemTransactionBinding
import com.example.imilipocket.model.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val onEdit: (TransactionEntity) -> Unit,
    private val onDelete: (TransactionEntity) -> Unit,
    private val categories: List<com.example.imilipocket.model.CategoryEntity>,
    private val currencies: List<com.example.imilipocket.model.CurrencyEntity>
) : ListAdapter<TransactionEntity, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: TransactionEntity) {
            val category = categories.find { it.id == transaction.categoryId }
            val currency = currencies.find { it.id == transaction.currencyId }
            binding.tvAmount.text = String.format("%.2f %s", transaction.amount, currency?.code ?: "Unknown")
            binding.tvCategory.text = category?.name ?: "Unknown"
            binding.tvDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(transaction.date)
            binding.btnEdit.setOnClickListener { onEdit(transaction) }
            binding.btnDelete.setOnClickListener { onDelete(transaction) }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
            return oldItem == newItem
        }
    }
}