package com.example.depositocalculateapp

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        supportActionBar?.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    this,
                    R.color.blue
                )
            )
        )
        val textColor = ContextCompat.getColor(this, R.color.white)
        val title = SpannableString("Deposito Calculate")
        title.setSpan(
            ForegroundColorSpan(textColor),
            0,
            title.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        supportActionBar?.title = title

        val editText = findViewById<EditText>(R.id.etNominal)
        val spinnerTenor: Spinner = findViewById(R.id.spinnerTenor)
        val etSukuBunga: EditText = findViewById(R.id.etSukuBunga)
        val etPajak: EditText = findViewById(R.id.etPajak)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val etNominal = findViewById<EditText>(R.id.etNominal)

        // Mapping tenor ke suku bunga
        val sukuBungaMap = mapOf(
            "1 Bulan" to "3,5%",
            "3 Bulan" to "4%",
            "6 Bulan" to "5%",
            "12 Bulan" to "5,75%"
        )

        // Inisialisasi Spinner di sini agar tidak terulang di dalam TextWatcher
        val tenorOptions = listOf("Pilih Tenor", "1 Bulan", "3 Bulan", "6 Bulan", "12 Bulan")
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tenorOptions)
        spinnerTenor.adapter = adapter

        // Event ketika item di Spinner dipilih
        spinnerTenor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedTenor = parent.getItemAtPosition(position).toString()
                etSukuBunga.setText(sukuBungaMap[selectedTenor] ?: "")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        editText.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    editText.removeTextChangedListener(this)

                    val cleanString = s.toString().replace(".", "").replace(",", "")
                    val parsed = if (cleanString.isNotEmpty()) cleanString.toLong() else 0L
                    val formatter: NumberFormat = DecimalFormat("#,###")
                    val formatted = formatter.format(parsed).replace(",", ".")

                    current = formatted
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)

                    // **LOGIKA PAJAK**: Jika lebih dari 7.500.000, pajak 20% muncul, jika tidak kosong
                    if (parsed >= 7_500_000) {
                        etPajak.setText("20%")
                    } else {
                        etPajak.setText("") // Kosongkan jika kurang dari atau sama dengan 7.500.000
                    }

                    editText.addTextChangedListener(this)
                }
            }
        })

        val btnHitung = findViewById<Button>(R.id.btnHitung)
        val etBungaSebelumPajak = findViewById<EditText>(R.id.etBungaSebelumPajak)
        val etBungaSetelahPajak = findViewById<EditText>(R.id.etBungaSetelahPajak)
        val etNominalSetelahTenor = findViewById<EditText>(R.id.etNominalSetelahTenor)

        btnHitung.setOnClickListener {
            val nominalText = editText.text.toString().replace(".", "").replace(",", "")
            val sukuBungaText = etSukuBunga.text.toString().replace("%", "").replace(",", ".")
            val selectedTenor = spinnerTenor.selectedItem.toString()

            if (nominalText.isNotEmpty() && sukuBungaText.isNotEmpty() && selectedTenor != "Pilih Tenor") {
                val nominal = nominalText.toDoubleOrNull() ?: 0.0
                val sukuBunga = sukuBungaText.toDoubleOrNull() ?: 0.0

                if (nominal > 0 && sukuBunga > 0) {
                    val symbols = DecimalFormatSymbols(Locale("id", "ID"))
                    symbols.groupingSeparator = '.'
                    symbols.decimalSeparator = ','
                    val formatter = DecimalFormat("#,###.##", symbols)

                    // **Konversi tenor ke jumlah bulan**
                    val jumlahBulan = when (selectedTenor) {
                        "1 Bulan" -> 1
                        "3 Bulan" -> 3
                        "6 Bulan" -> 6
                        "12 Bulan" -> 12
                        else -> 1 // Default, jika ada error
                    }

                    // **Hitung Bunga Sebelum Pajak**
                    val bungaSebelumPajak = (nominal * sukuBunga) / 100
                    etBungaSebelumPajak.setText(formatter.format(bungaSebelumPajak))

                    var hasil = 0.0  // Hasil setelah pajak jika dikenakan
                    var hasilTenor = 0.0  // Hasil tenor berdasarkan bulan

                    // **Hitung bunga setelah pajak jika nominal > 7.500.000**
                    if (nominal >= 7_500_000) {
                        val a =
                            (nominal * sukuBunga * 30) / (365 * 100) // Perhitungan bunga sebelum pajak
                        val b = a * 0.20  // Pajak 20%
                        hasil = a - b
                        etBungaSetelahPajak.setText(formatter.format(hasil))
                        etBungaSetelahPajak.visibility = View.VISIBLE
                    } else {
                        // **Jika nominal <= 7.500.000, pakai perhitungan tanpa pajak**
                        hasil = (nominal * sukuBunga * 30) / (365 * 100)
                        etBungaSetelahPajak.setText(formatter.format(hasil))
                        etBungaSetelahPajak.visibility = View.VISIBLE
                    }

                    // **Hitung total bunga berdasarkan jumlah bulan tenor**
                    hasilTenor = hasil * jumlahBulan

                    // **Hitung total nominal setelah tenor (Deposito + Total Bunga Tenor)**
                    val totalNominal = nominal + hasilTenor
                    etNominalSetelahTenor.setText(formatter.format(totalNominal))
                }
            }
        }

        // Tombol reset untuk mengosongkan semua input
        btnReset.setOnClickListener {
            etNominal.text.clear()
            etSukuBunga.text.clear()
            etPajak.text.clear()
            etBungaSebelumPajak.text.clear()
            etBungaSetelahPajak.text.clear()
            etNominalSetelahTenor.text.clear()

            // Set Spinner ke posisi pertama (default)
            spinnerTenor.setSelection(0)
        }
    }
}