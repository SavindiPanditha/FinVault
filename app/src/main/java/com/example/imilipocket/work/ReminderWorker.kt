package com.example.imilipocket.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.imilipocket.R
import com.example.imilipocket.data.AppDatabase
import com.example.imilipocket.data.FinanceRepository
import com.example.imilipocket.model.TransactionType
import kotlinx.coroutines.flow.first
import java.util.Calendar

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        createNotificationChannel()
        val repository = FinanceRepository(
            AppDatabase.getDatabase(applicationContext).transactionDao(),
            AppDatabase.getDatabase(applicationContext).budgetDao(),
            AppDatabase.getDatabase(applicationContext).categoryDao(),
            AppDatabase.getDatabase(applicationContext).currencyDao()
        )

        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        val budgets = repository.getAllBudgets().first()
        val transactions = repository.getAllTransactions().first()
        val categories = repository.getAllCategories().first()

        budgets.forEach { budget ->
            if (budget.month == month && budget.year == year) {
                val expenses = transactions
                    .filter { it.type == TransactionType.EXPENSE && it.categoryId == budget.categoryId }
                    .sumOf { it.amount }
                val category = categories.find { it.id == budget.categoryId }
                if (expenses >= budget.amount * 0.8 && expenses < budget.amount) {
                    sendNotification(
                        budget.categoryId,
                        category?.name ?: "Category ${budget.categoryId}",
                        expenses,
                        budget.amount,
                        "Approaching Budget Limit"
                    )
                } else if (expenses >= budget.amount) {
                    sendNotification(
                        budget.categoryId,
                        category?.name ?: "Category ${budget.categoryId}",
                        expenses,
                        budget.amount,
                        "Budget Exceeded"
                    )
                }
            }
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Notifications"
            val descriptionText = "Notifications for budget limits and reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("budget_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(categoryId: Int, categoryName: String, expenses: Double, budget: Double, title: String) {
        val notification = NotificationCompat.Builder(applicationContext, "budget_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("$categoryName: Spent $expenses, Budget $budget")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(categoryId, notification)
    }
}