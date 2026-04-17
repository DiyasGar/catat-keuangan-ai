package com.prata.finance.ui.manage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prata.finance.data.local.entity.Wallet
import com.prata.finance.ui.components.DarkTextField
import com.prata.finance.ui.dashboard.AppViewModelProvider
import com.prata.finance.ui.theme.MetallicGold
import com.prata.finance.ui.theme.NavyDark
import com.prata.finance.ui.theme.OffWhite
import com.prata.finance.ui.theme.SlateGrey
import com.prata.finance.ui.theme.VibrantPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageWalletsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageWalletsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    // Mengambil data dompet dari database secara reaktif
    val wallets by viewModel.wallets.collectAsState()

    // State untuk menyimpan kondisi dialog yang aktif
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Wallet?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Wallet?>(null) }

    var walletNameInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                title = { Text("Kelola Dompet", color = MetallicGold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = MetallicGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark)
            )
        },
        floatingActionButton = {
            // Tombol untuk menambah dompet baru
            FloatingActionButton(
                onClick = {
                    walletNameInput = ""
                    showAddDialog = true
                },
                containerColor = VibrantPurple,
                contentColor = OffWhite
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Tambah Dompet")
            }
        }
    ) { padding ->
        if (wallets.isEmpty()) {
            // Tampilkan pesan jika belum ada dompet
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada dompet. Tambahkan sekarang!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                items(wallets) { wallet ->
                    WalletListItem(
                        wallet = wallet,
                        onEditClick = {
                            walletNameInput = wallet.name
                            showEditDialog = wallet
                        },
                        onDeleteClick = { showDeleteDialog = wallet }
                    )
                }
            }
        }

        // Dialog tambah dompet baru
        if (showAddDialog) {
            WalletInputDialog(
                title = "Tambah Dompet Baru",
                initialName = walletNameInput,
                onDismiss = { showAddDialog = false },
                onConfirm = { name ->
                    viewModel.addWallet(name)
                    showAddDialog = false
                }
            )
        }

        // Dialog edit nama dompet yang dipilih
        showEditDialog?.let { walletToEdit ->
            WalletInputDialog(
                title = "Ubah Nama Dompet",
                initialName = walletNameInput,
                onDismiss = { showEditDialog = null },
                onConfirm = { name ->
                    viewModel.updateWallet(walletToEdit, name)
                    showEditDialog = null
                }
            )
        }

        // Dialog konfirmasi penghapusan dompet
        showDeleteDialog?.let { walletToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Hapus Dompet", color = MetallicGold) },
                text = {
                    Text(
                        "Peringatan: Menghapus dompet '${walletToDelete.name}' akan otomatis " +
                        "menghapus SEMUA transaksi yang terkait secara permanen!",
                        color = Color.White
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteWallet(walletToDelete)
                        showDeleteDialog = null
                        if (wallets.size == 1) onNavigateBack()
                    }) {
                        Text("Hapus Permanen", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Batal", color = MetallicGold)
                    }
                },
                containerColor = SlateGrey
            )
        }
    }
}

// Item daftar dompet dengan tombol edit dan hapus
@Composable
fun WalletListItem(
    wallet: Wallet,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateGrey),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = wallet.name,
                color = OffWhite,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Ubah", tint = MetallicGold)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// Dialog input untuk nama dompet (digunakan saat tambah dan edit)
@Composable
fun WalletInputDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MetallicGold) },
        text = {
            DarkTextField(
                value = text,
                onValueChange = { text = it },
                label = "Nama Dompet"
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Simpan", color = MetallicGold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Gray)
            }
        },
        containerColor = SlateGrey
    )
}
