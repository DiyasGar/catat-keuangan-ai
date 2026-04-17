package com.prata.finance.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prata.finance.data.local.entity.Wallet
import com.prata.finance.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageWalletsViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val currentUserId = 1L

    val wallets: StateFlow<List<Wallet>> = financeRepository.getWalletsByUser(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWallet(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            financeRepository.insertWallet(Wallet(userId = currentUserId, name = name))
        }
    }

    fun updateWallet(wallet: Wallet, newName: String) {
        if (newName.isBlank() || wallet.name == newName) return
        viewModelScope.launch {
            financeRepository.updateWallet(wallet.copy(name = newName))
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch {
            financeRepository.deleteWallet(wallet)
        }
    }
}
