package com.orbitalsurf.core.save

object GameSaveDefaults {
    /** A brand-new save, as given to a player who has never played before. */
    fun new(): GameSave = GameSave()
}
