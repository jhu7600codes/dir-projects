package com.ytclassic.app.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ytclassic.app.R
import com.ytclassic.app.YtClassicApp
import com.ytclassic.app.auth.LoginActivity
import com.ytclassic.app.databinding.FragmentLibraryBinding
import com.ytclassic.app.ui.downloads.DownloadsActivity
import com.ytclassic.app.ui.settings.SettingsActivity

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val sessionManager get() = (requireActivity().application as YtClassicApp).sessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rowHistory.rowIcon.setImageResource(R.drawable.ic_tab_trending)
        binding.rowHistory.rowLabel.text = getString(R.string.library_history)
        binding.rowYourVideos.rowIcon.setImageResource(R.drawable.ic_tab_subscriptions)
        binding.rowYourVideos.rowLabel.text = getString(R.string.library_your_videos)
        binding.rowWatchLater.rowIcon.setImageResource(R.drawable.ic_tab_library)
        binding.rowWatchLater.rowLabel.text = getString(R.string.library_watch_later)
        binding.rowPlaylists.rowIcon.setImageResource(R.drawable.ic_playlist_add)
        binding.rowPlaylists.rowLabel.text = getString(R.string.library_playlists)
        binding.rowDownloads.rowIcon.setImageResource(R.drawable.ic_download)
        binding.rowDownloads.rowLabel.text = getString(R.string.downloads_title)
        binding.rowSettings.rowIcon.setImageResource(R.drawable.ic_more_vert)
        binding.rowSettings.rowLabel.text = getString(R.string.settings_title)

        binding.rowDownloads.root.setOnClickListener {
            startActivity(Intent(requireContext(), DownloadsActivity::class.java))
        }
        binding.rowSettings.root.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        binding.signInButton.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.signInBanner.visibility = if (sessionManager.isSignedIn) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
