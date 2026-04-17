package com.prata.finance.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.prata.finance.ui.add.AddTransactionScreen
import com.prata.finance.ui.ai.AiAdvisorScreen
import com.prata.finance.ui.budget.BudgetNegotiatorScreen
import com.prata.finance.ui.dashboard.DashboardScreen
import com.prata.finance.ui.edit.EditTransactionScreen
import com.prata.finance.ui.history.TransactionHistoryScreen
import com.prata.finance.ui.manage.ManageCategoriesScreen
import com.prata.finance.ui.manage.ManageWalletsScreen
import com.prata.finance.ui.report.ReportScreen
import com.prata.finance.ui.settings.SettingsScreen
import com.prata.finance.ui.theme.MetallicGold
import com.prata.finance.ui.theme.NavyDark
import com.prata.finance.ui.theme.OffWhite
import com.prata.finance.ui.theme.SlateGrey
import com.prata.finance.ui.theme.VibrantPurple

// ── Model untuk setiap tab ─────────────────────────────────────────────────────
private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("dashboard",         "Beranda",  Icons.Rounded.Home),
    BottomNavItem("reports",           "Rekap",    Icons.Rounded.BarChart),
    BottomNavItem("manage_categories", "Kategori", Icons.Rounded.Category),
    BottomNavItem("ai_hub",            "Asisten",  Icons.Rounded.AutoAwesome)
)

// Rute yang menampilkan bottom nav + FAB
private val mainRoutes = setOf(
    "dashboard", "reports", "manage_categories", "ai_hub"
)

@Composable
fun AppNavHost(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in mainRoutes
    // FAB hanya muncul di Beranda dan Rekap — Kategori dan Asisten punya UI input sendiri
    val showFab = currentRoute == "dashboard" || currentRoute == "reports"

    Scaffold(
        containerColor = NavyDark,
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_transaction") },
                    containerColor = VibrantPurple,
                    contentColor = OffWhite,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Tambah Transaksi")
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                ArthaBottomNavBar(
                    currentRoute = currentRoute,
                    navController = navController
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition  = { fadeOut(tween(180)) }
        ) {
            // ── Tab 1: Beranda ──────────────────────────────────────────────
            composable("dashboard") {
                DashboardScreen(
                    onNavigateToAddTransaction = { navController.navigate("add_transaction") },
                    onNavigateToEditTransaction = { txId ->
                        navController.navigate("edit_transaction/$txId")
                    },
                    onNavigateToManageWallets = { navController.navigate("manage_wallets") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToTransactionHistory = { navController.navigate("transaction_history") }
                )
            }

            // ── Tab 2: Rekap ────────────────────────────────────────────────
            composable("reports") {
                ReportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Tab 3: Kategori ─────────────────────────────────────────────
            composable("manage_categories") {
                ManageCategoriesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Tab 4: Asisten — hub untuk memilih Advisor atau Negosiator ──
            composable("ai_hub") {
                AiHubScreen(
                    onNavigateToAdvisor    = { navController.navigate("ai_advisor") },
                    onNavigateToNegotiator = { navController.navigate("budget_negotiator") }
                )
            }

            // ── Detail screens (tanpa bottom nav) ───────────────────────────
            composable("add_transaction") {
                AddTransactionScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = "edit_transaction/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) {
                EditTransactionScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("manage_wallets") {
                ManageWalletsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToManageWallets    = { navController.navigate("manage_wallets") }
                )
            }
            composable("transaction_history") {
                TransactionHistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditTransaction = { txId ->
                        navController.navigate("edit_transaction/$txId")
                    }
                )
            }
            composable("ai_advisor") {
                AiAdvisorScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("budget_negotiator") {
                BudgetNegotiatorScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

// ── Bottom Navigation Bar ──────────────────────────────────────────────────────
@Composable
private fun ArthaBottomNavBar(
    currentRoute: String?,
    navController: NavController
) {
    NavigationBar(
        containerColor = SlateGrey,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop ke start destination agar backstack tidak numpuk
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MetallicGold,
                    selectedTextColor   = MetallicGold,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor      = MetallicGold.copy(alpha = 0.15f)
                )
            )
        }
    }
}

// ── AI Hub: kartu pilihan menu AI ─────────────────────────────────────────────
@Composable
private fun AiHubScreen(
    onNavigateToAdvisor: () -> Unit,
    onNavigateToNegotiator: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Asisten AI",
            color = MetallicGold,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Pilih fitur AI yang ingin kamu gunakan.",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Kartu: Asisten Keuangan AI
        AiFeatureCard(
            icon = Icons.Rounded.AutoAwesome,
            title = "Asisten Keuangan AI",
            description = "Tanya apa aja soal keuangan lu. Analisis pengeluaran, tips hemat, dan roasting catatan transaksi kamu!",
            onClick = onNavigateToAdvisor
        )

        // Kartu: Negosiator Budget Pintar
        AiFeatureCard(
            icon = Icons.Rounded.Balance,
            title = "Negosiator Budget Pintar",
            description = "Sepakati jatah harian bareng AI. Masukkan tawaran, deal-dealan, dan budget langsung tersimpan ke app!",
            onClick = onNavigateToNegotiator
        )
    }
}

@Composable
private fun AiFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateGrey),
        border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon badge
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MetallicGold.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MetallicGold,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = OffWhite,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
