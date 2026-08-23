package com.ytclassic.app.ui.search

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.ytclassic.app.databinding.ActivitySearchBinding
import com.ytclassic.app.ui.common.VideoListFragment
import com.ytclassic.app.ui.common.VideoListSource

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.searchInput.requestFocus()

        binding.searchInput.setOnEditorActionListener { _, actionId, event ->
            val isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
            if (isSearchAction) {
                performSearch(binding.searchInput.text.toString())
                true
            } else {
                false
            }
        }
        binding.searchGoButton.setOnClickListener {
            performSearch(binding.searchInput.text.toString())
        }
        binding.clearButton.setOnClickListener { binding.searchInput.text.clear() }
    }

    private fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        supportFragmentManager.beginTransaction()
            .replace(binding.resultsContainer.id, VideoListFragment.newInstance(VideoListSource.Search(trimmed)))
            .commit()
    }
}
