package com.ytclassic.app.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ytclassic.app.YtClassicApp
import com.ytclassic.app.databinding.FragmentVideoListBinding
import com.ytclassic.app.ui.player.PlayerActivity
import kotlinx.coroutines.launch

/**
 * Backs Home, Trending, Subscriptions and Search results - all four are
 * "a list of videos with pull-to-refresh and infinite scroll", the only
 * real difference is where [VideoListViewModel] pulls rows from.
 */
class VideoListFragment : Fragment() {

    private var _binding: FragmentVideoListBinding? = null
    private val binding get() = _binding!!

    private lateinit var source: VideoListSource
    private lateinit var viewModel: VideoListViewModel
    private lateinit var adapter: VideoListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = requireArguments().getString(ARG_SOURCE_TYPE)!!
        val extra = requireArguments().getString(ARG_SOURCE_EXTRA)
        source = VideoListSource.deserialize(type, extra)

        viewModel = ViewModelProvider(this, Factory(source))[VideoListViewModel::class.java]
        viewModel.cookieProvider = {
            (requireActivity().application as YtClassicApp).sessionManager.cookie
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVideoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = VideoListAdapter(
            onClick = { video -> PlayerActivity.start(requireContext(), video.url) },
            onOverflowClick = { _, _ -> /* TODO: download/save/share overflow menu */ },
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (lastVisible >= adapter.itemCount - 4) {
                    viewModel.loadMore()
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        binding.emptyActionButton.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state -> render(state) }
        }

        if (viewModel.state.value.items.isEmpty()) {
            viewModel.refresh()
        }
    }

    private fun render(state: VideoListUiState) {
        binding.swipeRefresh.isRefreshing = false
        binding.progressBar.visibility = if (state.isLoading && state.items.isEmpty()) View.VISIBLE else View.GONE

        adapter.submitList(state.items)

        val showEmpty = !state.isLoading && state.items.isEmpty()
        binding.emptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (state.items.isNotEmpty()) View.VISIBLE else View.GONE

        if (showEmpty) {
            binding.emptyText.text = when {
                state.requiresSignIn -> getString(com.ytclassic.app.R.string.empty_no_subscriptions)
                state.error != null -> getString(com.ytclassic.app.R.string.error_generic)
                else -> getString(com.ytclassic.app.R.string.empty_no_results)
            }
            binding.emptyActionButton.visibility = if (state.error != null) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class Factory(private val source: VideoListSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            VideoListViewModel(source) as T
    }

    companion object {
        private const val ARG_SOURCE_TYPE = "source_type"
        private const val ARG_SOURCE_EXTRA = "source_extra"

        fun newInstance(source: VideoListSource): VideoListFragment {
            val (type, extra) = source.serialize()
            return VideoListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SOURCE_TYPE, type)
                    putString(ARG_SOURCE_EXTRA, extra)
                }
            }
        }
    }
}
