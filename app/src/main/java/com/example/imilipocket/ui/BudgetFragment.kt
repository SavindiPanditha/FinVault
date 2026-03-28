package com.example.imilipocket.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.imilipocket.databinding.FragmentBudgetBinding
import com.example.imilipocket.model.BudgetEntity
import com.example.imilipocket.model.CategoryEntity
import com.example.imilipocket.model.TransactionType
import com.example.imilipocket.ui.adapter.BudgetAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private var expenseCategories: List<CategoryEntity> = emptyList()

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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getCategoriesByType(TransactionType.EXPENSE.name).collectLatest { categories ->
                    expenseCategories = categories
                    binding.spinnerBudgetCategory.adapter = ArrayAdapter(
                        requireContext(), android.R.layout.simple_spinner_item,
                        categories.map { it.name }
                    ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                    budgetAdapter.setCategories(categories)
                }
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
            val index = binding.spinnerBudgetCategory.selectedItemPosition
            if (index < 0 || index >= expenseCategories.size) {
                Toast.makeText(context, "Select a valid category", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val categoryId = expenseCategories[index].id
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)

            val existingBudget = viewModel.getBudgetByCategory(categoryId, month, year).first()
            if (existingBudget != null) {
                Toast.makeText(context, "Budget already exists for this category and month", Toast.LENGTH_SHORT).show()
                return@launch
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

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.budgets,
                    viewModel.transactions,
                    viewModel.getCategoriesByType(TransactionType.EXPENSE.name)
                ) { budgets, transactions, categories ->
                    Triple(budgets, transactions, categories)
                }.collectLatest { (budgets, transactions, categories) ->
                    val now = Calendar.getInstance()
                    val currentMonth = now.get(Calendar.MONTH) + 1
                    val currentYear = now.get(Calendar.YEAR)

                    val spentAmounts = budgets.associate { budget ->
                        val spent = transactions.filter { transaction ->
                            val transactionCalendar = Calendar.getInstance().apply { time = transaction.date }
                            transaction.categoryId == budget.categoryId &&
                                transactionCalendar.get(Calendar.MONTH) + 1 == currentMonth &&
                                transactionCalendar.get(Calendar.YEAR) == currentYear
                        }.sumOf { it.amount }
                        budget.id to spent
                    }

                    budgetAdapter = BudgetAdapter(
                        onDelete = { budget -> viewModel.deleteBudget(budget) },
                        currentMonth = currentMonth,
                        currentYear = currentYear,
                        spentAmounts = spentAmounts
                    )
                    binding.rvBudgets.adapter = budgetAdapter
                    budgetAdapter.setCategories(categories)
                    budgetAdapter.submitList(budgets)
                }
            }
        }
    }
}