package com.prata.finance.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prata.finance.data.local.entity.TransactionType
import com.prata.finance.data.local.entity.Wallet
import com.prata.finance.data.repository.FinanceRepository
import com.prata.finance.ui.components.ExpenseCategoryStat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val currentUserId = 1L

    val wallets: StateFlow<List<Wallet>> = financeRepository.getWalletsByUser(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedWalletId = MutableStateFlow<Long?>(null)
    val selectedWalletId: StateFlow<Long?> = _selectedWalletId.asStateFlow()

    private val currentLocalDate = LocalDate.now()
    private val _selectedMonth = MutableStateFlow(currentLocalDate.monthValue)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(currentLocalDate.year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    fun selectWallet(walletId: Long) {
        _selectedWalletId.value = walletId
    }

    fun selectMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun selectYear(year: Int) {
        _selectedYear.value = year
    }

    val expenseStats: StateFlow<List<ExpenseCategoryStat>> = combine(
        _selectedWalletId, _selectedMonth, _selectedYear
    ) { walletId, month, year ->
        Triple(walletId, month, year)
    }.flatMapLatest { (walletId, month, year) ->
        if (walletId == null) {
            flowOf(emptyList())
        } else {
            combine(
                financeRepository.getTransactionsByWallet(walletId),
                financeRepository.getCategoriesByWallet(walletId)
            ) { txs, cats ->
                val categoryMap = cats.associateBy { it.id }
                val expenseTotals = mutableMapOf<Long, Double>()
                
                txs.forEach { tx ->
                    val txDate = java.time.Instant.ofEpochMilli(tx.date)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    
                    if (txDate.monthValue == month && txDate.year == year) {
                        val cat = categoryMap[tx.categoryId]
                        if (cat != null && cat.type == TransactionType.EXPENSE) {
                            expenseTotals[cat.id] = (expenseTotals[cat.id] ?: 0.0) + tx.amount
                        }
                    }
                }
                
                expenseTotals.mapNotNull { (catId, amount) ->
                    val catName = categoryMap[catId]?.name ?: return@mapNotNull null
                    ExpenseCategoryStat(catName, amount)
                }.sortedByDescending { it.totalAmount }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
