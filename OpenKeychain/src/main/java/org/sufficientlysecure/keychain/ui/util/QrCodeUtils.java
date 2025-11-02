/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.sufficientlysecure.keychain.KeychainApplication;
import timber.log.Timber;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

/**
 * Copied from Bitcoin Wallet
 */
public class QrCodeUtils {

    public static Bitmap getQRCodeBitmap(final Uri uri) {
        return getQRCodeBitmap(uri.toString(), 0);
    }

    public static Bitmap getQRCodeBitmap(final Uri uri, final int size) {
        // for URIs we want alphanumeric encoding to save space, thus make everything upper case!
        // zxing will then select Mode.ALPHANUMERIC internally
        return getQRCodeBitmap(uri.toString().toUpperCase(Locale.ENGLISH), size);
    }

    /**
     * Generate Bitmap with QR Code based on input.
     * @return QR Code as Bitmap
     */
    private static Bitmap getQRCodeBitmap(final String input, final int size) {

        try {

            // the qrCodeCache is handled in KeychainApplication so we can
            // properly react to onTrimMemory calls
            Bitmap bitmap = KeychainApplication.qrCodeCache.get(input);
            if (bitmap == null) {

                Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                BitMatrix result = new QRCodeWriter().encode(input, BarcodeFormat.QR_CODE, size,
                        size, hints);

                int width = result.getWidth();
                int height = result.getHeight();
                int[] pixels = new int[width * height];

                for (int y = 0; y < height; y++) {
                    final int offset = y * width;
                    for (int x = 0; x < width; x++) {
                        pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
                    }
                }

                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

                KeychainApplication.qrCodeCache.put(input, bitmap);
            }

            return bitmap;
        } catch (WriterException e) {
            Timber.e(e, "QrCodeUtils");
            return null;
        }

    }

    public static Bitmap generateQRCode(String text, int size) {
        try {
            // 设置二维码参数
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L); // 关键：设置L级纠错
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 5); // 设置边距
            // 创建二维码生成器
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            // 生成位矩阵
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hints);
            //BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
            // 转 Bitmap
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            Timber.e(e, "QrCodeUtils");
            return null;
        }
    }

    public static String decodeQRCodeFromUri(Uri uri, ContentResolver contentresolver) {
        try {
            // 从 Uri 读取图片
            Bitmap bitmap = getBitmapFromUri(uri,contentresolver);
            if (bitmap == null) {
                return null;
            }

            // 解析二维码
            String qrContent = decodeQRCodeFromBitmap(bitmap);
            return qrContent;

        } catch (Exception e) {
            Timber.e(e, "Error decoding QR code from URI: %s", uri);
            return null;
        }
    }

    private static Bitmap getBitmapFromUri(Uri uri,ContentResolver contentresolver) {
        try {
            InputStream inputStream = contentresolver.openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            return bitmap;

        } catch (Exception e) {
            Timber.e(e, "Error reading bitmap from URI: %s", uri);
            return null;
        }
    }

    private static String decodeQRCodeFromBitmap(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];

            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            MultiFormatReader reader = new MultiFormatReader();
            Result result = reader.decode(binaryBitmap);

            return result.getText();

        } catch (Exception e) {
            Timber.e(e, "Error decoding QR code from bitmap");
            return null;
        }
    }

}
