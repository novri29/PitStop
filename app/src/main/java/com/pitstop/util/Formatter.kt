package com.pitstop.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatter {
    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
        maximumFractionDigits = 0
    }

    fun rupiah(value: Double): String = rupiahFormat.format(value)

    fun tanggalWaktu(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun tanggal(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale("in", "ID"))
        return sdf.format(Date(timestamp))
    }
}
