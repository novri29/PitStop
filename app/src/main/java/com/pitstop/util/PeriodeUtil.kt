package com.pitstop.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class TipePeriode { HARIAN, BULANAN, TAHUNAN }

/**
 * Helper untuk menghitung rentang awal-akhir (dalam milidetik) serta label tampilan
 * untuk filter laporan Harian / Bulanan / Tahunan, berdasarkan sebuah tanggal acuan.
 */
object PeriodeUtil {

    fun rentang(tipe: TipePeriode, acuan: Calendar): Pair<Long, Long> {
        val awal = acuan.clone() as Calendar
        val akhir = acuan.clone() as Calendar

        when (tipe) {
            TipePeriode.HARIAN -> {
                awal.set(Calendar.HOUR_OF_DAY, 0); awal.set(Calendar.MINUTE, 0)
                awal.set(Calendar.SECOND, 0); awal.set(Calendar.MILLISECOND, 0)

                akhir.set(Calendar.HOUR_OF_DAY, 23); akhir.set(Calendar.MINUTE, 59)
                akhir.set(Calendar.SECOND, 59); akhir.set(Calendar.MILLISECOND, 999)
            }
            TipePeriode.BULANAN -> {
                awal.set(Calendar.DAY_OF_MONTH, 1)
                awal.set(Calendar.HOUR_OF_DAY, 0); awal.set(Calendar.MINUTE, 0)
                awal.set(Calendar.SECOND, 0); awal.set(Calendar.MILLISECOND, 0)

                akhir.set(Calendar.DAY_OF_MONTH, akhir.getActualMaximum(Calendar.DAY_OF_MONTH))
                akhir.set(Calendar.HOUR_OF_DAY, 23); akhir.set(Calendar.MINUTE, 59)
                akhir.set(Calendar.SECOND, 59); akhir.set(Calendar.MILLISECOND, 999)
            }
            TipePeriode.TAHUNAN -> {
                awal.set(Calendar.DAY_OF_YEAR, 1)
                awal.set(Calendar.HOUR_OF_DAY, 0); awal.set(Calendar.MINUTE, 0)
                awal.set(Calendar.SECOND, 0); awal.set(Calendar.MILLISECOND, 0)

                akhir.set(Calendar.MONTH, Calendar.DECEMBER)
                akhir.set(Calendar.DAY_OF_MONTH, 31)
                akhir.set(Calendar.HOUR_OF_DAY, 23); akhir.set(Calendar.MINUTE, 59)
                akhir.set(Calendar.SECOND, 59); akhir.set(Calendar.MILLISECOND, 999)
            }
        }
        return awal.timeInMillis to akhir.timeInMillis
    }

    fun label(tipe: TipePeriode, acuan: Calendar): String {
        val locale = Locale("in", "ID")
        return when (tipe) {
            TipePeriode.HARIAN -> SimpleDateFormat("dd MMMM yyyy", locale).format(acuan.time)
            TipePeriode.BULANAN -> SimpleDateFormat("MMMM yyyy", locale).format(acuan.time)
            TipePeriode.TAHUNAN -> SimpleDateFormat("yyyy", locale).format(acuan.time)
        }
    }

    /** Geser tanggal acuan maju/mundur sesuai tipe periode. delta = -1 (mundur) atau 1 (maju). */
    fun geser(tipe: TipePeriode, acuan: Calendar, delta: Int): Calendar {
        val baru = acuan.clone() as Calendar
        when (tipe) {
            TipePeriode.HARIAN -> baru.add(Calendar.DAY_OF_MONTH, delta)
            TipePeriode.BULANAN -> baru.add(Calendar.MONTH, delta)
            TipePeriode.TAHUNAN -> baru.add(Calendar.YEAR, delta)
        }
        return baru
    }
}
