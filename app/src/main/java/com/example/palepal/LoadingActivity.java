package com.example.palepal;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {
    private static final String TAG = "RAKTA_FLOW";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        Intent intentMasuk = getIntent();
        String nextScreen = intentMasuk.getStringExtra("next_screen");

        // Log ini sangat penting untuk kamu cek di Logcat
        Log.d(TAG, "Loading menerima perintah ke: " + nextScreen);

        float skorKuku = intentMasuk.getFloatExtra("skor_kuku", 0.0f);
        float skorTelapak = intentMasuk.getFloatExtra("skor_telapak", 0.0f);
        float skorKonjungtiva = intentMasuk.getFloatExtra("skor_konjungtiva", 0.0f);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intentKeluar = null;

            if ("telapak".equals(nextScreen)) {
                intentKeluar = new Intent(LoadingActivity.this, DeteksiTelapakActivity.class);
            } else if ("konjungtiva".equals(nextScreen)) {
                intentKeluar = new Intent(LoadingActivity.this, DeteksiKonjungtivaActivity.class);
            } else if ("hasil".equals(nextScreen)) {
                intentKeluar = new Intent(LoadingActivity.this, HasilDeteksiActivity.class);
            } else {
                // JIKA ERROR ATAU DATA KOSONG, KEMBALIKAN KE MENU UTAMA (Contoh)
                Log.e(TAG, "Navigasi gagal, nextScreen tidak dikenal: " + nextScreen);
                intentKeluar = new Intent(LoadingActivity.this, MainMenuActivity.class);
            }

            // Meneruskan data
            intentKeluar.putExtra("skor_kuku", skorKuku);
            intentKeluar.putExtra("skor_telapak", skorTelapak);
            intentKeluar.putExtra("skor_konjungtiva", skorKonjungtiva);

            startActivity(intentKeluar);
            finish(); // Penting agar user tidak bisa kembali ke layar loading

        }, 2000);
    }
}