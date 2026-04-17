package com.prata.finance.data.repository

import com.prata.finance.data.local.dao.*
import com.prata.finance.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class FinanceRepository(
    private val userDao: UserDao,
    private val walletDao: WalletDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {
    // --- User Operations ---
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
    
    fun getUserById(id: Long): Flow<User?> = userDao.getUserById(id)
    
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    // --- Wallet Operations ---
    // Strict isolation: always fetched by userId
    fun getWalletsByUser(userId: Long): Flow<List<Wallet>> = walletDao.getWalletsByUser(userId)
    
    suspend fun insertWallet(wallet: Wallet): Long = walletDao.insertWallet(wallet)
    
    suspend fun updateWallet(wallet: Wallet) = walletDao.updateWallet(wallet)
    
    suspend fun deleteWallet(wallet: Wallet) = walletDao.deleteWallet(wallet)

    // Note: initializeUserWallets is removed to ensure 0 default wallets.

    // --- Category Operations ---
    fun getCategoriesByWallet(walletId: Long): Flow<List<Category>> = categoryDao.getCategoriesByWallet(walletId)
    
    fun getCategoriesByWallets(walletIds: List<Long>): Flow<List<Category>> = categoryDao.getCategoriesByWallets(walletIds)
    
    fun getCategoriesByWalletAndType(walletId: Long, type: TransactionType): Flow<List<Category>> = categoryDao.getCategoriesByWalletAndType(walletId, type)
    
    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    // --- Transaction Operations ---
    fun getTransactionById(id: Long): Flow<Transaction?> = transactionDao.getTransactionById(id)

    // Strict isolation: always fetched by userId or walletId
    fun getTransactionsByUser(userId: Long): Flow<List<Transaction>> = transactionDao.getTransactionsByUser(userId)
    
    fun getTransactionsByWallet(walletId: Long): Flow<List<Transaction>> = transactionDao.getTransactionsByWallet(walletId)
    
    suspend fun insertTransaction(transaction: Transaction): Long = transactionDao.insertTransaction(transaction)
    
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)

    suspend fun deleteAllTransactionsByUser(userId: Long) = transactionDao.deleteAllTransactionsByUser(userId)
}
