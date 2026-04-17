package com.prata.finance.ui.ai

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAdvisorScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiAdvisorViewModel = viewModel(
        // Hoist ke Activity scope agar riwayat chat tidak hilang saat navigasi
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = AppViewModelProvider.Factory
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll ke bawah setiap kali ada pesan baru
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
                        Text(
                            text = "Asisten Keuangan AI",
                            color = MetallicGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Ditenagai OpenRouter · Konteks keuangan real-time",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
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
            // Area input pesan di bagian bawah
            Surface(
                color = SlateGrey,
                tonalElevation = 8.dp
            ) {
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
                        placeholder = { Text("Tanya soal keuangan lu...", color = Color.Gray, fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetallicGold,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite,
                            cursorColor = MetallicGold,
                            focusedContainerColor = NavyDark,
                            unfocusedContainerColor = NavyDark
                        ),
                        maxLines = 3,
                        singleLine = false
                    )
                    // Tombol kirim dengan background gradient gold
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(MetallicGold, Color(0xFFB8860B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !uiState.isLoading) {
                                    viewModel.sendChat(inputText.trim())
                                    inputText = ""
                                }
                            },
                            enabled = !uiState.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Send,
                                contentDescription = "Kirim",
                                tint = NavyDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Area chat utama
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pesan sambutan jika belum ada chat
                if (uiState.messages.isEmpty()) {
                    item {
                        WelcomeCard(onSuggestionClick = { suggestion ->
                            viewModel.sendChat(suggestion)
                        })
                    }
                }

                items(uiState.messages) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
                    ) {
                        ChatBubble(message = message)
                    }
                }

                // Indikator loading saat AI sedang merespons
                if (uiState.isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Tampilkan error jika ada
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Kartu sambutan yang muncul sebelum ada pesan
@Composable
private fun WelcomeCard(onSuggestionClick: (String) -> Unit) {
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
            Text(text = "✨", fontSize = 40.sp)
            Text(
                text = "Halo! Gue AI Advisor keuangan lu.",
                color = MetallicGold,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tanya apa aja soal keuangan lu — gue udah tau data pemasukan, pengeluaran, dan kategori lu bulan ini. Langsung gas!",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SuggestionChip(
                    text = "Gimana kondisi keuangan gue bulan ini?",
                    onClick = onSuggestionClick
                )
                SuggestionChip(
                    text = "Di mana pengeluaran terbesar gue?",
                    onClick = onSuggestionClick
                )
                SuggestionChip(
                    text = "Kasih tips hemat buat gue dong!",
                    onClick = onSuggestionClick
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: (String) -> Unit) {
    Surface(
        onClick = { onClick(text) },
        shape = RoundedCornerShape(8.dp),
        color = NavyDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "💬 $text",
            color = MetallicGold.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

// Komponen bubble chat — user di kanan, AI di kiri
@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isFromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Avatar AI
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MetallicGold),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✦", fontSize = 14.sp, color = NavyDark)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUser) 20.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    )
                )
                .background(if (isUser) VibrantPurple else SlateGrey)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Gunakan MarkdownText agar bold/italic/code/list dirender dengan benar
            MarkdownText(
                content = message.content,
                textColor = OffWhite,
                fontSize = 14.sp
            )
        }
        if (isUser) Spacer(modifier = Modifier.width(8.dp))
    }
}

// Indikator "AI sedang mengetik..."
@Composable
private fun TypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MetallicGold),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✦", fontSize = 14.sp, color = NavyDark)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(SlateGrey)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Lagi mikir...",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
