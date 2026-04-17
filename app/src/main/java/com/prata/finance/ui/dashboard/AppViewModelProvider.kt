package com.prata.finance.ui.dashboard

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prata.finance.FinanceApplication
import com.prata.finance.ui.add.AddTransactionViewModel
import com.prata.finance.ui.ai.AiAdvisorViewModel
import com.prata.finance.ui.budget.BudgetNegotiatorViewModel
import com.prata.finance.ui.edit.EditTransactionViewModel
import com.prata.finance.ui.history.TransactionHistoryViewModel
import com.prata.finance.ui.manage.ManageCategoriesViewModel
import com.prata.finance.ui.manage.ManageWalletsViewModel
import com.prata.finance.ui.report.ReportViewModel
import com.prata.finance.ui.settings.ResetViewModel
import com.prata.finance.ui.settings.SettingsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer<DashboardViewModel> {
            val application = (this[APPLICATION_KEY] as FinanceApplication)
            DashboardViewModel(financeRepository = application.repository)
        }
        initializer<AddTransactionViewModel> {
            val application = (this[APPLICATION_KEY] as FinanceApplication)
            AddTransactionViewModel(
                financeRepository = application.repository,
                budgetPreferences = application.budgetPreferences
            )
        }
        initializer<EditTransactionViewModel> {
            val application = (this[APPLICATION_KEY] as FinanceApplication)
            EditTransactionViewModel(
                savedStateHandle = this.createSavedStateHandle(),
                financeRepository = application.repository
            )
        }
        initializer<ManageCategoriesViewModel> {
            val application = (this[APPLICATION_KEY] as FinanceApplication)
            ManageCategoriesViewModel(financeRepository = application.repository)
        }
        initializer<ManageWalletsViewModel> {
            val application = (this[APPLICATION_KEY] as FinanceApplication)
            ManageWalletsViewModel(financeRepository = application.repository)
        }
        initializer<ReportViewModel> {
            val application = (this[APPLICATION_KEY] as FinanceApplication)
            ReportViewModel(financeRepository = application.repository)
        }
        initializer<ResetViewModel> {
            val application = (this[APPLICATION_KEY] as FinanceApplication)
            ResetViewModel(financeRepository = application.repository)
        }
        initializer<SettingsViewModel> {
            val application = (this[APPLICATION_KEY] as FinanceApplication)
            SettingsViewModel(financeRepository = application.repository)
        }
        initializer<TransactionHistoryViewModel> {
            val application = (this[APPLICATION_KEY] as FinanceApplication)
            TransactionHistoryViewModel(financeRepository = application.repository)
        }
        initializer<AiAdvisorViewModel> {
            val application = (this[APPLICATION_KEY] as FinanceApplication)
            AiAdvisorViewModel(financeRepository = application.repository)
        }
        initializer<BudgetNegotiatorViewModel> {
            val application = (this[APPLICATION_KEY] as FinanceApplication)
            BudgetNegotiatorViewModel(
                financeRepository = application.repository,
                budgetPreferences = application.budgetPreferences
            )
        }
    }
}
