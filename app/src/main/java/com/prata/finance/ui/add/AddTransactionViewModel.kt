package com.prata.finance.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prata.finance.data.local.entity.Category
import com.prata.finance.data.local.entity.Transaction
import com.prata.finance.data.local.entity.TransactionType
import com.prata.finance.data.local.entity.Wallet
import com.prata.finance.data.preferences.BudgetPreferences
import com.prata.finance.data.repository.FinanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class AddTransactionUiState(
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isSaveSuccessful: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModel(
    private val financeRepository: FinanceRepository,
    private val budgetPreferences: BudgetPreferences
) : ViewModel() {

    private val currentUserId = 1L

    private val _selectedWalletId = MutableStateFlow<Long?>(null)
    private val _isSaveSuccessful = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Jumlah yang sedang diketik user di field input — diupdate real-time dari Screen
    val currentAmountInput = MutableStateFlow("")

    val uiState: StateFlow<AddTransactionUiState> = combine(
        financeRepository.getWalletsByUser(currentUserId),
        _selectedWalletId.flatMapLatest { walletId ->
            if (walletId == null) flowOf(emptyList())
            else financeRepository.getCategoriesByWallet(walletId)
        },
        _isSaveSuccessful,
        _errorMessage
    ) { wallets, categories, isSaved, error ->
        AddTransactionUiState(
            wallets = wallets,
            categories = categories,
            isSaveSuccessful = isSaved,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddTransactionUiState()
    )

    // Total pengeluaran hari ini dari Room DB (hanya expense, bukan income)
    private val todayExpense: StateFlow<Double> = financeRepository
        .getTransactionsByUser(currentUserId)
        .flatMapLatest { transactions ->
            // Ambil semua kategori yang terlibat untuk cek tipenya
            val walletIds = financeRepository.getWalletsByUser(currentUserId)
                .map { it.map { w -> w.id } }
            walletIds.flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf(0.0)
                else financeRepository.getCategoriesByWallets(ids).map { categories ->
                    val categoryMap = categories.associateBy { it.id }
                    val today = LocalDate.now()

                    transactions
                        .filter { tx ->
                            // Hanya transaksi hari ini
                            val txDate = Instant.ofEpochMilli(tx.date)
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            txDate == today
                        }
                        .sumOf { tx ->
                            val cat = categoryMap[tx.categoryId]
                            // Hanya hitung pengeluaran (expense), bukan pemasukan
                            if (cat?.type == TransactionType.EXPENSE) tx.amount else 0.0
                        }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Warning teks reaktif: muncul ketika (pengeluaran hari ini + input baru) melebihi budget harian.
     * Hanya berlaku untuk kategori EXPENSE — tidak muncul saat user input pemasukan.
     * Screen mengobservasi ini dan menampilkannya di bawah field amount.
     */
    val budgetWarning: StateFlow<String?> = combine(
        currentAmountInput,
        todayExpense,
        budgetPreferences.dailyBudget
    ) { amountStr, todayTotal, dailyBudget ->
        // Tidak ada warning jika budget belum diset (0.0)
        if (dailyBudget <= 0.0) return@combine null

        val newAmount = amountStr.toDoubleOrNull() ?: return@combine null
        if (newAmount <= 0.0) return@combine null

        val projected = todayTotal + newAmount
        if (projected > dailyBudget) {
            val formattedBudget = formatRupiah(dailyBudget)
            val formattedProjected = formatRupiah(projected)
            "⚠️ Peringatan: Transaksi ini bikin lu melebihi batas jatah harian lu ($formattedBudget)! " +
            "Total pengeluaran lu hari ini bakal jadi $formattedProjected. " +
            "Yakin tetep mau buang-buang duit?"
        } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onWalletSelected(walletId: Long) {
        _selectedWalletId.value = walletId
    }

    fun saveTransaction(
        walletId: Long?,
        categoryId: Long?,
        amountStr: String,
        description: String,
        dateMillis: Long?
    ) {
        if (walletId == null || categoryId == null) {
            _errorMessage.value = "Dompet dan kategori harus dipilih"
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _errorMessage.value = "Jumlah harus berupa angka lebih dari 0"
            return
        }
        if (description.isBlank()) {
            _errorMessage.value = "Deskripsi tidak boleh kosong"
            return
        }
        val date = dateMillis ?: System.currentTimeMillis()

        viewModelScope.launch {
            try {
                financeRepository.insertTransaction(
                    Transaction(
                        userId = currentUserId,
                        walletId = walletId,
                        categoryId = categoryId,
                        amount = amount,
                        description = description,
                        date = date
                    )
                )
                _isSaveSuccessful.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menyimpan: ${e.message}"
            }
        }
    }

    fun resetState() {
        _isSaveSuccessful.value = false
        _errorMessage.value = null
    }

    private fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return "Rp${formatter.format(amount.toLong())}"
    }
}
