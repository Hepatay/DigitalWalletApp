package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGoldAssetDao {

    @Query("UPDATE user_gold_assets SET user_id = :uid, updated_at = :timestamp, is_synced = 0 WHERE user_id IS NULL OR user_id = 'guest'")
    suspend fun assignUserToGuestRecords(uid: String, timestamp: Long)

    @Query("SELECT * FROM user_gold_assets WHERE is_deleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UserGoldAssetEntity>>

    @Query("SELECT COUNT(*) FROM user_gold_assets WHERE is_deleted = 0")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_gold_assets WHERE is_deleted = 0")
    suspend fun getCount(): Int

    @Query("SELECT * FROM user_gold_assets ORDER BY createdAt DESC")
    suspend fun getAllSnapshot(): List<UserGoldAssetEntity>

    @Query("SELECT * FROM user_gold_assets")
    suspend fun getAllSync(): List<UserGoldAssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: UserGoldAssetEntity): Long

    @Update
    suspend fun update(asset: UserGoldAssetEntity)

    @Query("UPDATE user_gold_assets SET is_deleted = 1, is_synced = 0 WHERE uuid = :uuid")
    suspend fun softDelete(uuid: String)

    @Query("DELETE FROM user_gold_assets WHERE uuid = :uuid")
    suspend fun hardDelete(uuid: String)

    @Query("DELETE FROM user_gold_assets WHERE uuid LIKE 'DEMO_TUTORIAL_%'")
    suspend fun clearDemoGold()

    @Query("DELETE FROM user_gold_assets")
    suspend fun clearAll()
}
