package com.example.imilipocket.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imilipocket.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardData(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0
)

class DashboardViewModel(private val financeViewModel: FinanceViewModel) : ViewModel() {
    private val _dashboardData = MutableStateFlow(DashboardData())
    val dashboardData: StateFlow<DashboardData> = _dashboardData
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) // 0-based

    init {
        updateDashboardData(selectedMonth)
    }

    fun setSelectedMonth(month: Int) {
        selectedMonth = month
        updateDashboardData(month)
    }

    private fun updateDashboardData(month: Int) {
        viewModelScope.launch {
            financeViewModel.transactions.collectLatest { transactions ->
                val calendar = Calendar.getInstance()
                val filteredTransactions = transactions.filter {
                    calendar.time = it.date
                    calendar.get(Calendar.MONTH) == month
                }
                val income = filteredTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                val expense = filteredTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                val balance = income - expense
                _dashboardData.value = DashboardData(
                    income = income,
                    expense = expense,
                    balance = balance
                )
            }
        }
    }
}