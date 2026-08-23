package com.jhulian.android.youtube.classic.ui.shorts

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.jhulian.android.youtube.classic.R
import com.jhulian.android.youtube.classic.YtClassicApp
import com.jhulian.android.youtube.classic.data.model.VideoUi
import com.jhulian.android.youtube.classic.databinding.FragmentShortsBinding
import com.jhulian.android.youtube.classic.extractor.YouTubeRepository
import com.jhulian.android.youtube.classic.playback.PlaybackService
import com.jhulian.android.youtube.classic.ui.channel.ChannelActivity
import com.jhulian.android.youtube.classic.ui.player.PlayerActivity
import com.jhulian.android.youtube.classic.playback.StreamSelector
import kotlinx.coroutines.launch

/**
 * A vertical, one-at-a-time feed of Shorts. Only the centered item ever has
 * the shared [MediaController] attached (see [ShortsAdapter]) - resolving
 * a playable stream is a network call per item
 * ([YouTubeRepository.streamInfo]), so results are cached by position for
 * the lifetime of this screen to make swiping back up instant.
 */
@UnstableApi
class ShortsFragment : Fragment() {

    private var _binding: FragmentShortsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShortsViewModel by viewModels()
    private lateinit var adapter: ShortsAdapter

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller get() = controllerFuture?.takeIf { it.isDone }?.get()

    private val resolvedMediaItems = mutableMapOf<Int, androidx.media3.common.MediaItem>()
    private var currentActivePosition = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentShortsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ShortsAdapter(
            onLike = { /* like via innertube not wired here yet - sign-in-gated like on the player screen covers that path */ },
            onComment = { video -> PlayerActivity.start(requireContext(), video.url) },
            onShare = { video ->
                startActivity(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, video.url)
                    },
                )
            },
            onChannel = { video ->
                video.channelUrl?.let { channelUrl ->
                    startActivity(
                        Intent(requireContext(), ChannelActivity::class.java)
                            .putExtra(ChannelActivity.EXTRA_CHANNEL_URL, channelUrl),
                    )
                }
            },
        )

        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter
        PagerSnapHelper().attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                val position = layoutManager.findFirstVisibleItemPosition()
                if (position != RecyclerView.NO_POSITION && position != currentActivePosition) {
                    currentActivePosition = position
                    playPosition(position)
                }
            }
        })

        binding.signInButton.setOnClickListener {
            startActivity(Intent(requireContext(), com.jhulian.android.youtube.classic.auth.LoginActivity::class.java))
        }

        connectToPlaybackService()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state -> render(state) }
        }

        val cookie = (requireActivity().application as YtClassicApp).sessionManager.cookie
        viewModel.loadIfNeeded(cookie)
    }

    private fun render(state: ShortsUiState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        val showEmpty = state.hasLoaded && !state.isLoading && state.items.isEmpty()
        binding.emptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.signInButton.visibility = if (state.requiresSignIn) View.VISIBLE else View.GONE
        binding.emptyText.text = if (state.requiresSignIn) {
            getString(R.string.empty_no_shorts)
        } else {
            getString(R.string.empty_no_results)
        }

        adapter.submitList(state.items) {
            if (currentActivePosition == -1 && state.items.isNotEmpty()) {
                currentActivePosition = 0
                playPosition(0)
            }
        }
    }

    private fun connectToPlaybackService() {
        val token = SessionToken(requireContext(), ComponentName(requireContext(), PlaybackService::class.java))
        val future = MediaController.Builder(requireContext(), token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                if (currentActivePosition >= 0) playPosition(currentActivePosition)
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun playPosition(position: Int) {
        val player = controller ?: return
        val video = adapter.currentList.getOrNull(position) ?: return

        val cached = resolvedMediaItems[position]
        if (cached != null) {
            startPlayback(player, position, cached)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val info = YouTubeRepository.streamInfo(video.url)
                val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val maxHeight = StreamSelector.heightForQualityPref(prefs.getString("pref_default_quality", "auto"))
                val mediaItem = StreamSelector.buildMediaItem(info, maxHeight) ?: return@launch
                resolvedMediaItems[position] = mediaItem
                if (position == currentActivePosition) startPlayback(player, position, mediaItem)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve short at position $position", e)
            }
        }
    }

    private fun startPlayback(player: MediaController, position: Int, mediaItem: androidx.media3.common.MediaItem) {
        adapter.setActivePosition(position, player, binding.recyclerView)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
        player.play()
    }

    override fun onPause() {
        super.onPause()
        controller?.pause()
    }

    // MainActivity switches tabs via FragmentManager hide()/show(), not
    // replace() - that leaves this fragment fully RESUMED, just with its
    // view hidden, so onPause()/onResume() never fire when the user leaves
    // or comes back to this tab. onHiddenChanged is what actually fires.
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            controller?.pause()
        } else {
            controller?.play()
        }
    }

    override fun onDestroyView() {
        controller?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ShortsFragment"
    }
}
