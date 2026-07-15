package com.example.palepal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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

public class DeteksiTelapakActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private TFLiteHelper tfLiteHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deteksi_telapak);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menu_deteksi_telapak), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + 40);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView btnAmbilGambar = findViewById(R.id.btn_ambil_gambar_telapak);
        viewFinder = findViewById(R.id.group_13);

        ImageView btnFlipCamera = findViewById(R.id.btnFlipCamera);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Inisialisasi model AI Telapak
        try {
            tfLiteHelper = new TFLiteHelper(this, "model_telapak.tflite");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal memuat model AI Telapak", Toast.LENGTH_LONG).show();
        }

        btnBack.setOnClickListener(v -> finish());

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

                // Zoom lebih rendah dari konjungtiva (0.8f) karena area telapak lebih luas,
                // cukup di-zoom sedikit agar telapak tetap utuh masuk frame
                camera.getCameraControl().setLinearZoom(0.4f);

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
                Bitmap originalBitmap = image.toBitmap();
                int rotationDegrees = image.getImageInfo().getRotationDegrees();

                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                image.close();

                if (ImageUtils.isImageTooDark(rotatedBitmap)) {
                    runOnUiThread(() -> Toast.makeText(DeteksiTelapakActivity.this, "Cahaya terlalu redup/kamera tertutup!. Harap cari tempat yang terang.", Toast.LENGTH_LONG).show());
                    return; // Hentikan eksekusi
                }

                if (ImageUtils.isImageBlurry(rotatedBitmap)) {
                    runOnUiThread(() -> Toast.makeText(DeteksiTelapakActivity.this, "Gambar terlalu blur! Harap ambil gambar dengan lebih stabil.", Toast.LENGTH_LONG).show());
                    return; // Berhenti di sini, jangan lanjut ke AI
                }

                float skorKuku = getIntent().getFloatExtra("skor_kuku", 0.0f);

                TFLiteHelper.ClassificationResult hasilTelapak = null;
                if (tfLiteHelper != null) {
                    hasilTelapak = tfLiteHelper.classifyImageDetailed(rotatedBitmap);
                }

                //Cek "apakah objek terdeteksi" pakai confidence, bukan skor Anemia mentah
                if (hasilTelapak == null || hasilTelapak.confidence < 0.2f) {
                    runOnUiThread(() -> {
                        Toast.makeText(DeteksiTelapakActivity.this, "Area tidak terdeteksi. Pastikan kamera fokus pada telapak tangan!", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                final float finalSkorTelapak = hasilTelapak.probAnemia;

                runOnUiThread(() -> {
                    Intent intent = new Intent(DeteksiTelapakActivity.this, LoadingActivity.class);
                    intent.putExtra("next_screen", "konjungtiva");
                    intent.putExtra("skor_kuku", skorKuku);
                    intent.putExtra("skor_telapak", finalSkorTelapak);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(DeteksiTelapakActivity.this, "Gagal mengambil gambar telapak", Toast.LENGTH_SHORT).show());
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