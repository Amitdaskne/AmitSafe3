package com.amitdas.amitsafe.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val originalPath: String,
    val vaultPath: String,
    val fileName: String,
    val fileType: String, // "image" or "video"
    val fileSize: Long,
    val dateAdded: Long,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedDate: Long = 0L
)

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items WHERE isDeleted = 0 ORDER BY dateAdded DESC")
    fun getAllActive(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavorites(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE isDeleted = 1 ORDER BY deletedDate DESC")
    fun getRecycleBin(): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultItem): Long

    @Query("UPDATE vault_items SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFav: Boolean)

    @Query("UPDATE vault_items SET isDeleted = 1, deletedDate = :deletedDate WHERE id = :id")
    suspend fun moveToRecycleBin(id: Long, deletedDate: Long)

    @Query("UPDATE vault_items SET isDeleted = 0, deletedDate = 0 WHERE id = :id")
    suspend fun restoreFromRecycleBin(id: Long)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VaultItem?

    @Query("DELETE FROM vault_items WHERE isDeleted = 1")
    suspend fun clearRecycleBin()
}

@Database(entities = [VaultItem::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun dao(): VaultItemDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "amitsafe_vault_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
