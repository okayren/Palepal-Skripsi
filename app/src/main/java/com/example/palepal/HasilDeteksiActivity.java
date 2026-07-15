package com.example.palepal;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable; // Tambahan untuk mengatur sudut kotak
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RelativeLayout; // Tambahan untuk memanipulasi kotak background
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HasilDeteksiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hasil_deteksi);

        //Tangkap semua ID dari XML
        TextView tvHasil = findViewById(R.id.tv_hasil);
        TextView tvConfidenceValue = findViewById(R.id.tv_confidence_value);
        TextView tvHasilKalimat = findViewById(R.id.tv_hasil_kalimat);
        TextView tvHasilDeteksi = findViewById(R.id.tv_hasil_deteksi);
        ImageView btnBack = findViewById(R.id.btn_back);
        TextView btnDeteksiKembali = findViewById(R.id.btn_deteksi_kembali);

        //Tangkap ID kotak background agar warnanya bisa diubah
        RelativeLayout frame7 = findViewById(R.id.frame_7);

        //Tangkap semua skor murni (0.0 - 1.0) dari Intent
        float confKuku = getIntent().getFloatExtra("skor_kuku", 0.0f);
        float confTelapak = getIntent().getFloatExtra("skor_telapak", 0.0f);
        float confKonjungtiva = getIntent().getFloatExtra("skor_konjungtiva", 0.0f);

        //Terapkan Logika Weighted Decision Fusion
        //Konjungtiva: 60% (indikator primer, spesifisitas klinis tertinggi)
        //Telapak: 20%, Kuku: 20% (indikator sekunder)
        float bobotKonjungtiva = confKonjungtiva * 0.60f;
        float bobotTelapak = confTelapak * 0.20f;
        float bobotKuku = confKuku * 0.20f;

        float skorAkhir = bobotKonjungtiva + bobotTelapak + bobotKuku;
        int persenAkhir = Math.round(skorAkhir * 100);

        //Masukkan data angka ke UI
        tvConfidenceValue.setText(persenAkhir + "%");

        // Rincian skor per area — pakai istilah "skor indikasi" bukan "confidence"
        // supaya lebih mudah dipahami pengguna awam
        String listRincian =
                "• Konjungtiva Mata: " + Math.round(confKonjungtiva * 100) + "%\n" +
                "• Kuku: " + Math.round(confKuku * 100) + "%\n" +
                "• Telapak Tangan: " + Math.round(confTelapak * 100) + "%";
        tvHasilDeteksi.setText(listRincian);

        //Siapkan bentuk kotak baru agar sudutnya tetap tumpul saat diwarnai merah
        GradientDrawable shapeMerah = new GradientDrawable();
        shapeMerah.setShape(GradientDrawable.RECTANGLE);
        shapeMerah.setCornerRadius(24f); // Angka kelengkungan sudut (bisa disesuaikan)
        shapeMerah.setColor(Color.parseColor("#B33A3A")); // Warna background merah

        //Atur Perubahan Visual Berdasarkan Hasil
        if (skorAkhir >= 0.50f) {
            // JIKA TERINDIKASI ANEMIA
            tvHasil.setText("ANEMIA");

            // Ubah teks menjadi warna background aplikasi (#FCF5EE)
            tvHasil.setTextColor(Color.parseColor("#FCF5EE"));

            // Ubah kotak background menjadi merah
            frame7.setBackground(shapeMerah);

            tvHasilKalimat.setText("Ditemukan indikasi pucat pada beberapa area tubuh. Disarankan untuk berkonsultasi dengan tenaga medis.");
        } else {
            // JIKA NORMAL
            tvHasil.setText("NORMAL");

            // Teks tetap coklat bawaan
            tvHasil.setTextColor(Color.parseColor("#594100"));

            // Kembalikan kotak background ke gambar bawaan XML (biru muda)
            frame7.setBackgroundResource(R.drawable.frame_7);

            tvHasilKalimat.setText("Tidak terdeteksi tanda-tanda pucat yang signifikan.");
        }

        // 6. Navigasi Tombol
        btnBack.setOnClickListener(v -> finish());

        btnDeteksiKembali.setOnClickListener(v -> {
            Intent intent = new Intent(HasilDeteksiActivity.this, DeteksiKukuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}