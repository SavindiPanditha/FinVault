package com.example.imilipocket.ui

import android.os.Bundle
import android.content.Intent
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
import com.example.imilipocket.auth.SupabaseAuthService
import com.example.imilipocket.databinding.FragmentSettingsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by viewModels()
    private lateinit var authService: SupabaseAuthService
    private val currencies = listOf("LKR", "USD", "EUR", "GBP", "JPY") // LKR first

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        authService = SupabaseAuthService(requireContext())
        setupCurrencySpinner()
        setupButtons()
        observeErrorMessages()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupCurrencySpinner() {
        binding.spinnerCurrency.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, currencies
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getCurrency().collectLatest { currency ->
                    currency?.let { curr ->
                        val index = currencies.indexOf(curr.code)
                        if (index != -1) binding.spinnerCurrency.setSelection(index)
                    }
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnSaveCurrency.setOnClickListener {
            val selectedCurrency = currencies[binding.spinnerCurrency.selectedItemPosition]
            viewModel.saveCurrency(selectedCurrency)
            Toast.makeText(context, "Currency set to $selectedCurrency", Toast.LENGTH_SHORT).show()
        }

        binding.btnExport.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val json = viewModel.exportDataToFile(requireContext())
                if (json.isNotBlank()) {
                    binding.etBackupJson.setText(json)
                    Toast.makeText(context, "Data exported to internal storage", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnImport.setOnClickListener {
            viewModel.restoreDataFromFile(requireContext())
            Toast.makeText(context, "Data restoration started", Toast.LENGTH_SHORT).show()
        }

        binding.btnClearData.setOnClickListener {
            viewModel.clearAllData()
            Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
        }

        binding.btnSignOut.setOnClickListener {
            authService.signOut()
            startActivity(Intent(requireContext(), SignInActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun observeErrorMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collectLatest { message ->
                    message?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}