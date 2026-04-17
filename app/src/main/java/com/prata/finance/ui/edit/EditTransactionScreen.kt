package com.prata.finance.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun EditTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditTransactionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    // Tampilkan loading jika transaksi belum selesai dimuat dari database
    val tx = uiState.selectedTransaction
    if (tx == null) {
        Scaffold(containerColor = NavyDark) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                CircularProgressIndicator(color = MetallicGold, modifier = Modifier.padding(16.dp))
            }
        }
        return
    }

    // State form
    var initialized by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var txDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Inisialisasi nilai awal form sekali saja ketika data sudah tersedia
    if (!initialized && uiState.wallets.isNotEmpty() && uiState.categories.isNotEmpty()) {
        amount = tx.amount.toString().let { if (it.endsWith(".0")) it.dropLast(2) else it }
        description = tx.description
        selectedWallet = uiState.wallets.find { it.id == tx.walletId }
        selectedCategory = uiState.categories.find { it.id == tx.categoryId }
        txDateMillis = tx.date
        initialized = true
    }

    // State untuk dropdown dan date picker
    var walletExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = txDateMillis)
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    val dateString = sdf.format(Date(datePickerState.selectedDateMillis ?: txDateMillis))

    // Kembali ke halaman sebelumnya setelah update atau hapus berhasil
    LaunchedEffect(uiState.isUpdatedOrDeleted) {
        if (uiState.isUpdatedOrDeleted) onNavigateBack()
    }

    Scaffold(
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                title = { Text("Ubah Transaksi", color = MetallicGold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = MetallicGold)
                    }
                },
                actions = {
                    // Tombol hapus transaksi di pojok kanan TopAppBar
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tampilkan pesan error jika ada
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Field jumlah
            DarkTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Jumlah (Rp)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Field deskripsi
            DarkTextField(
                value = description,
                onValueChange = { description = it },
                label = "Deskripsi"
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
                    })
                }
            }

            // Dropdown pilih kategori
            ThemedDropdown(
                label = "Kategori",
                value = selectedCategory?.name ?: "Pilih Kategori",
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                uiState.categories.forEach { category ->
                    val tipeLabel = if (category.type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran"
                    ThemedDropdownItem(text = "${category.name} ($tipeLabel)", onClick = {
                        selectedCategory = category
                        categoryExpanded = false
                    })
                }
            }

            // Field tanggal, diklik untuk membuka dialog kalender
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
                        TextButton(onClick = { showDatePicker = false }) { Text("Pilih", color = MetallicGold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Batal", color = MetallicGold) }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tombol simpan perubahan
            ElegantButton(
                text = "Simpan Perubahan",
                onClick = {
                    viewModel.updateTransaction(
                        walletId = selectedWallet?.id,
                        categoryId = selectedCategory?.id,
                        amountStr = amount,
                        description = description,
                        dateMillis = datePickerState.selectedDateMillis
                    )
                }
            )
        }

        // Dialog konfirmasi hapus transaksi
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Hapus Transaksi", color = MetallicGold) },
                text = { Text("Yakin ingin menghapus transaksi ini? Data tidak dapat dikembalikan.", color = Color.White) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTransaction()
                    }) {
                        Text("Ya, Hapus", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Batal", color = MetallicGold)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}
