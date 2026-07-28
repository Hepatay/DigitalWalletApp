package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGoldAssetDao {

    @Query("SELECT * FROM user_gold_assets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UserGoldAssetEntity>>

    @Query("SELECT * FROM user_gold_assets ORDER BY createdAt DESC")
    suspend fun getAllSnapshot(): List<UserGoldAssetEntity>

    @Insert
    suspend fun insert(asset: UserGoldAssetEntity): Long

    @Update
    suspend fun update(asset: UserGoldAssetEntity)

    @Delete
    suspend fun delete(asset: UserGoldAssetEntity)
}
