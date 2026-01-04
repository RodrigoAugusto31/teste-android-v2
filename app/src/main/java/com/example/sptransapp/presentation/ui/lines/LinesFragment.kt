package com.example.sptransapp.presentation.ui.lines

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sptransapp.R
import com.example.sptransapp.databinding.FragmentLinesBinding
import com.example.sptransapp.presentation.adapter.LinesAdapter
import com.example.sptransapp.presentation.viewmodel.LinesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LinesFragment : Fragment() {

    private var _binding: FragmentLinesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LinesViewModel by viewModels()

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
            val bundle = bundleOf("selectedLine" to line)
            findNavController().navigate(R.id.nav_line_details, bundle)
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

        binding.edittextSearchLines.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    viewModel.loadFavorites()
                }
            }
        })
    }

    private fun performSearch() {
        val termo = binding.edittextSearchLines.text.toString()
        if (termo.isNotEmpty()) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.edittextSearchLines.windowToken, 0)
            viewModel.searchLine(termo)
        } else {
            viewModel.loadFavorites()
        }
    }

    private fun setupObservers() {
        viewModel.displayedLines.observe(viewLifecycleOwner) { lines ->

            adapter.submitList(lines)

            val isSearching = binding.edittextSearchLines.text.isNotEmpty()
            val hasItems = lines.isNotEmpty()

            binding.textviewListTitle.isVisible = !isSearching && hasItems

            if (!hasItems) {
                binding.textviewEmpty.isVisible = true
                if (isSearching) {
                    binding.textviewEmpty.text = getString(R.string.no_lines_found_message)
                } else {
                    binding.textviewEmpty.text = getString(R.string.no_fav_lines)
                }
            } else {
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
