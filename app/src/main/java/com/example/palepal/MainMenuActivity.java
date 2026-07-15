package com.example.palepal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.w3c.dom.Text;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_menu), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom +40);
            return insets;
        });

        TextView btnMulaiDeteksi = findViewById(R.id.btn_mulai_deteksi);
        TextView btnTentangAnemia = findViewById(R.id.btn_tentang_anemia);

        btnMulaiDeteksi.setOnClickListener(v -> {
            Intent intent = new Intent(this, DeteksiKukuActivity.class);
            startActivity(intent);
        });

        btnTentangAnemia.setOnClickListener(v -> {
            Intent intent = new Intent(this, TentangAnemiaActivity.class);
            startActivity(intent);
        });
    }
}