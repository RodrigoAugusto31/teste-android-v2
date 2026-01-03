package com.example.sptransapp.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sptransapp.R
import com.example.sptransapp.data.api.RetrofitClient
import com.example.sptransapp.data.repository.BusRepositoryImpl
import com.example.sptransapp.databinding.FragmentLinesBinding
import com.example.sptransapp.presentation.adapter.LinesAdapter
import com.example.sptransapp.presentation.viewmodel.MapViewModel
import com.example.sptransapp.presentation.viewmodel.MapViewModelFactory

class LinesFragment : Fragment() {

    private var _binding: FragmentLinesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by activityViewModels {
        MapViewModelFactory(
            repository = BusRepositoryImpl(
                api = RetrofitClient.api,
                context = requireContext().applicationContext
            ),
            context = requireContext().applicationContext
        )
    }

    private lateinit var adapter: LinesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLinesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupUI()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = LinesAdapter { line ->
            viewModel.selectLine(line)

            findNavController().navigate(R.id.nav_line_details)
        }

        binding.recyclerviewLines.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerviewLines.adapter = adapter
    }

    private fun setupUI() {
        binding.btnSearchLines.setOnClickListener { performSearch() }

        binding.edittextSearchLines.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val termo = binding.edittextSearchLines.text.toString()
        if (termo.isNotEmpty()) {
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.edittextSearchLines.windowToken, 0)

            viewModel.searchLine(termo)
        }
    }

    private fun setupObservers() {
        viewModel.foundLines.observe(viewLifecycleOwner) { lines ->
            binding.textviewEmpty.isVisible = lines.isEmpty()
            adapter.updateList(lines)

            if (lines.isEmpty() && binding.edittextSearchLines.text.isNotEmpty()) {
                binding.textviewEmpty.text = getString(R.string.no_lines_found_message)
            } else if (lines.isNotEmpty()) {
                binding.textviewEmpty.isVisible = false
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressbarLines.isVisible = isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
