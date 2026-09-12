package com.fivepesos.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fivepesos.app.data.BuiltInSkins
import com.fivepesos.app.data.CoinArt
import com.fivepesos.app.data.CoinBrand
import com.fivepesos.app.data.CoinSkin
import com.fivepesos.app.data.Cr2032Brands
import com.fivepesos.app.data.Face
import com.fivepesos.app.data.FlipPhase
import com.fivepesos.app.data.ImageTarget
import com.fivepesos.app.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class CoinUiState(
    val skins: List<CoinSkin> = BuiltInSkins,
    val selectedSkin: CoinSkin = BuiltInSkins.first(),
    val cr2032Brands: List<CoinBrand> = Cr2032Brands,
    val selectedCr2032Brand: CoinBrand = Cr2032Brands.first(),
    val spinForever: Boolean = true,
    val customHeadsUri: Uri? = null,
    val customTailsUri: Uri? = null,
    val displayedFace: Face = Face.HEADS,
    val phase: FlipPhase = FlipPhase.IDLE,
    val settingsOpen: Boolean = false,
    val googleImportOpen: Boolean = false,
)

private const val CR2032_SKIN_ID = "cr2032"

class CoinViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    private val _ui = MutableStateFlow(CoinUiState())
    val ui: StateFlow<CoinUiState> = _ui.asStateFlow()

    private var flipJob: Job? = null

    // Flipping/spinning is a plain interval toggle between the two static
    // faces -- deliberately no rotation animation, just a swap every tick.
    private val cycleIntervalMs = 90L
    private val timedFlipDurationMs = 1100L

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                val brand = Cr2032Brands.find { it.id == settings.selectedCr2032BrandId }
                    ?: Cr2032Brands.first()
                // The "cr2032" entry in BuiltInSkins only carries a default
                // brand's art; swap in whichever brand is actually selected
                // so the rest of the app never needs to know brands exist.
                val skins = BuiltInSkins.map { skin ->
                    if (skin.id == CR2032_SKIN_ID) {
                        skin.copy(art = CoinArt.Photo(brand.headsRes, brand.tailsRes))
                    } else {
                        skin
                    }
                }
                _ui.update { current ->
                    current.copy(
                        skins = skins,
                        spinForever = settings.spinForever,
                        selectedSkin = skins.find { it.id == settings.selectedSkinId }
                            ?: skins.first(),
                        selectedCr2032Brand = brand,
                        customHeadsUri = settings.customHeadsUri?.let(Uri::parse),
                        customTailsUri = settings.customTailsUri?.let(Uri::parse),
                    )
                }
            }
        }
    }

    fun toggleSettings(open: Boolean) {
        _ui.update { it.copy(settingsOpen = open) }
    }

    fun toggleGoogleImport(open: Boolean) {
        _ui.update { it.copy(googleImportOpen = open) }
    }

    /** A photo picked (gallery) or downloaded (Google search) for either
     * face of "Your Own Coin" -- both paths land here. */
    fun importImage(target: ImageTarget, uri: Uri) {
        when (target) {
            ImageTarget.HEADS -> setCustomHeads(uri)
            ImageTarget.TAILS -> setCustomTails(uri)
        }
    }

    fun setSpinForever(value: Boolean) {
        viewModelScope.launch { repository.setSpinForever(value) }
    }

    fun selectSkin(id: String) {
        viewModelScope.launch { repository.setSelectedSkin(id) }
    }

    fun selectCr2032Brand(id: String) {
        viewModelScope.launch { repository.setSelectedCr2032Brand(id) }
    }

    fun setCustomHeads(uri: Uri) {
        viewModelScope.launch { repository.setCustomHeadsUri(uri.toString()) }
    }

    fun setCustomTails(uri: Uri) {
        viewModelScope.launch { repository.setCustomTailsUri(uri.toString()) }
    }

    /** What the single coin button does, driven by [CoinUiState.phase] and
     * [CoinUiState.spinForever] -- see the button-label mapping in
     * `CoinScreen.kt` for the labels this drives. */
    fun primaryAction() {
        val current = _ui.value
        val missingCustomArt = current.selectedSkin.art is CoinArt.Custom &&
            (current.customHeadsUri == null || current.customTailsUri == null)
        if (missingCustomArt) {
            _ui.update { it.copy(settingsOpen = true) }
            return
        }
        when (current.phase) {
            FlipPhase.IDLE, FlipPhase.RESULT -> startFlip()
            FlipPhase.FLIPPING -> if (current.spinForever) stopFlip()
        }
    }

    private fun startFlip() {
        flipJob?.cancel()
        _ui.update { it.copy(phase = FlipPhase.FLIPPING) }
        val spinForever = _ui.value.spinForever
        flipJob = viewModelScope.launch {
            val start = System.currentTimeMillis()
            while (isActive && (spinForever || System.currentTimeMillis() - start < timedFlipDurationMs)) {
                _ui.update { it.copy(displayedFace = it.displayedFace.opposite()) }
                delay(cycleIntervalMs)
            }
            if (isActive && !spinForever) {
                finishFlip()
            }
        }
    }

    private fun stopFlip() {
        flipJob?.cancel()
        finishFlip()
    }

    private fun finishFlip() {
        val result = if (Random.nextBoolean()) Face.HEADS else Face.TAILS
        _ui.update { it.copy(displayedFace = result, phase = FlipPhase.RESULT) }
    }

    private fun Face.opposite(): Face = if (this == Face.HEADS) Face.TAILS else Face.HEADS
}
