package com.vanbank.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A VANBank user. Passwords are never stored -- only a PBKDF2 hash + salt
 * (see [com.vanbank.core.security.PasswordHasher]), even though this is a
 * fully simulated bank with no real money behind it.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val passwordSalt: String,
    val fullName: String,
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
