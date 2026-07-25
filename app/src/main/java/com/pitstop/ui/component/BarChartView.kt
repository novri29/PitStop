package com.pitstop.ui.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.pitstop.pitstop.R

data class BarChartEntry(val label: String, val value: Float)

/**
 * Grafik batang sederhana (Canvas custom view) untuk menampilkan tren omzet/penjualan.
 * Sengaja dibuat manual (bukan library eksternal seperti MPAndroidChart) supaya project
 * tidak perlu dependency tambahan dan tetap ringan.
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var entries: List<BarChartEntry> = emptyList()
    private var formatRupiah: Boolean = false

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.grey)
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.black)
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.grey)
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    /** @param formatRupiah true untuk grafik omzet (Rp), false untuk grafik jumlah transaksi/produk */
    fun setData(data: List<BarChartEntry>, formatRupiah: Boolean = false) {
        this.entries = data
        this.formatRupiah = formatRupiah
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (entries.isEmpty() || entries.all { it.value <= 0f }) {
            canvas.drawText("Belum ada data penjualan", width / 2f, height / 2f, emptyPaint)
            return
        }

        val maxValue = entries.maxOf { it.value }.coerceAtLeast(1f)
        val paddingBottom = 60f
        val paddingTop = 44f
        val chartHeight = height - paddingBottom - paddingTop
        val slotWidth = width.toFloat() / entries.size
        val barWidth = (slotWidth * 0.5f).coerceAtMost(60f)

        entries.forEachIndexed { index, entry ->
            val barHeight = (entry.value / maxValue) * chartHeight
            val left = index * slotWidth + (slotWidth - barWidth) / 2f
            val right = left + barWidth
            val top = paddingTop + (chartHeight - barHeight)
            val bottom = paddingTop + chartHeight

            canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, barPaint)

            val teksNilai = if (formatRupiah) formatSingkatRupiah(entry.value) else entry.value.toInt().toString()
            canvas.drawText(teksNilai, left + barWidth / 2f, (top - 10f).coerceAtLeast(20f), valuePaint)
            canvas.drawText(entry.label, left + barWidth / 2f, height.toFloat() - 14f, labelPaint)
        }
    }

    private fun formatSingkatRupiah(value: Float): String = when {
        value >= 1_000_000 -> "%.1fjt".format(value / 1_000_000)
        value >= 1_000 -> "%.0frb".format(value / 1_000)
        else -> value.toInt().toString()
    }
}