package com.example.imilipocket.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.imilipocket.R
import com.example.imilipocket.model.CategoryEntity
import com.example.imilipocket.model.TransactionEntity
import com.example.imilipocket.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var etAmount: EditText
    private lateinit var etDate: EditText
    private lateinit var etNote: EditText
    private lateinit var radioGroupType: RadioGroup
    private lateinit var radioIncome: RadioButton
    private lateinit var radioExpense: RadioButton
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSave: Button
    private val viewModel: FinanceViewModel by viewModels()
    private val calendar = Calendar.getInstance()
    private var selectedDate: Date = calendar.time
    private var selectedType: TransactionType = TransactionType.EXPENSE // Default to Expense
    private var selectedCurrencyId: Int = 1
    private var categoriesForSelectedType: List<CategoryEntity> = emptyList()
    private var editTransaction: TransactionEntity? = null
    private var pendingCategoryId: Int? = null
    private var refreshCategoriesJob: Job? = null
    private val TAG = "AddTransactionActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        // Initialize views with findViewById
        etAmount = findViewById(R.id.et_amount)
        etDate = findViewById(R.id.et_date)
        etNote = findViewById(R.id.et_note)
        radioGroupType = findViewById(R.id.radio_group_type)
        radioIncome = findViewById(R.id.radio_income)
        radioExpense = findViewById(R.id.radio_expense)
        spinnerCategory = findViewById(R.id.spinner_category)
        btnSave = findViewById(R.id.btn_save)

        setupDatePicker()
        setupTypeRadioGroup()
        refreshCategorySpinner()
        loadDefaultCurrency()
        loadTransactionForEditIfNeeded()
        setupSaveButton()
    }

    private fun setupDatePicker() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        etDate.setText(dateFormat.format(selectedDate))
        etDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedDate = calendar.time
                    etDate.setText(dateFormat.format(selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupTypeRadioGroup() {
        radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            selectedType = when (checkedId) {
                R.id.radio_income -> TransactionType.INCOME
                R.id.radio_expense -> TransactionType.EXPENSE
                else -> TransactionType.EXPENSE // Fallback
            }
            Log.d(TAG, "Selected type: $selectedType")
            refreshCategorySpinner()
        }
        // Set initial selection
        radioExpense.isChecked = true
    }

    private fun refreshCategorySpinner() {
        refreshCategoriesJob?.cancel()
        refreshCategoriesJob = lifecycleScope.launch {
            var categories = viewModel.getCategoriesByType(selectedType.name).first()
            if (categories.isEmpty()) {
                spinnerCategory.isEnabled = false
                btnSave.isEnabled = false
                viewModel.initializeCategories()
                categories = viewModel.getCategoriesByType(selectedType.name).first()
            }

            categoriesForSelectedType = categories
            Log.d(TAG, "Categories for $selectedType: ${categories.map { it.name }}")

            spinnerCategory.adapter = ArrayAdapter(
                this@AddTransactionActivity,
                android.R.layout.simple_spinner_item,
                categories.map { it.name }
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            spinnerCategory.isEnabled = categories.isNotEmpty()
            btnSave.isEnabled = categories.isNotEmpty()

            val categoryId = pendingCategoryId
            if (categoryId != null) {
                val selectedIndex = categories.indexOfFirst { it.id == categoryId }
                if (selectedIndex >= 0) {
                    spinnerCategory.setSelection(selectedIndex)
                }
                pendingCategoryId = null
            } else if (categories.isNotEmpty()) {
                spinnerCategory.setSelection(0)
            }
        }
    }

    private fun loadDefaultCurrency() {
        lifecycleScope.launch {
            selectedCurrencyId = viewModel.getDefaultCurrencyId()
        }
    }

    private fun loadTransactionForEditIfNeeded() {
        val transactionId = intent.getIntExtra("TRANSACTION_ID", -1)
        if (transactionId <= 0) {
            return
        }

        lifecycleScope.launch {
            val transaction = viewModel.getTransactionById(transactionId).first()
            if (transaction == null) {
                Toast.makeText(this@AddTransactionActivity, "Transaction not found", Toast.LENGTH_SHORT).show()
                return@launch
            }

            editTransaction = transaction
            etAmount.setText(transaction.amount.toString())
            etNote.setText(transaction.note.orEmpty())
            selectedDate = transaction.date
            etDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate))
            selectedCurrencyId = transaction.currencyId
            pendingCategoryId = transaction.categoryId

            when (transaction.type) {
                TransactionType.INCOME -> radioIncome.isChecked = true
                TransactionType.EXPENSE -> radioExpense.isChecked = true
            }
        }
    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener {
            lifecycleScope.launch {
                val amount = etAmount.text.toString().toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this@AddTransactionActivity, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (selectedDate.after(Date())) {
                    Toast.makeText(this@AddTransactionActivity, "Date cannot be in the future", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val note = etNote.text.toString().trim()
                if (note.length > 200) {
                    Toast.makeText(this@AddTransactionActivity, "Note must be 200 characters or less", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val index = spinnerCategory.selectedItemPosition
                if (index < 0 || index >= categoriesForSelectedType.size) {
                    Toast.makeText(this@AddTransactionActivity, "Select a valid category", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val transaction = TransactionEntity(
                    id = editTransaction?.id ?: 0,
                    amount = amount,
                    type = selectedType,
                    categoryId = categoriesForSelectedType[index].id,
                    date = selectedDate,
                    note = note,
                    currencyId = selectedCurrencyId
                )

                if (editTransaction == null) {
                    Log.d(TAG, "Inserting transaction: $transaction")
                    viewModel.insertTransaction(transaction)
                } else {
                    Log.d(TAG, "Updating transaction: $transaction")
                    viewModel.updateTransaction(transaction)
                }

                setResult(RESULT_OK)
                finish()
            }
        }
    }

    override fun onDestroy() {
        refreshCategoriesJob?.cancel()
        super.onDestroy()
    }
}