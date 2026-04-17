package com.prata.finance.ui.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prata.finance.data.local.entity.Category
import com.prata.finance.data.local.entity.TransactionType
import com.prata.finance.data.local.entity.Wallet
import com.prata.finance.ui.components.DarkTextField
import com.prata.finance.ui.components.ElegantButton
import com.prata.finance.ui.components.ThemedDropdown
import com.prata.finance.ui.components.ThemedDropdownItem
import com.prata.finance.ui.dashboard.AppViewModelProvider
import com.prata.finance.ui.theme.MetallicGold
import com.prata.finance.ui.theme.NavyDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    // Mengambil data dompet, kategori, dan peringatan budget dari ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val budgetWarning by viewModel.budgetWarning.collectAsState()

    // State untuk input form
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // State untuk dropdown dompet
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    // State untuk dropdown kategori
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // State untuk date picker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    val dateString = sdf.format(Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis()))

    // Kembali ke halaman sebelumnya setelah berhasil menyimpan
    LaunchedEffect(uiState.isSaveSuccessful) {
        if (uiState.isSaveSuccessful) {
            viewModel.resetState()
            onNavigateBack()
        }
    }

    Scaffold(containerColor = NavyDark) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tambah Transaksi",
                color = MetallicGold,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Tampilkan pesan error jika ada
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Field jumlah transaksi
            DarkTextField(
                value = amount,
                onValueChange = { newVal ->
                    amount = newVal
                    // Kirim nilai terbaru ke ViewModel untuk kalkulasi warning reaktif
                    viewModel.currentAmountInput.value = newVal
                },
                label = "Jumlah (Rp)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Peringatan budget harian — muncul secara reaktif saat input melebihi jatah
            if (budgetWarning != null) {
                Text(
                    text = budgetWarning!!,
                    color = Color(0xFFFF6B6B),  // merah cerah agar mencolok
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Field deskripsi/keterangan
            DarkTextField(
                value = description,
                onValueChange = { description = it },
                label = "Deskripsi / Keterangan"
            )

            // Dropdown pilih dompet
            ThemedDropdown(
                label = "Dompet",
                value = selectedWallet?.name ?: "Pilih Dompet",
                expanded = walletExpanded,
                onExpandedChange = { walletExpanded = it }
            ) {
                uiState.wallets.forEach { wallet ->
                    ThemedDropdownItem(text = wallet.name, onClick = {
                        selectedWallet = wallet
                        walletExpanded = false
                        viewModel.onWalletSelected(wallet.id)
                        selectedCategory = null // Reset kategori saat dompet berubah
                    })
                }
            }

            // Dropdown pilih kategori (aktif hanya jika dompet sudah dipilih)
            val categoryEnabled = selectedWallet != null
            ThemedDropdown(
                label = "Kategori",
                value = selectedCategory?.name ?: if (categoryEnabled) "Pilih Kategori" else "Pilih Dompet Dahulu",
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                enabled = categoryEnabled
            ) {
                uiState.categories.forEach { category ->
                    val tipeLabel = if (category.type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran"
                    ThemedDropdownItem(text = "${category.name} ($tipeLabel)", onClick = {
                        selectedCategory = category
                        categoryExpanded = false
                    })
                }
            }

            // Field tanggal transaksi, diklik untuk membuka date picker
            Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                DarkTextField(
                    value = dateString,
                    onValueChange = {},
                    label = "Tanggal Transaksi",
                    enabled = false
                )
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Pilih", color = MetallicGold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Batal", color = MetallicGold)
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tombol simpan transaksi
            ElegantButton(
                text = "Simpan Transaksi",
                onClick = {
                    viewModel.saveTransaction(
                        walletId = selectedWallet?.id,
                        categoryId = selectedCategory?.id,
                        amountStr = amount,
                        description = description,
                        dateMillis = datePickerState.selectedDateMillis
                    )
                }
            )
        }
    }
}
