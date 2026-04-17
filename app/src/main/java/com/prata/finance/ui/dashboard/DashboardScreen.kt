package com.prata.finance.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.prata.finance.ui.components.GoldBorderCard
import com.prata.finance.ui.theme.MetallicGold
import com.prata.finance.ui.theme.NavyDark
import com.prata.finance.ui.theme.OffWhite
import com.prata.finance.ui.theme.SlateGrey
import com.prata.finance.ui.theme.VibrantPurple
import java.text.NumberFormat
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToManageWallets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTransactionHistory: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    // Konten ditampilkan langsung tanpa Scaffold tambahan
    // karena Scaffold utama (dengan FAB + BottomNav) ada di AppNavHost
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MetallicGold)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Header Beranda
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Beranda",
                            color = MetallicGold,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Pengaturan",
                                tint = MetallicGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Top Section: Summary Cards
                    SummarySection(
                        totalSaldo = uiState.totalSaldo,
                        income = uiState.currentMonthIncome,
                        expense = uiState.currentMonthExpense
                    )



                    Spacer(modifier = Modifier.height(24.dp))

                    // Menampilkan Daftar Dompet dengan Card Terbatas
                    Text(
                        text = "Dompet Anda",
                        style = MaterialTheme.typography.titleMedium,
                        color = OffWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Dynamically scaled layout without relying on nested scroll bounds
                    val chunkedWallets = uiState.walletsList.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunkedWallets.forEach { rowWallets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowWallets.forEach { walletUi ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        WalletCard(
                                            walletUi = walletUi
                                        )
                                    }
                                }
                                // Fill missing trailing column spaces neatly natively
                                if (rowWallets.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Menampilkan Riwayat Transaksi Ringkas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transaksi Terakhir",
                            style = MaterialTheme.typography.titleMedium,
                            color = OffWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lihat Semua",
                            style = MaterialTheme.typography.labelLarge,
                            color = MetallicGold,
                            modifier = Modifier.clickable { onNavigateToTransactionHistory() }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Generate List Item Secara Dinamis untuk Kinerja yang Lebih Baik
                items(uiState.recentTransactions) { tx ->
                    TransactionItem(
                        txUi = tx,
                        onClick = { onNavigateToEditTransaction(tx.transaction.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
        }
    }
}

@Composable
fun SummarySection(totalSaldo: Double, income: Double, expense: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateGrey),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Total Saldo", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = formatRupiah(totalSaldo),
                color = MetallicGold,
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Pemasukan (Bulan Ini)", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    Text(text = formatRupiah(income), color = Color.Green, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Pengeluaran (Bulan Ini)", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    Text(text = formatRupiah(expense), color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WalletCard(walletUi: WalletUiModel) {
    GoldBorderCard {
        Box(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Column {
                Text(
                    text = walletUi.wallet.name, 
                    color = Color.LightGray, 
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatRupiah(walletUi.balance), 
                    color = OffWhite, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TransactionItem(txUi: TransactionUiModel, onClick: () -> Unit) {
    val tx = txUi.transaction
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val dateStr = Instant.ofEpochMilli(tx.date).atZone(ZoneId.systemDefault()).format(formatter)
    
    val amountColor = if (txUi.isIncome) Color.Green else Color.Red
    val sign = if (txUi.isIncome) "+" else "-"

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateGrey),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tx.description, color = OffWhite, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = txUi.categoryName, color = MetallicGold, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dateStr, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Text(
                text = "$sign${formatRupiah(tx.amount)}",
                color = amountColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ")
}
