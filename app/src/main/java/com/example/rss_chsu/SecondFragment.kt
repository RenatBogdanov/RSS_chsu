package com.example.rss_chsu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.rss_chsu.databinding.FragmentSourcesBinding

class SecondFragment : Fragment() {

    private var _binding: FragmentSourcesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Переход на 1 страницу по нажатию на кнопку "Читать"
        binding.buttonRead.setOnClickListener {
            findNavController().navigateUp()
        }

        // Переход на страницу заметок по нажатию на кнопку "Заметки"
        binding.buttonNotes.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_NotesFragment)
        }

        // Открытие шторки "Настройки" по нажатию на ⚙
        binding.buttonSettings.setOnClickListener {
            val dialog = BottomSheetDialog(requireContext())
            val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
            dialog.setContentView(dialogView)
            dialog.show()
        }

        // Открытие шторки "Меню добавления ленты" по нажатию на +
        binding.buttonAddFeed.setOnClickListener {
            val dialog = BottomSheetDialog(requireContext())
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_feed, null)
            dialog.setContentView(dialogView)
            dialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}