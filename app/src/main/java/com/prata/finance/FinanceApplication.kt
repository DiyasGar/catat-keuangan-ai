package com.prata.finance

import android.app.Application
import com.prata.finance.data.local.AppDatabase
import com.prata.finance.data.preferences.BudgetPreferences
import com.prata.finance.data.repository.FinanceRepository

class FinanceApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { 
        FinanceRepository(
            userDao = database.userDao(),
            walletDao = database.walletDao(),
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao()
        )
    }

    // Instance tunggal BudgetPreferences — bisa diakses dari ViewModel via APPLICATION_KEY
    val budgetPreferences by lazy { BudgetPreferences(applicationContext) }
}
