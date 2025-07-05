package com.example.imilipocket.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.imilipocket.databinding.FragmentBudgetBinding
import com.example.imilipocket.model.BudgetEntity
import com.example.imilipocket.model.TransactionType
import com.example.imilipocket.ui.adapter.BudgetAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by viewModels()
    private lateinit var budgetAdapter: BudgetAdapter
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        setupRecyclerView()
        setupCategorySpinner()
        updateMonthYear()
        binding.btnAddBudget.setOnClickListener { addBudget() }
        observeData()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        budgetAdapter = BudgetAdapter(
            onDelete = { budget -> viewModel.deleteBudget(budget) },
            currentMonth = calendar.get(Calendar.MONTH) + 1,
            currentYear = calendar.get(Calendar.YEAR),
            spentAmounts = emptyMap() // Initial empty map
        )
        binding.rvBudgets.layoutManager = LinearLayoutManager(context)
        binding.rvBudgets.adapter = budgetAdapter
    }

    private fun setupCategorySpinner() {
        lifecycleScope.launch {
            viewModel.getCategoriesByType(TransactionType.EXPENSE.name).collectLatest { categories ->
                binding.spinnerBudgetCategory.adapter = ArrayAdapter(
                    requireContext(), android.R.layout.simple_spinner_item,
                    categories.map { it.name }
                ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                budgetAdapter.setCategories(categories)
            }
        }
    }

    private fun updateMonthYear() {
        binding.tvMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    private fun addBudget() {
        val amount = binding.etBudgetAmount.text.toString().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            viewModel.getCategoriesByType(TransactionType.EXPENSE.name).collectLatest { categories ->
                val index = binding.spinnerBudgetCategory.selectedItemPosition
                if (index >= categories.size) {
                    Toast.makeText(context, "Select a valid category", Toast.LENGTH_SHORT).show()
                    return@collectLatest
                }

                val categoryId = categories[index].id
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)

                viewModel.getBudgetByCategory(categoryId, month, year).collectLatest { existingBudget ->
                    if (existingBudget != null) {
                        Toast.makeText(context, "Budget already exists for this category and month", Toast.LENGTH_SHORT).show()
                        return@collectLatest
                    }

                    viewModel.insertBudget(
                        BudgetEntity(
                            categoryId = categoryId,
                            amount = amount,
                            month = month,
                            year = year
                        )
                    )
                    binding.etBudgetAmount.text.clear()
                }
            }
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.budgets.combine(viewModel.transactions) { budgets, transactions ->
                Pair(budgets, transactions)
            }.collectLatest { (budgets, transactions) ->
                val spentAmounts = budgets.associate { budget ->
                    val spent = transactions.filter {
                        it.categoryId == budget.categoryId &&
                                it.date.month + 1 == calendar.get(Calendar.MONTH) + 1 &&
                                it.date.year + 1900 == calendar.get(Calendar.YEAR)
                    }.sumOf { it.amount }
                    budget.id to spent
                }
                budgetAdapter = BudgetAdapter(
                    onDelete = { budget -> viewModel.deleteBudget(budget) },
                    currentMonth = calendar.get(Calendar.MONTH) + 1,
                    currentYear = calendar.get(Calendar.YEAR),
                    spentAmounts = spentAmounts
                )
                binding.rvBudgets.adapter = budgetAdapter
                setupCategorySpinner() // Re-apply categories
                budgetAdapter.submitList(budgets)
            }
        }
    }
}