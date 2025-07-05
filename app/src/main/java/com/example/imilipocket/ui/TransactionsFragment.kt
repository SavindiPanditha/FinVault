package com.example.imilipocket.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.imilipocket.databinding.FragmentTransactionsBinding
import com.example.imilipocket.ui.adapter.TransactionAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        setupRecyclerView()
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(context, AddTransactionActivity::class.java))
        }
        observeData()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onEdit = { transaction ->
                val intent = Intent(context, AddTransactionActivity::class.java)
                intent.putExtra("TRANSACTION_ID", transaction.id)
                startActivity(intent)
            },
            onDelete = { transaction -> viewModel.deleteTransaction(transaction) },
            categories = emptyList(),
            currencies = emptyList()
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(context)
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.transactions.collectLatest { transactions ->
                viewModel.categories.collectLatest { categories ->
                    viewModel.currencies.collectLatest { currencies ->
                        transactionAdapter = TransactionAdapter(
                            onEdit = { transaction ->
                                val intent = Intent(context, AddTransactionActivity::class.java)
                                intent.putExtra("TRANSACTION_ID", transaction.id)
                                startActivity(intent)
                            },
                            onDelete = { transaction -> viewModel.deleteTransaction(transaction) },
                            categories = categories,
                            currencies = currencies
                        )
                        binding.rvTransactions.adapter = transactionAdapter
                        transactionAdapter.submitList(transactions)
                    }
                }
            }
        }
    }
}