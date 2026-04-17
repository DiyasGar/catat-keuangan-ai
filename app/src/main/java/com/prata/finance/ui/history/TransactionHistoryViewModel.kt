package com.prata.finance.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prata.finance.data.local.entity.TransactionType
import com.prata.finance.data.local.entity.Wallet
import com.prata.finance.data.repository.FinanceRepository
import com.prata.finance.ui.dashboard.TransactionUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionHistoryViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val currentUserId = 1L

    // Mengambil semua dompet milik pengguna untuk opsi filter
    val wallets: StateFlow<List<Wallet>> = financeRepository.getWalletsByUser(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // State filter: null berarti "Semua Dompet"
    private val _selectedWalletId = MutableStateFlow<Long?>(null)
    val selectedWalletId: StateFlow<Long?> = _selectedWalletId.asStateFlow()

    // State filter: null berarti "Semua Tipe"
    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    val selectedType: StateFlow<TransactionType?> = _selectedType.asStateFlow()

    fun selectWallet(walletId: Long?) { _selectedWalletId.value = walletId }
    fun selectType(type: TransactionType?) { _selectedType.value = type }

    // Flow gabungan: transaksi difilter berdasarkan dompet dan tipe yang dipilih
    val filteredTransactions: StateFlow<List<TransactionUiModel>> = combine(
        _selectedWalletId, _selectedType
    ) { walletId, type -> Pair(walletId, type) }
        .flatMapLatest { (walletId, type) ->
            // Ambil daftar walletId yang relevan
            financeRepository.getWalletsByUser(currentUserId).flatMapLatest { allWallets ->
                val targetWalletIds = if (walletId != null) listOf(walletId)
                                      else allWallets.map { it.id }

                if (targetWalletIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        financeRepository.getTransactionsByUser(currentUserId),
                        financeRepository.getCategoriesByWallets(targetWalletIds)
                    ) { txs, cats ->
                        val categoryMap = cats.associateBy { it.id }

                        txs.mapNotNull { tx ->
                            val cat = categoryMap[tx.categoryId] ?: return@mapNotNull null

                            // Filter berdasarkan dompet jika dipilih
                            if (walletId != null && tx.walletId != walletId) return@mapNotNull null

                            val isIncome = cat.type == TransactionType.INCOME

                            // Filter berdasarkan tipe transaksi jika dipilih
                            if (type != null && cat.type != type) return@mapNotNull null

                            TransactionUiModel(
                                transaction = tx,
                                isIncome = isIncome,
                                categoryName = cat.name
                            )
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
