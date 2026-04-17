package com.prata.finance.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property untuk membuat satu instance DataStore per Context (singleton bawaan)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_prefs")

/**
 * Menyimpan dan mengambil preferensi budget keuangan pengguna menggunakan Preferences DataStore.
 *
 * Dibuat sebagai class biasa (bukan object) agar bisa diinjeksikan melalui FinanceApplication
 * mengikuti pola yang sama dengan FinanceRepository di proyek ini.
 */
class BudgetPreferences(private val context: Context) {

    companion object {
        // Key untuk menyimpan nilai budget harian di DataStore
        private val DAILY_BUDGET_KEY = doublePreferencesKey("daily_budget")
    }

    /**
     * Flow yang memancarkan nilai budget harian secara reaktif.
     * Nilai default adalah 0.0 (belum diset).
     * UI dan ViewModel dapat mengobservasi ini dengan collectAsState().
     */
    val dailyBudget: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[DAILY_BUDGET_KEY] ?: 0.0
        }

    /**
     * Menyimpan budget harian ke DataStore secara asinkron.
     * Harus dipanggil dari coroutine (suspend function).
     */
    suspend fun saveDailyBudget(amount: Double) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_BUDGET_KEY] = amount
        }
    }

    /**
     * Menghapus budget harian (reset ke nilai default 0.0).
     */
    suspend fun clearDailyBudget() {
        context.dataStore.edit { preferences ->
            preferences.remove(DAILY_BUDGET_KEY)
        }
    }
}
