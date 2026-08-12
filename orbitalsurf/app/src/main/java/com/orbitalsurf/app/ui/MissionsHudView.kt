package com.orbitalsurf.app.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.orbitalsurf.app.databinding.ItemMissionHudBinding
import com.orbitalsurf.core.progression.Mission

/** In-game overlay listing the 3 active missions with progress and, when a voucher is available, a per-mission Skip button. */
class MissionsHudView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    init {
        orientation = VERTICAL
    }

    fun update(missions: List<Mission>, voucherCount: Int, onUseVoucher: (missionId: String) -> Unit) {
        removeAllViews()
        for (mission in missions) {
            val item = ItemMissionHudBinding.inflate(LayoutInflater.from(context), this, false)
            val progress = mission.progress.toInt()
            val target = mission.target.toInt()
            item.missionText.text = "${mission.description} ($progress/$target)"
            if (!mission.isComplete && voucherCount > 0) {
                item.skipButton.visibility = View.VISIBLE
                item.skipButton.setOnClickListener { onUseVoucher(mission.id) }
            } else {
                item.skipButton.visibility = View.GONE
            }
            addView(item.root)
        }
    }
}
