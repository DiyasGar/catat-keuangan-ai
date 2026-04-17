package com.prata.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prata.finance.ui.components.ExpenseCategoryStat
import com.prata.finance.ui.theme.MetallicGold
import com.prata.finance.ui.theme.OffWhite
import com.prata.finance.ui.theme.SlateGrey
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FinanceStatChart(
    expenseStats: List<ExpenseCategoryStat>,
    modifier: Modifier = Modifier
) {
    if (expenseStats.isEmpty()) {
        Text(
            text = "Belum ada pengeluaran bulan ini.",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(vertical = 16.dp)
        )
        return
    }

    // Limit to Top 4 Expenses for clean UX
    val displayStats = expenseStats.take(4)
    val maxExpense = displayStats.maxOfOrNull { it.totalAmount } ?: 1.0
    val totalExpense = expenseStats.sumOf { it.totalAmount } 

    GoldBorderCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pengeluaran by Category",
                color = OffWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            displayStats.forEach { stat ->
                // Ensure fraction is at least bounded by 0 avoiding negative scaling errors
                val fraction = if (maxExpense > 0) (stat.totalAmount / maxExpense).toFloat().coerceIn(0f, 1f) else 0f
                val percentage = if (totalExpense > 0) ((stat.totalAmount / totalExpense) * 100).toInt() else 0
                
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stat.categoryName,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = "$percentage% (${formatRupiahCompact(stat.totalAmount)})",
                            color = OffWhite,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // The Bar Track (Empty Path Background)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.DarkGray.copy(alpha = 0.4f)) // distinct from SlateGrey surface
                    ) {
                        // The solid fractional Metric slice
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = fraction)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MetallicGold)
                        )
                    }
                }
            }
        }
    }
}

private fun formatRupiahCompact(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace(",00", "").replace("Rp", "Rp ")
}
