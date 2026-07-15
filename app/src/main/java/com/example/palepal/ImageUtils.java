package com.example.palepal;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ImageUtils {
    // Fungsi statis untuk mengecek apakah gambar terlalu gelap
    public static boolean isImageTooDark(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        long totalLuminance = 0;

        // Melompat setiap 5 piksel agar komputasi sangat ringan & tidak bikin HP lag
        for (int x = 0; x < width; x += 5) {
            for (int y = 0; y < height; y += 5) {
                int pixel = bitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                // Rumus standar kecerahan warna (Luminance)
                int luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                totalLuminance += luminance;
            }
        }

        int sampledPixels = (width / 5) * (height / 5);
        if (sampledPixels == 0) return true; // Keamanan tambahan

        long averageLuminance = totalLuminance / sampledPixels;

        // Jika rata-rata kecerahan di bawah 50, berarti terlalu gelap
        return averageLuminance < 50;
    }

    public static boolean isImageBlurry(Bitmap bitmap) {
        // Perkecil gambar dulu agar proses hitungnya secepat kilat (tidak bikin lag)
        Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, 150, 150, true);

        int width = smallBitmap.getWidth();
        int height = smallBitmap.getHeight();
        long sum = 0;
        long sumSquares = 0;
        int count = 0;

        // Mengecek ketajaman tepi (edge) piksel
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int p1 = getGray(smallBitmap.getPixel(x, y - 1));
                int p2 = getGray(smallBitmap.getPixel(x - 1, y));
                int p3 = getGray(smallBitmap.getPixel(x, y));
                int p4 = getGray(smallBitmap.getPixel(x + 1, y));
                int p5 = getGray(smallBitmap.getPixel(x, y + 1));

                // Rumus Matriks Laplacian
                int laplacian = p1 + p2 + p4 + p5 - (4 * p3);

                sum += laplacian;
                sumSquares += (laplacian * laplacian);
                count++;
            }
        }
        smallBitmap.recycle(); // Bersihkan memori

        if (count == 0) return true;

        // Menghitung Varians
        double mean = (double) sum / count;
        double variance = ((double) sumSquares / count) - (mean * mean);

        // Jika varians (tingkat ketajaman) di bawah 50, berarti gambar nge-blur
        // Catatan: Angka 50 ini bisa kamu naikkan ke 70 atau 100 kalau dirasa masih kurang ketat
        return variance < 50;
    }

    // Fungsi bantuan untuk mengubah warna menjadi format keabuan (grayscale)
    private static int getGray(int pixel) {
        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }
}
