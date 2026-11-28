package app.krala.core

import java.util.UUID

object Ids {
    fun new(): String = UUID.randomUUID().toString()
}
