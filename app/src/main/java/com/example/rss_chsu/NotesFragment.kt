package com.example.rss_chsu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rss_chsu.ui.screens.NotesScreen
import com.example.rss_chsu.ui.theme.RSS_chsuTheme

class NotesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RSS_chsuTheme {
                    NotesScreen(
                        onBackClick = {
                            findNavController().navigateUp()
                        }
                    )
                }
            }
        }
    }
}