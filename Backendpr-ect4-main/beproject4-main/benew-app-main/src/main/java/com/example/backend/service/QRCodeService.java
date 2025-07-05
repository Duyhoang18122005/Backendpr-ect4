package com.example.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class QRCodeService {
    
    public String generateQRCode(String content, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public String generatePaymentQRCode(String method, String amount, String userId, String transactionId) throws WriterException, IOException {
        String content = switch (method.toUpperCase()) {
            case "MOMO" -> String.format("momo://payment?amount=%s&userId=%s&transactionId=%s", amount, userId, transactionId);
            case "VNPAY" -> String.format("vnpay://payment?amount=%s&userId=%s&transactionId=%s", amount, userId, transactionId);
            case "ZALOPAY" -> String.format("zalopay://payment?amount=%s&userId=%s&transactionId=%s", amount, userId, transactionId);
            default -> throw new IllegalArgumentException("Unsupported payment method: " + method);
        };
        
        return generateQRCode(content, 300, 300);
    }
} 