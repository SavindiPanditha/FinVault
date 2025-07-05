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
import com.example.imilipocket.model.TransactionEntity
import com.example.imilipocket.model.TransactionType
import kotlinx.coroutines.flow.collectLatest
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
        setupCategorySpinner()
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
            setupCategorySpinner()
        }
        // Set initial selection
        radioExpense.isChecked = true
    }

    private fun setupCategorySpinner() {
        lifecycleScope.launch {
            viewModel.getCategoriesByType(selectedType.name).collectLatest { categories ->
                Log.d(TAG, "Categories for $selectedType: ${categories.map { it.name }}")
                spinnerCategory.adapter = ArrayAdapter(
                    this@AddTransactionActivity,
                    android.R.layout.simple_spinner_item,
                    categories.map { it.name }
                ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                if (categories.isEmpty()) {
                    spinnerCategory.isEnabled = false
                    btnSave.isEnabled = false
                    Toast.makeText(
                        this@AddTransactionActivity,
                        "No categories available for $selectedType. Initializing categories...",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.initializeCategories()
                    // Retry fetching categories
                    viewModel.getCategoriesByType(selectedType.name).collectLatest { retryCategories ->
                        Log.d(TAG, "Retry categories for $selectedType: ${retryCategories.map { it.name }}")
                        spinnerCategory.adapter = ArrayAdapter(
                            this@AddTransactionActivity,
                            android.R.layout.simple_spinner_item,
                            retryCategories.map { it.name }
                        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                        if (retryCategories.isNotEmpty()) {
                            spinnerCategory.isEnabled = true
                            btnSave.isEnabled = true
                            spinnerCategory.setSelection(0)
                        }
                    }
                } else {
                    spinnerCategory.isEnabled = true
                    btnSave.isEnabled = true
                    spinnerCategory.setSelection(0)
                }
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
                viewModel.getCategoriesByType(selectedType.name).collectLatest { categories ->
                    val index = spinnerCategory.selectedItemPosition
                    if (index < 0 || index >= categories.size) {
                        Toast.makeText(this@AddTransactionActivity, "Select a valid category", Toast.LENGTH_SHORT).show()
                        return@collectLatest
                    }
                    val transaction = TransactionEntity(
                        amount = amount,
                        type = selectedType,
                        categoryId = categories[index].id,
                        date = selectedDate,
                        note = etNote.text.toString(),
                        currencyId = 1 // Default to LKR
                    )
                    Log.d(TAG, "Inserting transaction: $transaction")
                    viewModel.insertTransaction(transaction)
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }
}