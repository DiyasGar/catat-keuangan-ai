package com.prata.finance.ui.budget

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prata.finance.ui.components.MarkdownText
import com.prata.finance.ui.dashboard.AppViewModelProvider
import com.prata.finance.ui.theme.MetallicGold
import com.prata.finance.ui.theme.NavyDark
import com.prata.finance.ui.theme.OffWhite
import com.prata.finance.ui.theme.SlateGrey
import com.prata.finance.ui.theme.VibrantPurple
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetNegotiatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetNegotiatorViewModel = viewModel(
        // Hoist ke Activity scope agar riwayat negosiasi tidak hilang saat user navigasi
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = AppViewModelProvider.Factory
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll ke pesan terbaru
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Negosiator Budget Pintar", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Sepakati jatah harian dengan AI", color = Color.Gray, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = MetallicGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark)
            )
        },
        bottomBar = {
            // Input area — disabled setelah deal tercapai
            val dealDone = uiState.dealBudget != null
            Surface(color = SlateGrey, tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        enabled = !dealDone && !uiState.isLoading && !uiState.isInitializing,
                        placeholder = {
                            Text(
                                if (dealDone) "Deal sudah tersimpan ✅" else "Tawar angka lain...",
                                color = Color.Gray, fontSize = 14.sp
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetallicGold,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite,
                            cursorColor = MetallicGold,
                            focusedContainerColor = NavyDark,
                            unfocusedContainerColor = NavyDark,
                            disabledContainerColor = NavyDark.copy(alpha = 0.6f),
                            disabledBorderColor = Color.DarkGray
                        ),
                        maxLines = 3
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (dealDone) Brush.radialGradient(listOf(Color.Gray, Color.DarkGray))
                                else Brush.radialGradient(listOf(MetallicGold, Color(0xFFB8860B)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendOffer(inputText.trim())
                                    inputText = ""
                                }
                            },
                            enabled = !dealDone && !uiState.isLoading && !uiState.isInitializing
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = "Kirim", tint = NavyDark, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Banner sukses deal — muncul di atas daftar chat setelah deal
            AnimatedVisibility(visible = uiState.dealBudget != null) {
                uiState.dealBudget?.let { amount ->
                    DealSuccessBanner(amount = amount)
                }
            }

            // Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Loading awal saat data keuangan sedang difetch
            if (uiState.isInitializing) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = MetallicGold)
                        Text("Menganalisis keuangan lu...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                // Daftar pesan negosiasi
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Empty state: muncul saat belum ada pesan (sebelum chip diklik)
                    if (uiState.messages.isEmpty() && !uiState.isLoading) {
                        item {
                            NegotiatorWelcomeCard(onSuggestionClick = { suggestion ->
                                viewModel.sendOffer(suggestion)
                            })
                        }
                    }

                    items(uiState.messages) { message ->
                        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it / 2 }) {
                            NegotiatorBubble(message = message)
                        }
                    }
                    if (uiState.isLoading) {
                        item { NegotiatorTypingIndicator() }
                    }
                }
            }
        }
    }
}

// Kartu sambutan empty state untuk negosiasi
@Composable
private fun NegotiatorWelcomeCard(onSuggestionClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateGrey),
        border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "🤝", fontSize = 40.sp)
            Text(
                text = "Halo! Gue Negosiator Budget lu.",
                color = MetallicGold,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Pilih salah satu opsi di bawah buat mulai ngatur jatah jajan lu. Kita deal-dealan sampai dapet angka yang pas!",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NegotiatorChip(
                    emoji = "💰",
                    text = "Aturin jatah jajan harian gue dari sisa saldo sekarang dong!",
                    onClick = onSuggestionClick
                )
                NegotiatorChip(
                    emoji = "📅",
                    text = "Cek pengeluaran bulan kemaren dan buatin target budget per kategori buat bulan ini!",
                    onClick = onSuggestionClick
                )
                NegotiatorChip(
                    emoji = "⚖️",
                    text = "Bantu bagi pemasukan bulan ini pake aturan 50/30/20 (Kebutuhan, Keinginan, Tabungan) ya!",
                    onClick = onSuggestionClick
                )
            }
        }
    }
}

@Composable
private fun NegotiatorChip(emoji: String, text: String, onClick: (String) -> Unit) {
    Surface(
        onClick = { onClick(text) },
        shape = RoundedCornerShape(12.dp),
        color = NavyDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Text(
                text = text,
                color = MetallicGold.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
        }
    }
}

// Banner sukses yang muncul setelah deal tercapai
@Composable
private fun DealSuccessBanner(amount: Double) {
    val formatted = "Rp${NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount.toLong())}"
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A1A)),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("🎉 Kontrak Budget Tersimpan!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Jatah harian kamu: $formatted/hari", color = OffWhite, style = MaterialTheme.typography.bodyMedium)
            Text("Budget ini sudah otomatis disimpan ke aplikasi.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// Bubble chat — user (ungu, kanan) vs AI (slate, kiri)
@Composable
private fun NegotiatorBubble(message: NegotiatorMessage) {
    val isUser = message.isFromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(MetallicGold),
                contentAlignment = Alignment.Center
            ) {
                Text("⚖", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(
                        topStart = if (isUser) 20.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    ))
                    .background(
                        when {
                            message.isDealMessage -> Color(0xFF1A3A1A)
                            isUser -> VibrantPurple
                            else -> SlateGrey
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Gunakan MarkdownText agar format AI (bold, list, kode) dirender dengan benar
                MarkdownText(
                    content = message.content,
                    textColor = OffWhite,
                    fontSize = 14.sp
                )
            }
        }
        if (isUser) Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun NegotiatorTypingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MetallicGold), contentAlignment = Alignment.Center) {
            Text("⚖", fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(SlateGrey)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text("Lagi ngitung...", color = Color.Gray, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}
