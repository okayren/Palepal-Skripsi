package com.example.palepal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix; //untuk rotasi
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeteksiKonjungtivaActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private TFLiteHelper tfLiteHelper;

    // [BARU] Variabel kamera depan/belakang
    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deteksi_konjungtiva);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menu_deteksi_konjungtiva), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + 40);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView btnAmbilGambar = findViewById(R.id.btn_ambil_gambar_konjungtiva);
        viewFinder = findViewById(R.id.group_13);

        // [BARU] Inisialisasi tombol Flip Camera
        ImageView btnFlipCamera = findViewById(R.id.btnFlipCamera);

        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            tfLiteHelper = new TFLiteHelper(this, "model_konjungtiva.tflite");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal memuat model AI Konjungtiva", Toast.LENGTH_LONG).show();
        }

        btnBack.setOnClickListener(v -> finish());

        // [BARU] Aksi saat tombol Flip ditekan
        btnFlipCamera.setOnClickListener(v -> {
            if (lensFacing == CameraSelector.LENS_FACING_BACK){
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            }
            startCamera();
        });

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
        }

        btnAmbilGambar.setOnClickListener(v -> takePhoto());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                cameraProvider.unbindAll();

                // [BARU] Simpan ke variabel camera dan aktifkan Zoom
                androidx.camera.core.Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                // Set Zoom ke 30% (0.3f). Kamu bisa ubah angkanya (misal 0.2f atau 0.4f) jika kurang pas
                camera.getCameraControl().setLinearZoom(0.8f);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private void takePhoto() {
        if (imageCapture == null) return;

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                try {
                    Bitmap originalBitmap = image.toBitmap();
                    int rotationDegrees = image.getImageInfo().getRotationDegrees();
                    image.close(); // Tutup memori gambar bawaan secepatnya

                    // [PENTING] Perkecil ukuran dulu agar HP tidak RAM-nya jebol saat diputar
                    Bitmap downscaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 224, 224, true);
                    originalBitmap.recycle(); // Buang gambar raksasa asli

                    // Putar gambar yang sudah dikecilkan
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationDegrees);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(downscaledBitmap, 0, 0, downscaledBitmap.getWidth(), downscaledBitmap.getHeight(), matrix, true);
                    downscaledBitmap.recycle(); //buang gambar sementara

                    if (ImageUtils.isImageTooDark(rotatedBitmap)) {
                        runOnUiThread(() -> Toast.makeText(DeteksiKonjungtivaActivity.this, "Cahaya terlalu redup/kamera tertutup! Harap cari tempat yang terang.", Toast.LENGTH_LONG).show());
                        return; //menghentikan eksekusi
                    }

                    if (ImageUtils.isImageBlurry(rotatedBitmap)) {
                        runOnUiThread(() -> Toast.makeText(DeteksiKonjungtivaActivity.this, "Gambar terlalu blur! Harap ambil gambar dengan lebih stabil.", Toast.LENGTH_LONG).show());
                        return; // Berhenti di sini, jangan lanjut ke AI
                    }

                    //mengambil skor titipan (sudah berupa probabilitas Anemia mentah, sudah lolos cek confidence di activity masing-masing)
                    float skorKuku = getIntent().getFloatExtra("skor_kuku", 0.0f);
                    float skorTelapak = getIntent().getFloatExtra("skor_telapak", 0.0f);

                    // Analisis AI
                    TFLiteHelper.ClassificationResult hasilMata = null;
                    if (tfLiteHelper != null) {
                        hasilMata = tfLiteHelper.classifyImageDetailed(rotatedBitmap);
                    }

                    // Cek confidence konjungtiva sendiri saja — kuku & telapak sudah lolos pengecekan
                    // confidence masing-masing di activity sebelumnya, tidak perlu dicek ulang di sini
                    if (hasilMata == null || hasilMata.confidence < 0.2f) {
                        runOnUiThread(() -> {
                            Toast.makeText(DeteksiKonjungtivaActivity.this, "Area tidak terdeteksi. Pastikan kamera fokus pada konjungtiva!", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    final float finalSkorMata = hasilMata.probAnemia;

                    // Bawa ke halaman akhir
                    runOnUiThread(() -> {
                        Intent intent = new Intent(DeteksiKonjungtivaActivity.this, LoadingActivity.class);
                        intent.putExtra("next_screen", "hasil");
                        intent.putExtra("skor_kuku", skorKuku);
                        intent.putExtra("skor_telapak", skorTelapak);
                        intent.putExtra("skor_konjungtiva", finalSkorMata);
                        startActivity(intent);
                        finish();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(DeteksiKonjungtivaActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(DeteksiKonjungtivaActivity.this, "Gagal mengambil gambar mata", Toast.LENGTH_SHORT).show());
            }
        });
    }
    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (tfLiteHelper != null) {
            tfLiteHelper.close();
        }
    }
}