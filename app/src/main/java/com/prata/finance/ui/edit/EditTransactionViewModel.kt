package com.prata.finance.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prata.finance.data.local.entity.Category
import com.prata.finance.data.local.entity.Transaction
import com.prata.finance.data.local.entity.Wallet
import com.prata.finance.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

data class EditTransactionUiState(
    val selectedTransaction: Transaction? = null,
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isUpdatedOrDeleted: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class EditTransactionViewModel(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val transactionId: Long = checkNotNull(savedStateHandle["transactionId"])
    private val currentUserId = 1L

    private val _uiState = MutableStateFlow(EditTransactionUiState())
    val uiState: StateFlow<EditTransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            financeRepository.getWalletsByUser(currentUserId)
                .flatMapLatest { wallets: List<Wallet> ->
                    val walletIds = wallets.map { it.id }
                    if (walletIds.isEmpty()) {
                        flowOf(Triple(wallets, emptyList<Category>(), null))
                    } else {
                        combine(
                            financeRepository.getCategoriesByWallets(walletIds),
                            financeRepository.getTransactionById(transactionId)
                        ) { cats: List<Category>, tx: Transaction? ->
                            Triple(wallets, cats, tx)
                        }
                    }
                }.collect { (wallets, cats, tx) ->
                _uiState.update { state ->
                    state.copy(
                        selectedTransaction = tx,
                        wallets = wallets,
                        categories = cats
                    )
                }
            }
        }
    }

    fun updateTransaction(walletId: Long?, categoryId: Long?, amountStr: String, description: String, dateMillis: Long?) {
        val tx = _uiState.value.selectedTransaction ?: return
        if (walletId == null || categoryId == null) {
            _uiState.update { it.copy(errorMessage = "Wallet and Category must be selected") }
            return
        }
        val amount = amountStr.replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Amount must be a valid positive number") }
            return
        }
        viewModelScope.launch {
            try {
                financeRepository.updateTransaction(
                    tx.copy(
                        walletId = walletId,
                        categoryId = categoryId,
                        amount = amount,
                        description = description,
                        date = dateMillis ?: System.currentTimeMillis()
                    )
                )
                _uiState.update { it.copy(isUpdatedOrDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update transaction.") }
            }
        }
    }

    fun deleteTransaction() {
        val tx = _uiState.value.selectedTransaction ?: return
        viewModelScope.launch {
            try {
                financeRepository.deleteTransaction(tx)
                _uiState.update { it.copy(isUpdatedOrDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete transaction.") }
            }
        }
    }
}
