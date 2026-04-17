package com.prata.finance.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prata.finance.ui.components.FinanceStatChart
import com.prata.finance.ui.components.ThemedDropdown
import com.prata.finance.ui.components.ThemedDropdownItem
import com.prata.finance.ui.dashboard.AppViewModelProvider
import com.prata.finance.ui.theme.MetallicGold
import com.prata.finance.ui.theme.NavyDark
import com.prata.finance.ui.theme.OffWhite
import com.prata.finance.ui.theme.SlateGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    // Mengambil data dari ViewModel
    val wallets by viewModel.wallets.collectAsState()
    val selectedWalletId by viewModel.selectedWalletId.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val expenseStats by viewModel.expenseStats.collectAsState()

    // State untuk membuka/menutup setiap dropdown
    var walletExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    val months = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    val years = (2024..2050).toList()
    val selectedWallet = wallets.find { it.id == selectedWalletId }

    Scaffold(
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                title = { Text("Rekap Keuangan", color = MetallicGold) },
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
            // Dropdown untuk memilih dompet yang ingin dilihat rekapnya
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                ThemedDropdown(
                    label = "Dompet",
                    value = selectedWallet?.name ?: "Pilih Dompet",
                    expanded = walletExpanded,
                    onExpandedChange = { walletExpanded = it }
                ) {
                    wallets.forEach { wallet ->
                        ThemedDropdownItem(text = wallet.name, onClick = {
                            viewModel.selectWallet(wallet.id)
                            walletExpanded = false
                        })
                    }
                }
            }

            // Selector periode: pilih bulan dan tahun untuk filter data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dropdown bulan
                Box(modifier = Modifier.weight(1f)) {
                    ThemedDropdown(
                        label = "Bulan",
                        value = months[selectedMonth - 1],
                        expanded = monthExpanded,
                        onExpandedChange = { monthExpanded = it }
                    ) {
                        months.forEachIndexed { index, monthName ->
                            ThemedDropdownItem(text = monthName, onClick = {
                                viewModel.selectMonth(index + 1)
                                monthExpanded = false
                            })
                        }
                    }
                }

                // Dropdown tahun
                Box(modifier = Modifier.weight(1f)) {
                    ThemedDropdown(
                        label = "Tahun",
                        value = selectedYear.toString(),
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = it }
                    ) {
                        years.forEach { year ->
                            ThemedDropdownItem(text = year.toString(), onClick = {
                                viewModel.selectYear(year)
                                yearExpanded = false
                            })
                        }
                    }
                }
            }

            Divider(color = SlateGrey, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // Tampilkan chart atau pesan kosong berdasarkan kondisi
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                when {
                    selectedWalletId == null -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Pilih dompet untuk melihat rekapitulasi pengeluaran.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    expenseStats.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Belum ada pengeluaran di dompet ini pada periode tersebut.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        FinanceStatChart(expenseStats = expenseStats)
                    }
                }
            }
        }
    }
}
