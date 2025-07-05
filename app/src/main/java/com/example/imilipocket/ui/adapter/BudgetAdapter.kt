package com.example.imilipocket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.imilipocket.databinding.ItemBudgetBinding
import com.example.imilipocket.model.BudgetEntity
import com.example.imilipocket.model.CategoryEntity

class BudgetAdapter(
    private val onDelete: (BudgetEntity) -> Unit,
    private val currentMonth: Int,
    private val currentYear: Int,
    private val spentAmounts: Map<Int, Double> // Map of budget ID to spent amount
) : ListAdapter<BudgetEntity, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {

    private var categories: List<CategoryEntity> = emptyList()

    class BudgetViewHolder(
        private val binding: ItemBudgetBinding,
        private val onDelete: (BudgetEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(budget: BudgetEntity, categoryName: String, spent: Double) {
            binding.tvCategory.text = categoryName
            binding.tvBudgetAmount.text = "Budget: ${String.format("%.2f", budget.amount)}"
            binding.tvSpentAmount.text = "Spent: ${String.format("%.2f", spent)}"
            val progress = if (budget.amount > 0) ((spent / budget.amount) * 100).toInt() else 0
            binding.progressBar.progress = progress.coerceIn(0, 100)
            binding.btnDelete.setOnClickListener { onDelete(budget) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BudgetViewHolder(binding, onDelete)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val budget = getItem(position)
        val category = categories.find { it.id == budget.categoryId } ?: return
        val spent = spentAmounts[budget.id] ?: 0.0
        holder.bind(budget, category.name, spent)
    }

    fun setCategories(categories: List<CategoryEntity>) {
        this.categories = categories
        notifyDataSetChanged()
    }

    class BudgetDiffCallback : DiffUtil.ItemCallback<BudgetEntity>() {
        override fun areItemsTheSame(oldItem: BudgetEntity, newItem: BudgetEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BudgetEntity, newItem: BudgetEntity): Boolean {
            return oldItem == newItem
        }
    }
}