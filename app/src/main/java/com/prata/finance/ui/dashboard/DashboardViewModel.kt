package com.prata.finance.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prata.finance.data.local.entity.Category
import com.prata.finance.data.local.entity.Transaction
import com.prata.finance.data.local.entity.TransactionType
import com.prata.finance.data.local.entity.Wallet
import com.prata.finance.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WalletUiModel(
    val wallet: Wallet,
    val balance: Double
)

data class TransactionUiModel(
    val transaction: Transaction,
    val isIncome: Boolean,
    val categoryName: String
)



data class DashboardUiState(
    val totalSaldo: Double = 0.0,
    val currentMonthIncome: Double = 0.0,
    val currentMonthExpense: Double = 0.0,
    val walletsList: List<WalletUiModel> = emptyList(),
    val recentTransactions: List<TransactionUiModel> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    // Mocked User ID since Auth isn't finalized
    private val currentUserId = 1L

    val uiState: StateFlow<DashboardUiState> = financeRepository.getWalletsByUser(currentUserId)
        .flatMapLatest { wallets: List<Wallet> ->
            val walletIds = wallets.map { it.id }
            if (walletIds.isEmpty()) {
                flowOf(Triple(wallets, emptyList<Transaction>(), emptyList<Category>()))
            } else {
                combine(
                    financeRepository.getTransactionsByUser(currentUserId),
                    financeRepository.getCategoriesByWallets(walletIds)
                ) { txs: List<Transaction>, cats: List<Category> ->
                    Triple(wallets, txs, cats)
                }
            }
        }.map { (wallets: List<Wallet>, transactions: List<Transaction>, categories: List<Category>) ->
        val currentLocalDate = LocalDate.now()
        val currentMonth = currentLocalDate.month
        val currentYear = currentLocalDate.year

        val categoryMap = categories.associateBy { it.id }

        var totalIncome = 0.0
        var totalExpense = 0.0
        var totalSaldo = 0.0
        
        val walletBalances = mutableMapOf<Long, Double>()
        wallets.forEach { walletBalances[it.id] = 0.0 }

        val mappedTransactions = transactions.mapNotNull { tx ->
            val cat = categoryMap[tx.categoryId] ?: return@mapNotNull null
            val amount = tx.amount
            
            val isIncome = cat.type == TransactionType.INCOME

            if (isIncome) {
                totalSaldo += amount
                walletBalances[tx.walletId] = (walletBalances[tx.walletId] ?: 0.0) + amount
            } else {
                totalSaldo -= amount
                walletBalances[tx.walletId] = (walletBalances[tx.walletId] ?: 0.0) - amount
            }

            // Check if transaction is within the current month/year
            val txDate = Instant.ofEpochMilli(tx.date).atZone(ZoneId.systemDefault()).toLocalDate()
            if (txDate.month == currentMonth && txDate.year == currentYear) {
                if (isIncome) {
                    totalIncome += amount
                } else {
                    totalExpense += amount
                }
            }
            
            TransactionUiModel(
                transaction = tx,
                isIncome = isIncome,
                categoryName = cat.name
            )
        }

        val mappedWallets = wallets.map { wallet ->
            WalletUiModel(
                wallet = wallet,
                balance = walletBalances[wallet.id] ?: 0.0
            )
        }

        DashboardUiState(
            totalSaldo = totalSaldo,
            currentMonthIncome = totalIncome,
            currentMonthExpense = totalExpense,
            walletsList = mappedWallets,
            // limit to top 10 recent transactions
            recentTransactions = mappedTransactions.take(10),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
