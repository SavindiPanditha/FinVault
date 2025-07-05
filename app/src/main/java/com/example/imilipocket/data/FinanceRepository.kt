package com.example.imilipocket.data

import com.example.imilipocket.model.BudgetEntity
import com.example.imilipocket.model.CategoryEntity
import com.example.imilipocket.model.CurrencyEntity
import com.example.imilipocket.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val currencyDao: CurrencyDao
) {
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAll()

    suspend fun insertTransaction(transaction: TransactionEntity) = transactionDao.insert(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.update(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.delete(transaction)

    fun getTransactionById(id: Int): Flow<TransactionEntity?> = transactionDao.getById(id)

    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAll()

    suspend fun insertBudget(budget: BudgetEntity) = budgetDao.insert(budget)

    suspend fun updateBudget(budget: BudgetEntity) = budgetDao.update(budget)

    suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.delete(budget)

    fun getBudgetByCategory(categoryId: Int, month: Int, year: Int): Flow<BudgetEntity?> =
        budgetDao.getByCategory(categoryId, month, year)

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAll()

    suspend fun insertCategory(category: CategoryEntity) = categoryDao.insert(category)

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = categoryDao.getByType(type)

    suspend fun initializeCategories() {
        listOf(
            CategoryEntity(name = "Salary", type = "INCOME"),
            CategoryEntity(name = "Food", type = "EXPENSE"),
            CategoryEntity(name = "Transport", type = "EXPENSE"),
            CategoryEntity(name = "Bills", type = "EXPENSE"),
            CategoryEntity(name = "Entertainment", type = "EXPENSE")
        ).forEach { categoryDao.insert(it) }
    }

    fun getAllCurrencies(): Flow<List<CurrencyEntity>> = currencyDao.getAll()

    suspend fun insertCurrency(currency: CurrencyEntity) = currencyDao.insert(currency)

    fun getDefaultCurrency(): Flow<CurrencyEntity?> = currencyDao.getDefault()

    suspend fun getDefaultCurrencySync(): CurrencyEntity? = currencyDao.getDefaultSync()

    suspend fun clearAllData() {
        transactionDao.deleteAll()
        budgetDao.deleteAll()
        categoryDao.deleteAll()
        currencyDao.deleteAll()
    }
}