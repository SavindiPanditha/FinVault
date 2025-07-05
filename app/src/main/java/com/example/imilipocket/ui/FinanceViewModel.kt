package com.example.imilipocket.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.imilipocket.data.AppDatabase
import com.example.imilipocket.data.FinanceRepository
import com.example.imilipocket.model.BudgetEntity
import com.example.imilipocket.model.CategoryEntity
import com.example.imilipocket.model.CurrencyEntity
import com.example.imilipocket.model.TransactionEntity
import com.example.imilipocket.model.TransactionType
import com.example.imilipocket.work.ExpenseReminderWorker
import com.example.imilipocket.work.ReminderWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository
    val transactions: Flow<List<TransactionEntity>>
    val budgets: Flow<List<BudgetEntity>>
    val categories: Flow<List<CategoryEntity>>
    val currencies: Flow<List<CurrencyEntity>>
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(
            database.transactionDao(),
            database.budgetDao(),
            database.categoryDao(),
            database.currencyDao()
        )
        transactions = repository.getAllTransactions()
        budgets = repository.getAllBudgets()
        categories = repository.getAllCategories()
        currencies = repository.getAllCurrencies()
        // Initialize categories and default currency
        viewModelScope.launch {
            initializeCategories()
            getDefaultCurrency()
        }
    }

    fun insertTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        try {
            repository.insertTransaction(transaction)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to insert transaction: ${e.message}"
        }
    }

    fun updateTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        try {
            repository.updateTransaction(transaction)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to update transaction: ${e.message}"
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        try {
            repository.deleteTransaction(transaction)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to delete transaction: ${e.message}"
        }
    }

    fun getTransactionById(id: Int): Flow<TransactionEntity?> = repository.getTransactionById(id)

    fun insertBudget(budget: BudgetEntity) = viewModelScope.launch {
        try {
            repository.insertBudget(budget)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to insert budget: ${e.message}"
        }
    }

    fun updateBudget(budget: BudgetEntity) = viewModelScope.launch {
        try {
            repository.updateBudget(budget)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to update budget: ${e.message}"
        }
    }

    fun deleteBudget(budget: BudgetEntity) = viewModelScope.launch {
        try {
            repository.deleteBudget(budget)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to delete budget: ${e.message}"
        }
    }

    fun getBudgetByCategory(categoryId: Int, month: Int, year: Int): Flow<BudgetEntity?> =
        repository.getBudgetByCategory(categoryId, month, year)

    fun initializeCategories() = viewModelScope.launch {
        try {
            repository.initializeCategories()
        } catch (e: Exception) {
            _errorMessage.value = "Failed to initialize categories: ${e.message}"
        }
    }

    fun insertCurrency(currency: CurrencyEntity) = viewModelScope.launch {
        try {
            repository.insertCurrency(currency)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to insert currency: ${e.message}"
        }
    }

    fun getDefaultCurrency() = viewModelScope.launch {
        try {
            val currency = repository.getDefaultCurrencySync()
            if (currency == null) {
                repository.insertCurrency(CurrencyEntity(code = "LKR", isDefault = true))
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to get default currency: ${e.message}"
        }
    }

    fun getCurrency(): Flow<CurrencyEntity?> = repository.getDefaultCurrency()

    fun saveCurrency(code: String) = viewModelScope.launch {
        try {
            currencies.collect { currencyList ->
                currencyList.forEach { currency ->
                    if (currency.isDefault) {
                        repository.insertCurrency(currency.copy(isDefault = false))
                    }
                }
            }
            repository.insertCurrency(CurrencyEntity(code = code, isDefault = true))
        } catch (e: Exception) {
            _errorMessage.value = "Failed to save currency: ${e.message}"
        }
    }

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = repository.getCategoriesByType(type)

    fun clearAllData() = viewModelScope.launch {
        try {
            repository.clearAllData()
            _errorMessage.value = "All data cleared successfully"
        } catch (e: Exception) {
            _errorMessage.value = "Failed to clear data: ${e.message}"
        }
    }

    fun exportData(): String {
        val data = mutableMapOf<String, Any>()
        viewModelScope.launch {
            transactions.collect { transactionList ->
                data["transactions"] = transactionList.map {
                    mapOf(
                        "id" to it.id,
                        "amount" to it.amount,
                        "type" to it.type.name,
                        "categoryId" to it.categoryId,
                        "date" to it.date.time,
                        "note" to (it.note ?: ""),
                        "currencyId" to it.currencyId
                    )
                }
            }
            budgets.collect { data["budgets"] = it }
            categories.collect { data["categories"] = it }
            currencies.collect { data["currencies"] = it }
        }
        return Gson().toJson(data)
    }

    fun exportDataToFile(context: Context): String {
        try {
            val json = exportData()
            context.openFileOutput("finance_backup.json", Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
            return json
        } catch (e: Exception) {
            _errorMessage.value = "Failed to export data: ${e.message}"
            return ""
        }
    }

    fun restoreData(json: String) = viewModelScope.launch {
        try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = Gson().fromJson(json, type)
            (data["transactions"] as? List<Map<String, Any>>)?.forEach { t ->
                repository.insertTransaction(
                    TransactionEntity(
                        id = (t["id"] as? Double)?.toInt() ?: 0,
                        amount = (t["amount"] as? Double) ?: 0.0,
                        type = t["type"]?.let { TransactionType.valueOf(it as String) } ?: TransactionType.EXPENSE,
                        categoryId = (t["categoryId"] as? Double)?.toInt() ?: 0,
                        date = t["date"]?.let { Date((it as Double).toLong()) } ?: Date(),
                        note = t["note"] as? String,
                        currencyId = (t["currencyId"] as? Double)?.toInt() ?: 0
                    )
                )
            }
            (data["budgets"] as? List<Map<String, Any>>)?.forEach { b ->
                repository.insertBudget(
                    BudgetEntity(
                        id = (b["id"] as? Double)?.toInt() ?: 0,
                        categoryId = (b["categoryId"] as? Double)?.toInt() ?: 0,
                        amount = (b["amount"] as? Double) ?: 0.0,
                        month = (b["month"] as? Double)?.toInt() ?: 0,
                        year = (b["year"] as? Double)?.toInt() ?: 0
                    )
                )
            }
            (data["categories"] as? List<Map<String, Any>>)?.forEach { c ->
                repository.insertCategory(
                    CategoryEntity(
                        id = (c["id"] as? Double)?.toInt() ?: 0,
                        name = c["name"] as? String ?: "",
                        type = c["type"] as? String ?: "EXPENSE"
                    )
                )
            }
            (data["currencies"] as? List<Map<String, Any>>)?.forEach { c ->
                repository.insertCurrency(
                    CurrencyEntity(
                        id = (c["id"] as? Double)?.toInt() ?: 0,
                        code = c["code"] as? String ?: "",
                        isDefault = c["isDefault"] as? Boolean ?: false
                    )
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to restore data: ${e.message}"
        }
    }

    fun restoreDataFromFile(context: Context) = viewModelScope.launch {
        try {
            val json = context.openFileInput("finance_backup.json").bufferedReader().use { it.readText() }
            restoreData(json)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to restore data: ${e.message}"
        }
    }

    fun scheduleDailyReminder() {
        val budgetRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(20 - System.currentTimeMillis() / (1000 * 60 * 60) % 24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            "daily_budget_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            budgetRequest
        )

        val expenseRequest = PeriodicWorkRequestBuilder<ExpenseReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(18 - System.currentTimeMillis() / (1000 * 60 * 60) % 24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            "daily_expense_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            expenseRequest
        )
    }
}