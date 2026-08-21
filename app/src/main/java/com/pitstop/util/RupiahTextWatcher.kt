package com.pitstop.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class RupiahTextWatcher(
    private val editText: EditText,
    private val onValueChanged: (Double) -> Unit = {}
) : TextWatcher {

    private var sedangMemformat = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (sedangMemformat || s == null) return
        sedangMemformat = true

        val angkaBersih = s.toString().replace(".", "").replace(",", "")
        if (angkaBersih.isEmpty()) {
            sedangMemformat = false
            onValueChanged(0.0)
            return
        }

        val nilai = angkaBersih.toLongOrNull()
        if (nilai == null) {
            sedangMemformat = false
            return
        }

        val hasilFormat = FORMATTER.format(nilai)
        editText.setText(hasilFormat)
        editText.setSelection(hasilFormat.length)

        sedangMemformat = false
        onValueChanged(nilai.toDouble())
    }

    companion object {
        private val FORMATTER = DecimalFormat("#,###", DecimalFormatSymbols(Locale("in", "ID")))
        fun parse(teksTerformat: String): Double =
            teksTerformat.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }
}