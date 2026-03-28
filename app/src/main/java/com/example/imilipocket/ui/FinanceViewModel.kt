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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
            ensureDefaultCurrency()
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

    private suspend fun ensureDefaultCurrency() {
        try {
            val currency = repository.getDefaultCurrencySync()
            if (currency == null) {
                repository.setDefaultCurrency("LKR")
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to get default currency: ${e.message}"
        }
    }

    fun getCurrency(): Flow<CurrencyEntity?> = repository.getDefaultCurrency()

    fun saveCurrency(code: String) = viewModelScope.launch {
        try {
            if (code.isBlank()) {
                _errorMessage.value = "Currency code cannot be empty"
                return@launch
            }
            repository.setDefaultCurrency(code)
            _errorMessage.value = "Currency set to $code"
        } catch (e: Exception) {
            _errorMessage.value = "Failed to save currency: ${e.message}"
        }
    }

    suspend fun getDefaultCurrencyId(): Int {
        ensureDefaultCurrency()
        return repository.getDefaultCurrencySync()?.id
            ?: repository.getFirstCurrencySync()?.id
            ?: throw IllegalStateException("No currency available")
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

    suspend fun exportData(): String {
        val data = mutableMapOf<String, Any>()

        data["transactions"] = transactions.first().map {
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
        data["budgets"] = budgets.first()
        data["categories"] = categories.first()
        data["currencies"] = currencies.first()

        return Gson().toJson(data)
    }

    suspend fun exportDataToFile(context: Context): String {
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
            val data = Gson().fromJson(json, com.google.gson.JsonObject::class.java)

            data.getAsJsonArray("transactions")?.forEach { tElement ->
                val t = tElement.asJsonObject
                repository.insertTransaction(
                    TransactionEntity(
                        id = t.get("id")?.asInt ?: 0,
                        amount = t.get("amount")?.asDouble ?: 0.0,
                        type = t.get("type")?.asString?.let { TransactionType.valueOf(it) } ?: TransactionType.EXPENSE,
                        categoryId = t.get("categoryId")?.asInt ?: 0,
                        date = t.get("date")?.asLong?.let { Date(it) } ?: Date(),
                        note = if (t.has("note") && !t.get("note").isJsonNull) t.get("note").asString else null,
                        currencyId = t.get("currencyId")?.asInt ?: 0
                    )
                )
            }

            data.getAsJsonArray("budgets")?.forEach { bElement ->
                val b = bElement.asJsonObject
                repository.insertBudget(
                    BudgetEntity(
                        id = b.get("id")?.asInt ?: 0,
                        categoryId = b.get("categoryId")?.asInt ?: 0,
                        amount = b.get("amount")?.asDouble ?: 0.0,
                        month = b.get("month")?.asInt ?: 0,
                        year = b.get("year")?.asInt ?: 0
                    )
                )
            }

            data.getAsJsonArray("categories")?.forEach { cElement ->
                val c = cElement.asJsonObject
                repository.insertCategory(
                    CategoryEntity(
                        id = c.get("id")?.asInt ?: 0,
                        name = c.get("name")?.asString ?: "",
                        type = c.get("type")?.asString ?: "EXPENSE"
                    )
                )
            }

            data.getAsJsonArray("currencies")?.forEach { cElement ->
                val c = cElement.asJsonObject
                repository.insertCurrency(
                    CurrencyEntity(
                        id = c.get("id")?.asInt ?: 0,
                        code = c.get("code")?.asString ?: "",
                        isDefault = c.get("isDefault")?.asBoolean ?: false
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
        val budgetDelay = calculateInitialDelayHours(targetHour = 20)
        val budgetRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(budgetDelay, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            "daily_budget_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            budgetRequest
        )

        val expenseDelay = calculateInitialDelayHours(targetHour = 18)
        val expenseRequest = PeriodicWorkRequestBuilder<ExpenseReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(expenseDelay, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            "daily_expense_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            expenseRequest
        )
    }

    private fun calculateInitialDelayHours(targetHour: Int): Long {
        val now = java.util.Calendar.getInstance()
        val next = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            set(java.util.Calendar.HOUR_OF_DAY, targetHour)
            if (before(now)) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        val diffMillis = next.timeInMillis - now.timeInMillis
        val diffHours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diffMillis)
        return if (diffHours <= 0L) 1L else diffHours
    }
}