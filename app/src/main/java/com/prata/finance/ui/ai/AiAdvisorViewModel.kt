package com.prata.finance.ui.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prata.finance.BuildConfig
import com.prata.finance.data.local.entity.TransactionType
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
import java.time.ZoneId
import java.util.Locale

private const val TAG = "AiAdvisorViewModel"

// Model gratis dan stabil di OpenRouter — diverifikasi tersedia secara free tier
private const val MODEL_NAME = "openrouter/free"

// Model pesan chat: pengirim bisa "user" atau "ai"
data class ChatMessage(
    val content: String,
    val isFromUser: Boolean
)

data class AiAdvisorUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AiAdvisorViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val currentUserId = 1L

    // API key OpenRouter dari BuildConfig (bersumber dari local.properties)
    private val authHeader = "Bearer ${BuildConfig.OPENROUTER_API_KEY}"

    private val _uiState = MutableStateFlow(AiAdvisorUiState())
    val uiState: StateFlow<AiAdvisorUiState> = _uiState.asStateFlow()

    fun sendChat(userMessage: String) {
        if (userMessage.isBlank()) return

        Log.d(TAG, "Mengirim pesan: $userMessage")
        Log.d(TAG, "Menggunakan model: $MODEL_NAME")

        // Tampilkan pesan user langsung di UI
        val userBubble = ChatMessage(content = userMessage, isFromUser = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userBubble,
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                // Bangun prompt dengan injeksi konteks keuangan user
                val fullPrompt = buildFinancialContext(userMessage)
                Log.d(TAG, "Prompt dibangun (${fullPrompt.length} char). Mengirim ke OpenRouter...")

                // Buat request ke OpenRouter Chat Completions
                val request = ChatRequest(
                    model = MODEL_NAME,
                    messages = listOf(Message(role = "user", content = fullPrompt))
                )

                val response = OpenRouterClient.service.getChatCompletion(
                    authHeader = authHeader,
                    referer = "https://github.com/pratamayp/catat-keuangan",
                    appTitle = "Artha - Pencatat Keuangan",
                    request = request
                )

                // Ambil teks respons dari choices[0].message.content
                val aiReply = response.choices
                    .firstOrNull()
                    ?.message
                    ?.content
                    ?.trim()

                Log.d(TAG, "Respons diterima: ${aiReply?.take(120)}")

                val aiBubble = ChatMessage(
                    content = aiReply ?: "Hmm, gue ga dapat respons. Coba tanya lagi ya!",
                    isFromUser = false
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + aiBubble,
                    isLoading = false
                )

            } catch (e: Exception) {
                // Log full stack trace untuk debugging
                Log.e(TAG, "ERROR saat panggil OpenRouter API!", e)
                e.printStackTrace()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Gagal: ${e::class.simpleName} — ${e.message}"
                )
            }
        }
    }

    // Membangun prompt lengkap dengan riwayat transaksi per-item untuk analisis semantik
    private suspend fun buildFinancialContext(userQuestion: String): String {
        val wallets = financeRepository.getWalletsByUser(currentUserId).first()
        val walletIds = wallets.map { it.id }
        // Map walletId → nama dompet untuk label per transaksi
        val walletMap = wallets.associateBy { it.id }

        Log.d(TAG, "Jumlah dompet user: ${wallets.size}")

        // Jika user belum punya data apapun
        if (walletIds.isEmpty()) {
            return "Kamu adalah penasihat keuangan Indonesia yang asik, pakai gaya bahasa lu/gue. " +
                   "User belum punya dompet atau transaksi apapun. " +
                   "User bertanya: $userQuestion. " +
                   "Jawab pertanyaan, dan kalau soal data keuangan, saranin untuk mulai catat dulu."
        }

        val allTransactions = financeRepository.getTransactionsByUser(currentUserId).first()
        val allCategories   = financeRepository.getCategoriesByWallets(walletIds).first()
        val categoryMap     = allCategories.associateBy { it.id }

        Log.d(TAG, "Jumlah transaksi: ${allTransactions.size}, kategori: ${allCategories.size}")

        val currentDate  = LocalDate.now()
        val currentMonth = currentDate.month
        val currentYear  = currentDate.year
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))

        var monthlyIncome  = 0.0
        var monthlyExpense = 0.0
        var totalBalance   = 0.0
        val expenseByCategory = mutableMapOf<String, Double>()

        // Riwayat transaksi bulan ini dalam format yang bisa dianalisis secara semantik
        val transactionLines = StringBuilder()

        allTransactions.forEach { tx ->
            val cat = categoryMap[tx.categoryId] ?: return@forEach
            val isIncome = cat.type == TransactionType.INCOME
            if (isIncome) totalBalance += tx.amount else totalBalance -= tx.amount

            val txDate = Instant.ofEpochMilli(tx.date)
                .atZone(ZoneId.systemDefault()).toLocalDate()

            if (txDate.month == currentMonth && txDate.year == currentYear) {
                if (isIncome) {
                    monthlyIncome += tx.amount
                } else {
                    monthlyExpense += tx.amount
                    expenseByCategory[cat.name] =
                        (expenseByCategory[cat.name] ?: 0.0) + tx.amount
                }

                // Bangun baris detail per transaksi bulan ini
                val walletName  = walletMap[tx.walletId]?.name ?: "Tidak diketahui"
                val dateLabel   = txDate.format(dateFormatter)
                val typeLabel   = if (isIncome) "Pemasukan" else "Pengeluaran"
                val note        = tx.description.ifBlank { "(tidak ada catatan)" }
                transactionLines.appendLine(
                    "- $dateLabel | Dompet: $walletName | Kategori: ${cat.name} ($typeLabel)" +
                    " | ${formatRupiah(tx.amount)} | Catatan: $note"
                )
            }
        }

        val top3 = expenseByCategory.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { "${it.key}: ${formatRupiah(it.value)}" }
            .ifBlank { "belum ada pengeluaran bulan ini" }

        val walletNames = wallets.joinToString(", ") { it.name }
        val txHistoryBlock = transactionLines.toString().ifBlank { "(tidak ada transaksi bulan ini)" }

        Log.d(TAG, "Transaksi bulan ini:\n$txHistoryBlock")

        return "Kamu adalah penasihat keuangan Indonesia yang asik, pakai gaya bahasa lu/gue dan sedikit skena. " +
               "Jangan terlalu formal, tapi tetap kasih saran yang berguna dan to-the-point. " +
               "DILARANG KERAS MENGARANG ANGKA ATAU KATA. Jawab hanya berdasarkan data transaksi yang diberikan. " +

               // ── Instruksi analisis semantik per catatan transaksi ──────────────────────
               "Gue ngirim riwayat transaksi lengkap beserta catatannya. " +
               "Lu harus analisis catatannya! " +
               "Kalau di kategori yang sama ada catatan yang positif (misal: beli buku, nabung darurat) kasih apresiasi, " +
               "tapi kalau catatannya negatif/konsumtif (misal: belanja online ga jelas, jajan midnight) " +
               "lu harus roasting santai dan kasih advice yang konkret. " +
               "Lu juga bisa filter analisis berdasarkan dompet tertentu kalau user memintanya. " +

               // ── Ringkasan keuangan bulan ini ────────────────────────────────────────────
               "RINGKASAN KEUANGAN BULAN INI: " +
               "Total Saldo Keseluruhan ${formatRupiah(totalBalance)}, " +
               "Pemasukan ${formatRupiah(monthlyIncome)}, " +
               "Pengeluaran ${formatRupiah(monthlyExpense)}, " +
               "Top 3 kategori pengeluaran: $top3. " +
               "Dompet yang dimiliki: $walletNames. " +

               // ── Riwayat transaksi lengkap per item ──────────────────────────────────────
               "RIWAYAT TRANSAKSI BULAN INI (per item):\n$txHistoryBlock" +

               // ── Pertanyaan user ──────────────────────────────────────────────────────────
               "USER BERTANYA: $userQuestion. " +
               "Jawab secara spesifik. Kalau ada transaksi dengan catatan yang mencurigakan atau boros, sebutin namanya!"
    }

    private fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return "Rp${formatter.format(amount.toLong())}"
    }
}
