package com.example.utils

import android.content.Context

object DeletedReceiptsTracker {
    private const val PREFS_NAME = "deleted_receipts_prefs"
    private const val KEY_DELETED_SET = "deleted_receipt_numbers"

    fun addDeletedReceipt(context: Context, receiptNumber: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_DELETED_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(receiptNumber)
        prefs.edit().putStringSet(KEY_DELETED_SET, currentSet).apply()
    }

    fun isReceiptDeleted(context: Context, receiptNumber: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_DELETED_SET, emptySet()) ?: emptySet()
        return currentSet.contains(receiptNumber)
    }

    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DELETED_SET).apply()
    }
}
