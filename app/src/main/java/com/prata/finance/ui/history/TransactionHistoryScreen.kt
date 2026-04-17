package com.prata.finance.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prata.finance.data.local.entity.TransactionType
import com.prata.finance.ui.components.ThemedDropdown
import com.prata.finance.ui.components.ThemedDropdownItem
import com.prata.finance.ui.dashboard.AppViewModelProvider
import com.prata.finance.ui.dashboard.TransactionItem
import com.prata.finance.ui.theme.MetallicGold
import com.prata.finance.ui.theme.NavyDark
import com.prata.finance.ui.theme.SlateGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    viewModel: TransactionHistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    // Mengambil data dari ViewModel
    val wallets by viewModel.wallets.collectAsState()
    val selectedWalletId by viewModel.selectedWalletId.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()

    // State untuk membuka/menutup dropdown
    var walletExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    // Label yang ditampilkan berdasarkan pilihan filter
    val selectedWalletName = wallets.find { it.id == selectedWalletId }?.name ?: "Semua Dompet"
    val selectedTypeName = when (selectedType) {
        TransactionType.INCOME -> "Pemasukan"
        TransactionType.EXPENSE -> "Pengeluaran"
        null -> "Semua Tipe"
    }

    Scaffold(
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi", color = MetallicGold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = MetallicGold)
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
        ) {
            // Baris filter: Dompet dan Tipe Transaksi berdampingan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dropdown filter dompet
                Box(modifier = Modifier.weight(1f)) {
                    ThemedDropdown(
                        label = "Dompet",
                        value = selectedWalletName,
                        expanded = walletExpanded,
                        onExpandedChange = { walletExpanded = it }
                    ) {
                        ThemedDropdownItem(text = "Semua Dompet", onClick = {
                            viewModel.selectWallet(null)
                            walletExpanded = false
                        })
                        wallets.forEach { wallet ->
                            ThemedDropdownItem(text = wallet.name, onClick = {
                                viewModel.selectWallet(wallet.id)
                                walletExpanded = false
                            })
                        }
                    }
                }

                // Dropdown filter tipe transaksi
                Box(modifier = Modifier.weight(1f)) {
                    ThemedDropdown(
                        label = "Tipe",
                        value = selectedTypeName,
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        ThemedDropdownItem(text = "Semua Tipe", onClick = {
                            viewModel.selectType(null)
                            typeExpanded = false
                        })
                        ThemedDropdownItem(text = "Pemasukan", onClick = {
                            viewModel.selectType(TransactionType.INCOME)
                            typeExpanded = false
                        })
                        ThemedDropdownItem(text = "Pengeluaran", onClick = {
                            viewModel.selectType(TransactionType.EXPENSE)
                            typeExpanded = false
                        })
                    }
                }
            }

            Divider(color = SlateGrey, modifier = Modifier.padding(horizontal = 16.dp))

            // Daftar transaksi yang sudah difilter
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada transaksi yang sesuai.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = transactions,
                        key = { it.transaction.id }
                    ) { txUi ->
                        TransactionItem(
                            txUi = txUi,
                            onClick = { onNavigateToEditTransaction(txUi.transaction.id) }
                        )
                    }
                }
            }
        }
    }
}
