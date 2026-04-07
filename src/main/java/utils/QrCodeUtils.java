package utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaires pour la génération de codes QR
 */
public class QrCodeUtils {

    /**
     * Génère une image de code QR encodée en Base64
     * @param text Le texte à encoder dans le QR Code
     * @param width Largeur de l'image
     * @param height Hauteur de l'image
     * @return La chaîne Base64 de l'image (format PNG)
     */
    public static String generateQrCodeBase64(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();

        return Base64.getEncoder().encodeToString(pngData);
    }

    /**
     * Helper pour obtenir la balise src complète (data:image/png;base64,...)
     */
    public static String getQrCodeImageSrc(String text, int width, int height) {
        try {
            return "data:image/png;base64," + generateQrCodeBase64(text, width, height);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
