package com.prata.finance.ui.budget

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prata.finance.BuildConfig
import com.prata.finance.data.local.entity.TransactionType
import com.prata.finance.data.preferences.BudgetPreferences
import com.prata.finance.data.remote.ChatRequest
import com.prata.finance.data.remote.Message
import com.prata.finance.data.remote.OpenRouterClient
import com.prata.finance.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

private const val TAG = "BudgetNegotiatorVM"
private const val MODEL_NAME = "openai/gpt-oss-120b:free"

// Regex untuk mendeteksi kode deal dari AI: [DEAL: 50000]
private val DEAL_REGEX = Regex("""\[DEAL:\s*(\d+)\]""")

// Pesan UI yang tampil di chat — termasuk flag khusus jika deal tercapai
data class NegotiatorMessage(
    val content: String,
    val isFromUser: Boolean,
    val isDealMessage: Boolean = false
)

data class BudgetNegotiatorUiState(
    val messages: List<NegotiatorMessage> = emptyList(),
    val isLoading: Boolean = false,
    val dealBudget: Double? = null,      // null = belum deal, angka = budget harian yang disepakati
    val errorMessage: String? = null,
    val isInitializing: Boolean = true   // true saat data keuangan sedang difetch di awal
)

class BudgetNegotiatorViewModel(
    private val financeRepository: FinanceRepository,
    private val budgetPreferences: BudgetPreferences
) : ViewModel() {

    private val currentUserId = 1L
    private val authHeader = "Bearer ${BuildConfig.OPENROUTER_API_KEY}"

    // Riwayat percakapan lengkap untuk API (termasuk system message) — tidak ditampilkan ke UI
    private val conversationHistory = mutableListOf<Message>()

    private val _uiState = MutableStateFlow(BudgetNegotiatorUiState())
    val uiState: StateFlow<BudgetNegotiatorUiState> = _uiState.asStateFlow()

    init {
        // Mulai sesi negosiasi otomatis saat ViewModel dibuat
        startNegotiationSession()
    }

    private fun startNegotiationSession() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isInitializing = true, isLoading = true)

                // Kumpulkan data keuangan dari database
                val wallets = financeRepository.getWalletsByUser(currentUserId).first()
                val walletIds = wallets.map { it.id }
                val allTransactions = financeRepository.getTransactionsByUser(currentUserId).first()
                val allCategories = if (walletIds.isEmpty()) emptyList()
                    else financeRepository.getCategoriesByWallets(walletIds).first()
                val categoryMap = allCategories.associateBy { it.id }

                val today = LocalDate.now()
                val currentMonth = today.month
                val currentYear = today.year
                val daysInMonth = YearMonth.of(currentYear, currentMonth).lengthOfMonth()
                val remainingDays = daysInMonth - today.dayOfMonth + 1

                // ── Hitung saldo per dompet (all-time) dan rincian pengeluaran per kategori (bulan ini) ──
                val walletBalances = mutableMapOf<Long, Double>()
                wallets.forEach { walletBalances[it.id] = 0.0 }

                var monthlyIncome = 0.0
                var monthlyExpense = 0.0
                val expenseByCategory = mutableMapOf<String, Double>()

                allTransactions.forEach { tx ->
                    val cat = categoryMap[tx.categoryId] ?: return@forEach
                    val txDate = Instant.ofEpochMilli(tx.date)
                        .atZone(ZoneId.systemDefault()).toLocalDate()

                    // Saldo per dompet: dihitung dari ALL TIME
                    if (cat.type == TransactionType.INCOME) {
                        walletBalances[tx.walletId] = (walletBalances[tx.walletId] ?: 0.0) + tx.amount
                    } else {
                        walletBalances[tx.walletId] = (walletBalances[tx.walletId] ?: 0.0) - tx.amount
                    }

                    // Pemasukan & pengeluaran: hanya bulan berjalan
                    if (txDate.month == currentMonth && txDate.year == currentYear) {
                        if (cat.type == TransactionType.INCOME) {
                            monthlyIncome += tx.amount
                        } else {
                            monthlyExpense += tx.amount
                            expenseByCategory[cat.name] =
                                (expenseByCategory[cat.name] ?: 0.0) + tx.amount
                        }
                    }
                }

                // Format rincian saldo per dompet
                val walletBreakdown = wallets.joinToString("; ") { w ->
                    "${w.name} (${formatRupiah(walletBalances[w.id] ?: 0.0)})"
                }.ifBlank { "Tidak ada dompet" }

                // Format rincian pengeluaran per kategori, diurutkan dari terbesar
                val categoryBreakdown = expenseByCategory.entries
                    .sortedByDescending { it.value }
                    .joinToString("; ") { (name, amt) -> "$name (${formatRupiah(amt)})" }
                    .ifBlank { "Tidak ada pengeluaran bulan ini" }

                Log.d(TAG, "Data siap: income=$monthlyIncome, expense=$monthlyExpense, sisa=$remainingDays hari")

                // Bangun system prompt dengan data keuangan lengkap
                val systemPrompt = buildSystemPrompt(
                    income = monthlyIncome,
                    expense = monthlyExpense,
                    remainingDays = remainingDays,
                    walletBreakdown = walletBreakdown,
                    categoryBreakdown = categoryBreakdown
                )

                // Simpan system message ke riwayat API (tidak ditampilkan ke UI)
                // Pesan pertama dari user baru akan dikirim saat mereka klik salah satu chip
                conversationHistory.add(Message(role = "system", content = systemPrompt))

                // Data siap, tampilkan empty state dengan suggestion chips
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitializing = false
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error saat memulai sesi negosiasi", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitializing = false,
                    errorMessage = "Gagal memulai sesi: ${e.message}"
                )
            }
        }
    }

    fun sendOffer(userMessage: String) {
        if (userMessage.isBlank()) return
        // Jangan izinkan input baru jika deal sudah tercapai
        if (_uiState.value.dealBudget != null) return

        val userBubble = NegotiatorMessage(content = userMessage, isFromUser = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userBubble,
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                conversationHistory.add(Message(role = "user", content = userMessage))

                val response = sendToApi()
                val aiReply = response ?: "Hmm, gue ga dapat respons. Coba lagi!"

                conversationHistory.add(Message(role = "assistant", content = aiReply))

                val dealAmount = extractDeal(aiReply)
                val cleanText = cleanResponseText(aiReply)

                val aiBubble = NegotiatorMessage(
                    content = cleanText,
                    isFromUser = false,
                    isDealMessage = dealAmount != null
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + aiBubble,
                    isLoading = false,
                    dealBudget = dealAmount?.also { saveDeal(it) }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error saat mengirim tawaran", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Gagal: ${e::class.simpleName} — ${e.message}"
                )
            }
        }
    }

    // Kirim conversationHistory ke OpenRouter dan kembalikan teks respons
    private suspend fun sendToApi(): String? {
        val request = ChatRequest(
            model = MODEL_NAME,
            messages = conversationHistory.toList(),
            temperature = 0.3  // Sedikit lebih kreatif untuk negosiasi, tapi tetap faktual
        )
        return try {
            val response = OpenRouterClient.service.getChatCompletion(
                authHeader = authHeader,
                referer = "https://github.com/pratamayp/catat-keuangan",
                appTitle = "Artha - Budget Negotiator",
                request = request
            )
            response.choices.firstOrNull()?.message?.content?.trim()
        } catch (e: Exception) {
            Log.e(TAG, "API call gagal", e)
            throw e
        }
    }

    // Parsing kode [DEAL: 50000] dari teks respons AI
    private fun extractDeal(text: String): Double? {
        val match = DEAL_REGEX.find(text) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }

    // Hapus kode [DEAL: ...] dari teks sebelum ditampilkan ke user
    private fun cleanResponseText(text: String): String =
        text.replace(DEAL_REGEX, "").trim()

    // Simpan deal ke DataStore secara asinkron
    private fun saveDeal(amount: Double) {
        viewModelScope.launch {
            budgetPreferences.saveDailyBudget(amount)
            Log.d(TAG, "Budget harian tersimpan: ${formatRupiah(amount)}")
        }
    }

    private fun buildSystemPrompt(
        income: Double,
        expense: Double,
        remainingDays: Int,
        walletBreakdown: String,
        categoryBreakdown: String
    ): String {
        val saldo = income - expense
        return "Kamu adalah AI Financial Planner galak tapi asik yang berbicara dalam bahasa Indonesia dengan gaya lu/gue. " +

               // ── Data keuangan lengkap dengan rincian per dompet & kategori ──────────────
               "DATA KEUANGAN USER BULAN INI: " +
               "Pemasukan ${formatRupiah(income)}, " +
               "Pengeluaran ${formatRupiah(expense)}, " +
               "Sisa saldo bulan ini ${formatRupiah(saldo)}, " +
               "Sisa hari di bulan ini $remainingDays hari. " +
               "Rincian Saldo Per Dompet (saldo saat ini): $walletBreakdown. " +
               "Pengeluaran Per Kategori Bulan Ini: $categoryBreakdown. " +

               // ── Aturan mutlak: jangan gunakan dompet tabungan untuk hitung jatah harian ──
               "ATURAN MUTLAK: Saat menghitung rekomendasi jatah harian pribadi, " +
               "lu HANYA BOLEH menggunakan saldo dari dompet yang secara logika untuk harian " +
               "(misal: 'dompet jajan harian', 'uang hidup', 'jajan', dll). " +
               "DILARANG KERAS membagi total keseluruhan saldo. " +
               "Uang di dompet seperti 'nabung', 'darurat', atau 'berdua/doi' HARAM disentuh " +
               "untuk perhitungan jajan harian pribadi. " +
               "Evaluasi juga proporsi pengeluarannya secara spesifik tiap kategori! " +

               // ── Aturan negosiasi dan format deal ─────────────────────────────────────────
               "User boleh menawar angka tersebut dan kamu boleh bernegosiasi. " +
               "PENTING: Jika user setuju atau kamu setuju dengan suatu angka (deal tercapai), " +
               "KAMU WAJIB MENCETAK KODE INI PERSIS DI AKHIR PESANMU: [DEAL: <angka_tanpa_titik_atau_koma>] " +
               "Contoh: jika deal di Rp50.000, tulis [DEAL: 50000]. " +
               "Jangan cetak kode itu sebelum deal benar-benar tercapai dan disepakati kedua pihak."
    }

    private fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return "Rp${formatter.format(amount.toLong())}"
    }
}
