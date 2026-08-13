package com.vanbank.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vanbank.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username COLLATE NOCASE)")
    suspend fun usernameExists(username: String): Boolean

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}
