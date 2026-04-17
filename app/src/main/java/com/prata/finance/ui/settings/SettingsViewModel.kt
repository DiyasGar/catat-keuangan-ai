package com.prata.finance.ui.settings

import androidx.lifecycle.ViewModel
import com.prata.finance.data.local.entity.TransactionType
import com.prata.finance.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SettingsViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val currentUserId = 1L

    suspend fun getExportCsvData(): String {
        val transactions = financeRepository.getTransactionsByUser(currentUserId).first()
        if (transactions.isEmpty()) return "Tanggal,Dompet,Kategori,Tipe,Nominal,Catatan\n"

        val wallets = financeRepository.getWalletsByUser(currentUserId).first().associateBy { it.id }
        val categoryList = mutableListOf<com.prata.finance.data.local.entity.Category>()
        
        val walletIds = wallets.keys.toList()
        if (walletIds.isNotEmpty()) {
            categoryList.addAll(financeRepository.getCategoriesByWallets(walletIds).first())
        }
        val categories = categoryList.associateBy { it.id }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        val csvBuilder = java.lang.StringBuilder()
        // Header
        csvBuilder.append("Tanggal,Dompet,Kategori,Tipe,Nominal,Catatan\n")

        transactions.forEach { tx ->
            val dateStr = Instant.ofEpochMilli(tx.date).atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatter)
            val walletName = wallets[tx.walletId]?.name?.replace(",", ";") ?: "Unknown"
            val category = categories[tx.categoryId]
            val catName = category?.name?.replace(",", ";") ?: "Unknown"
            val typeStr = if (category?.type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran"
            val safeNote = tx.description.replace(",", ";").replace("\n", " ")

            csvBuilder.append("$dateStr,$walletName,$catName,$typeStr,${tx.amount},$safeNote\n")
        }

        return csvBuilder.toString()
    }
}
