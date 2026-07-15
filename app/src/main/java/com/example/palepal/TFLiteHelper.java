package com.example.palepal;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

public class TFLiteHelper {

    private Interpreter interpreter;
    private int imageSizeX;
    private int imageSizeY;
    private boolean isNCHW = false; // Penanda format export YOLOv8

    // [BARU] Kelas hasil klasifikasi, memisahkan confidence deteksi dan skor Anemia
    public static class ClassificationResult {
        public final float probAnemia;
        public final float probNormal;
        public final float confidence; // nilai tertinggi antara kedua kelas, dipakai untuk cek "area terdeteksi"

        public ClassificationResult(float probAnemia, float probNormal) {
            this.probAnemia = probAnemia;
            this.probNormal = probNormal;
            this.confidence = Math.max(probAnemia, probNormal);
        }
    }

    // Inisialisasi model
    public TFLiteHelper(Context context, String modelName) throws IOException {
        MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, modelName);
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);

        interpreter = new Interpreter(tfliteModel, options);

        Tensor inputTensor = interpreter.getInputTensor(0);
        int[] shape = inputTensor.shape();

        if (shape[1] == 3) {
            isNCHW = true;
            imageSizeY = shape[2];
            imageSizeX = shape[3];
        } else {
            isNCHW = false;
            imageSizeY = shape[1];
            imageSizeX = shape[2];
        }
    }

    // [BARU] Fungsi utama klasifikasi — mengembalikan objek hasil, bukan 1 angka tunggal
    public ClassificationResult classifyImageDetailed(Bitmap bitmap) {
        if (interpreter == null) return new ClassificationResult(0.0f, 0.0f);

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, imageSizeX, imageSizeY, true);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * imageSizeX * imageSizeY * 3 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[imageSizeX * imageSizeY];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        if (isNCHW) {
            for (int i = 0; i < intValues.length; ++i) {
                byteBuffer.putFloat(((intValues[i] >> 16) & 0xFF) / 255.0f);
            }
            for (int i = 0; i < intValues.length; ++i) {
                byteBuffer.putFloat(((intValues[i] >> 8) & 0xFF) / 255.0f);
            }
            for (int i = 0; i < intValues.length; ++i) {
                byteBuffer.putFloat((intValues[i] & 0xFF) / 255.0f);
            }
        } else {
            for (int i = 0; i < intValues.length; ++i) {
                int val = intValues[i];
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((val & 0xFF) / 255.0f);
            }
        }

        float[][] output = new float[1][2];
        interpreter.run(byteBuffer, output);

        float probabilitasAnemia = output[0][0];
        float probabilitasNormal = output[0][1];

        // [DIHAPUS] Peredaman ×0.1 yang lama — skor Anemia sekarang selalu nilai asli dari softmax,
        // sehingga konsisten dengan definisi P pada formula Weighted Decision Fusion (4.1)
        return new ClassificationResult(probabilitasAnemia, probabilitasNormal);
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}