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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prata.finance.data.local.entity.Category
import com.prata.finance.data.local.entity.TransactionType
import com.prata.finance.data.local.entity.Wallet
import com.prata.finance.ui.components.DarkTextField
import com.prata.finance.ui.components.ThemedDropdown
import com.prata.finance.ui.components.ThemedDropdownItem
import com.prata.finance.ui.dashboard.AppViewModelProvider
import com.prata.finance.ui.theme.MetallicGold
import com.prata.finance.ui.theme.NavyDark
import com.prata.finance.ui.theme.OffWhite
import com.prata.finance.ui.theme.SlateGrey
import com.prata.finance.ui.theme.VibrantPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageCategoriesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    // Mengambil data kategori dan dompet dari database
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    // State untuk menyimpan dompet dan tab yang dipilih
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
    val currentType = tabs[selectedTabIndex]

    // Filter kategori berdasarkan tab aktif (Pengeluaran / Pemasukan)
    val filteredCategories = categories.filter { it.type == currentType }

    // State untuk dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Category?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Category?>(null) }
    var categoryNameInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = NavyDark,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Kelola Kategori", color = MetallicGold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = MetallicGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedWallet != null) {
                        categoryNameInput = ""
                        showAddDialog = true
                    }
                },
                containerColor = if (selectedWallet == null) Color.Gray else VibrantPurple,
                contentColor = OffWhite
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Tambah Kategori")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Dropdown pilih dompet dengan tema gelap premium
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)) {
                ThemedDropdown(
                    label = "Dompet",
                    value = selectedWallet?.name ?: "Pilih Dompet",
                    expanded = walletExpanded,
                    onExpandedChange = { walletExpanded = it }
                ) {
                    wallets.forEach { wallet ->
                        ThemedDropdownItem(text = wallet.name, onClick = {
                            selectedWallet = wallet
                            viewModel.selectWallet(wallet.id)
                            walletExpanded = false
                        })
                    }
                }
            }

            // Tab untuk beralih antara Pengeluaran dan Pemasukan
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = NavyDark,
                contentColor = MetallicGold,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MetallicGold
                    )
                }
            ) {
                tabs.forEachIndexed { index, type ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = if (type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran",
                                color = if (selectedTabIndex == index) MetallicGold else Color.Gray
                            )
                        }
                    )
                }
            }

            if (filteredCategories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada kategori.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    items(filteredCategories) { category ->
                        CategoryListItem(
                            category = category,
                            onEditClick = {
                                categoryNameInput = category.name
                                showEditDialog = category
                            },
                            onDeleteClick = { showDeleteDialog = category }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            CategoryInputDialog(
                title = "Tambah Kategori Baru",
                initialName = categoryNameInput,
                initialType = currentType,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, type ->
                    viewModel.addCategory(name, type)
                    showAddDialog = false
                }
            )
        }

        showEditDialog?.let { categoryToEdit ->
            CategoryInputDialog(
                title = "Ubah Kategori",
                initialName = categoryNameInput,
                initialType = categoryToEdit.type,
                onDismiss = { showEditDialog = null },
                onConfirm = { name, type ->
                    viewModel.updateCategory(categoryToEdit, name, type)
                    showEditDialog = null
                }
            )
        }

        showDeleteDialog?.let { categoryToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Hapus Kategori", color = MetallicGold) },
                text = {
                    Text(
                        "Peringatan: Menghapus kategori '${categoryToDelete.name}' akan otomatis " +
                        "menghapus SEMUA transaksi terkait secara permanen!",
                        color = Color.White
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteCategory(categoryToDelete)
                        showDeleteDialog = null
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

@Composable
fun CategoryListItem(
    category: Category,
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
                text = category.name,
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

@Composable
fun CategoryInputDialog(
    title: String,
    initialName: String,
    initialType: TransactionType,
    onDismiss: () -> Unit,
    onConfirm: (String, TransactionType) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    var selectedType by remember { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MetallicGold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = { selectedType = TransactionType.INCOME },
                            colors = RadioButtonDefaults.colors(selectedColor = MetallicGold, unselectedColor = Color.Gray)
                        )
                        Text("Pemasukan", color = OffWhite)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = { selectedType = TransactionType.EXPENSE },
                            colors = RadioButtonDefaults.colors(selectedColor = MetallicGold, unselectedColor = Color.Gray)
                        )
                        Text("Pengeluaran", color = OffWhite)
                    }
                }
                DarkTextField(value = text, onValueChange = { text = it }, label = "Nama Kategori")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text, selectedType) }) { Text("Simpan", color = MetallicGold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = Color.Gray) }
        },
        containerColor = SlateGrey
    )
}
