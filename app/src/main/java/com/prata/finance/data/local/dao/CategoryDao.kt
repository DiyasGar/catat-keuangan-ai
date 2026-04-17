package com.prata.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prata.finance.data.local.entity.Category
import com.prata.finance.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE walletId = :walletId")
    fun getCategoriesByWallet(walletId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE walletId IN (:walletIds)")
    fun getCategoriesByWallets(walletIds: List<Long>): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE walletId = :walletId AND type = :type")
    fun getCategoriesByWalletAndType(walletId: Long, type: TransactionType): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}
