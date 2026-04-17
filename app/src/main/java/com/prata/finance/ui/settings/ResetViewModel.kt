package com.prata.finance.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prata.finance.data.repository.FinanceRepository
import kotlinx.coroutines.launch

class ResetViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val currentUserId = 1L

    fun deleteAllData() {
        viewModelScope.launch {
            financeRepository.deleteAllTransactionsByUser(currentUserId)
        }
    }
}
