package com.example.imilipocket.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.imilipocket.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider


class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed outside of view lifecycle")
    private val financeViewModel: FinanceViewModel by viewModels()
    private val viewModel: DashboardViewModel by viewModels { DashboardViewModelFactory(financeViewModel) }
    private val TAG = "DashboardFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinner()
        observeData()
    }

    private fun setupSpinner() {
        val months = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = adapter
        // Set current month
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        binding.spinnerMonth.setSelection(currentMonth)
        // Add listener
        binding.spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                viewModel.setSelectedMonth(position) // 0-based month
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dashboardData.collectLatest { data ->
                    try {
                        updateDashboard(data)
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Cannot update dashboard: view binding is null", e)
                    }
                }
            }
        }
    }

    private fun updateDashboard(data: DashboardData) {
        if (_binding == null) {
            Log.w(TAG, "updateDashboard: Binding is null, skipping UI update")
            return
        }
        binding.tvIncome.text = "Income: ${String.format("%.2f", data.income)}"
        binding.tvExpense.text = "Expense: ${String.format("%.2f", data.expense)}"
        binding.tvBalance.text = "Balance: ${String.format("%.2f", data.balance)}"

        // Setup PieChart for income
        val incomeEntries = listOf(
            PieEntry(data.income.toFloat(), "Income"),
            PieEntry(maxOf(0f, (data.balance - data.income).toFloat()), "Remaining")
        )
        val incomeDataSet = PieDataSet(incomeEntries, "Income")
        incomeDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        binding.pieChartIncome.data = PieData(incomeDataSet)
        binding.pieChartIncome.description.isEnabled = false
        binding.pieChartIncome.invalidate()

        // Setup PieChart for expense
        val expenseEntries = listOf(
            PieEntry(data.expense.toFloat(), "Expense"),
            PieEntry(maxOf(0f, (data.balance - data.expense).toFloat()), "Remaining")
        )
        val expenseDataSet = PieDataSet(expenseEntries, "Expense")
        expenseDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        binding.pieChartExpense.data = PieData(expenseDataSet)
        binding.pieChartExpense.description.isEnabled = false
        binding.pieChartExpense.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class DashboardViewModelFactory(private val financeViewModel: FinanceViewModel) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(financeViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}