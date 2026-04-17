package com.prata.finance.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prata.finance.ui.dashboard.AppViewModelProvider
import com.prata.finance.ui.theme.MetallicGold
import com.prata.finance.ui.theme.NavyDark
import com.prata.finance.ui.theme.OffWhite
import com.prata.finance.ui.theme.SlateGrey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToManageWallets: () -> Unit,
    resetViewModel: ResetViewModel = viewModel(factory = AppViewModelProvider.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    // State untuk dialog konfirmasi penghapusan data
    var showResetDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Launcher SAF untuk membuat dan menyimpan file CSV ke penyimpanan pengguna
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    // Ambil data CSV dari ViewModel, lalu tulis ke file yang dipilih
                    val csvData = settingsViewModel.getExportCsvData()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(csvData.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Data berhasil diexport!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal mengexport data.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", color = MetallicGold) },
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
                .padding(top = 16.dp)
        ) {
            // Menu: Kelola Dompet
            SettingsMenuItem(
                icon = Icons.Rounded.Wallet,
                title = "Kelola Dompet",
                onClick = onNavigateToManageWallets
            )
            Divider(color = SlateGrey, modifier = Modifier.padding(horizontal = 16.dp))

            // Menu: Export ke CSV menggunakan Storage Access Framework
            SettingsMenuItem(
                icon = Icons.Rounded.Share,
                title = "Ekspor Data (CSV)",
                onClick = { exportLauncher.launch("Keuangan_Backup.csv") }
            )
            Divider(color = SlateGrey, modifier = Modifier.padding(horizontal = 16.dp))

            // Menu: Reset semua data transaksi
            SettingsMenuItem(
                icon = Icons.Rounded.Delete,
                title = "Reset Data & Mulai Baru",
                onClick = { showResetDialog = true }
            )
        }
    }

    // Dialog konfirmasi sebelum menghapus semua transaksi
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Zona Berbahaya", color = Color.Red) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus SEMUA riwayat transaksi? " +
                    "Data ini tidak dapat dikembalikan!\n\n" +
                    "Catatan: Saldo bulan ini di Beranda direset otomatis setiap bulan baru.",
                    color = OffWhite
                )
            },
            containerColor = NavyDark,
            confirmButton = {
                TextButton(onClick = {
                    resetViewModel.deleteAllData()
                    showResetDialog = false
                }) {
                    Text("Hapus Permanen", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal", color = MetallicGold)
                }
            }
        )
    }
}

// Komponen baris menu pengaturan yang dapat diklik
@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: Color = MetallicGold
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, color = OffWhite, style = MaterialTheme.typography.titleMedium)
        }
        Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.Gray)
    }
}
