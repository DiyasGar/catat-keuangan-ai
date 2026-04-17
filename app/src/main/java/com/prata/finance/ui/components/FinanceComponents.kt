package com.prata.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prata.finance.ui.theme.MetallicGold
import com.prata.finance.ui.theme.OffWhite
import com.prata.finance.ui.theme.VibrantPurple

data class ExpenseCategoryStat(
    val categoryName: String,
    val totalAmount: Double
)

@Composable
fun ElegantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VibrantPurple,
            contentColor = OffWhite
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
fun GoldBorderCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MetallicGold),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

@Composable
fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isDropdown: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors? = null
) {
    val appliedColors = colors ?: OutlinedTextFieldDefaults.colors(
        focusedBorderColor = if (isDropdown) Color.Transparent else MetallicGold,
        unfocusedBorderColor = if (isDropdown) Color.Transparent else Color.Gray,
        focusedLabelColor = MetallicGold,
        unfocusedLabelColor = Color.LightGray,
        cursorColor = MetallicGold,
        focusedTextColor = OffWhite,
        unfocusedTextColor = OffWhite,
        disabledTextColor = OffWhite,
        disabledBorderColor = if (isDropdown) Color.Transparent else Color.Gray,
        disabledLabelColor = Color.LightGray,
        focusedContainerColor = if (isDropdown) Color(0xFF1F2937) else Color.Transparent,
        unfocusedContainerColor = if (isDropdown) Color(0xFF1F2937) else Color.Transparent
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = keyboardOptions,
        readOnly = readOnly,
        enabled = enabled,
        trailingIcon = trailingIcon,
        colors = appliedColors,
        singleLine = true
    )
}

// Warna latar popup dropdown — sedikit lebih terang dari NavyDark agar popup terasa melayang
private val DropdownPopupColor = Color(0xFF1A2535)

/**
 * Dropdown bertema Navy/Gold yang konsisten di seluruh aplikasi.
 * Trigger: DarkTextField dengan sudut bulat.
 * Popup: Latar gelap premium dengan sudut bulat 16dp.
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ThemedDropdown(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) onExpandedChange(!expanded) },
        modifier = modifier
    ) {
        DarkTextField(
            value = value,
            onValueChange = {},
            label = label,
            readOnly = true,
            enabled = enabled,
            isDropdown = true,
            modifier = Modifier.menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .exposedDropdownSize()
                .clip(RoundedCornerShape(16.dp))
                .background(DropdownPopupColor)
        ) {
            content()
        }
    }
}

/**
 * Item baris dalam ThemedDropdown.
 * Teks berwarna putih bersih dengan highlight emas saat ditekan.
 */
@Composable
fun ThemedDropdownItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenuItem(
        text = { Text(text, color = OffWhite) },
        onClick = onClick,
        modifier = modifier,
        colors = MenuDefaults.itemColors(
            textColor = OffWhite,
            leadingIconColor = MetallicGold,
            trailingIconColor = MetallicGold
        )
    )
}
