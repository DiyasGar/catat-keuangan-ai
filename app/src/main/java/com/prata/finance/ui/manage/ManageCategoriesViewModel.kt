package com.prata.finance.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prata.finance.data.local.entity.Category
import com.prata.finance.data.local.entity.TransactionType
import com.prata.finance.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.prata.finance.data.local.entity.Wallet
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ManageCategoriesViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val currentUserId = 1L

    private val _selectedWalletId = MutableStateFlow<Long?>(null)

    val wallets: StateFlow<List<Wallet>> = financeRepository.getWalletsByUser(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = _selectedWalletId.flatMapLatest { walletId ->
        if (walletId == null) {
            flowOf(emptyList())
        } else {
            financeRepository.getCategoriesByWallet(walletId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectWallet(walletId: Long) {
        _selectedWalletId.value = walletId
    }

    fun addCategory(name: String, type: TransactionType) {
        val walletId = _selectedWalletId.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            financeRepository.insertCategory(Category(name = name, type = type, walletId = walletId))
        }
    }

    fun updateCategory(category: Category, newName: String, newType: TransactionType) {
        if (newName.isBlank()) return
        if (category.name == newName && category.type == newType) return
        viewModelScope.launch {
            financeRepository.updateCategory(category.copy(name = newName, type = newType))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            financeRepository.deleteCategory(category)
        }
    }
}
