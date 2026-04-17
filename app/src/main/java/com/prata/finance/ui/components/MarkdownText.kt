package com.prata.finance.ui.components

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * Composable yang merender teks Markdown menggunakan library Markwon di dalam sebuah TextView.
 *
 * Mendukung:
 * - **Bold**, *italic*, ~~strikethrough~~
 * - `inline code` dan blok kode
 * - Daftar bullet dan numbered list
 * - Tabel sederhana
 * - Link yang bisa diklik
 * - Formula LaTeX (dikonversi ke blok kode agar tetap terbaca, tanpa rendering penuh)
 *
 * @param content     Teks Markdown yang akan dirender
 * @param textColor   Warna teks (dari tema Compose)
 * @param modifier    Modifier standar Compose
 * @param fontSize    Ukuran font
 */
@Composable
fun MarkdownText(
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp
) {
    val context = LocalContext.current
    val currentColor by rememberUpdatedState(textColor)

    // Instance Markwon di-cache — tidak dibuat ulang setiap recomposition
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .build()
    }

    // Preprocessing dilakukan sekali per perubahan konten
    val processedContent = remember(content) { preprocessMath(content) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // Buat TextView dengan gaya dasar — Markwon akan mengelola teks selanjutnya
            TextView(ctx).apply {
                textSize = fontSize.value
                setTextColor(currentColor.toArgb())
                setLineSpacing(4f, 1.2f)  // sedikit spacing agar lebih mudah dibaca
                setPadding(0, 0, 0, 0)
            }
        },
        update = { textView ->
            // Update warna jika berubah (misalnya bubble AI vs user)
            textView.setTextColor(currentColor.toArgb())
            markwon.setMarkdown(textView, processedContent)
        }
    )
}

/**
 * Mengonversi notasi LaTeX ke format Markdown yang bisa dirender oleh Markwon.
 *
 * Karena Markwon tanpa ekstensi LaTeX tidak bisa merender formula matematika secara visual,
 * strategi ini mengubahnya ke blok kode monospace agar tetap terbaca dan tidak mengganggu tampilan.
 *
 * Pola yang dikenali:
 * - Block math  : \[ ... \]  dan $$ ... $$
 * - Inline math : \( ... \)  dan $ ... $
 */
private fun preprocessMath(text: String): String = text
    // Block math: \[ ... \] → code block
    .replace(Regex("""(?s)\\\[(.*?)\\\]""")) { m ->
        "\n```\n${m.groupValues[1].trim()}\n```\n"
    }
    // Block math: $$ ... $$ → code block
    .replace(Regex("""(?s)\$\$(.*?)\$\$""")) { m ->
        "\n```\n${m.groupValues[1].trim()}\n```\n"
    }
    // Inline math: \( ... \) → inline code
    .replace(Regex("""\\\((.+?)\\\)""")) { m ->
        "`${m.groupValues[1].trim()}`"
    }
    // Inline math: $...$ → inline code (hindari match ganda dengan $$ di atas)
    .replace(Regex("""(?<!\$)\$(?!\$)(.+?)(?<!\$)\$(?!\$)""")) { m ->
        "`${m.groupValues[1].trim()}`"
    }
