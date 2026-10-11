package com.example.rss_chsu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rss_chsu.databinding.FragmentNotesBinding
import com.google.android.material.card.MaterialCardView

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Кнопка назад
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Сохранение и добавление заметки в список
        binding.buttonSaveNote.setOnClickListener {
            val text = binding.editTextNote.text.toString().trim()
            if (text.isNotEmpty()) {
                addNoteCard(text)
                binding.editTextNote.text?.clear()
            }
        }
    }

    private fun addNoteCard(noteText: String) {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            radius = 12f * resources.displayMetrics.density
            strokeWidth = 1
            strokeColor = resources.getColor(R.color.palette_text_sub, null)
            setCardBackgroundColor(resources.getColor(R.color.white, null))
        }

        val textView = TextView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
            text = noteText
            textSize = 16f
            setTextColor(resources.getColor(R.color.palette_text_main, null))
        }

        card.addView(textView)
        binding.containerNotes.addView(card, 0) // Добавляем сверху
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}