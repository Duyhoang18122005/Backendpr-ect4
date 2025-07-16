package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class VnPayService {
    private static final Logger log = LoggerFactory.getLogger(VnPayService.class);

    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;
    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;
    @Value("${vnpay.payUrl}")
    private String vnp_Url;
    @Value("${vnpay.returnUrl}")
    private String vnp_ReturnUrl;

    public String createPaymentUrl(Long amount, String orderInfo, String ipAddr, String txnRef) throws UnsupportedEncodingException {
        log.info("=== BẮT ĐẦU: Tạo VNPay payment URL ===");
        log.info("Input params: amount={}, orderInfo={}, ipAddr={}, txnRef={}", amount, orderInfo, ipAddr, txnRef);

        try {
            log.info("Bước 1: Tạo vnp_Params");
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay yêu cầu x100
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", txnRef);
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", ipAddr);
            vnp_Params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            vnp_Params.put("vnp_SecureHashType", "SHA512"); // Chỉ gửi lên URL, không dùng để ký
            log.info("vnp_Params đã tạo: {}", vnp_Params);

            // Tạo bản sao để ký, loại bỏ vnp_SecureHashType
            Map<String, String> paramsForHash = new HashMap<>(vnp_Params);
            paramsForHash.remove("vnp_SecureHashType");

            log.info("Bước 2: Sắp xếp params theo thứ tự alphabet");
            List<String> fieldNames = new ArrayList<>(paramsForHash.keySet());
            Collections.sort(fieldNames);
            log.info("Field names đã sắp xếp: {}", fieldNames);

            log.info("Bước 3: Tạo hash data và query theo chuẩn VNPay");
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();

            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = paramsForHash.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Build hash data - CHỈ ENCODE VALUE, KHÔNG ENCODE KEY
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    // Build query - ENCODE BOTH KEY AND VALUE
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            // Thêm vnp_SecureHashType vào query (không vào hashData)
            query.append("&vnp_SecureHashType=SHA512");

            log.info("Hash data: {}", hashData.toString());
            log.info("Query: {}", query.toString());

            log.info("Bước 4: Tạo secure hash");
            String secureHash = hmacSHA512(vnp_HashSecret, hashData.toString()); // Đổi sang SHA512
            log.info("Secure hash: {}", secureHash);

            log.info("Bước 5: Tạo payment URL");
            String queryUrl = query.toString();
            queryUrl += "&vnp_SecureHash=" + secureHash;
            String paymentUrl = vnp_Url + "?" + queryUrl;
            log.info("Payment URL cuối cùng: {}", paymentUrl);

            log.info("=== KẾT THÚC: Tạo VNPay payment URL thành công ===");
            return paymentUrl;

        } catch (Exception e) {
            log.error("=== LỖI: Tạo VNPay payment URL thất bại ===");
            log.error("Exception type: {}", e.getClass().getSimpleName());
            log.error("Exception message: {}", e.getMessage());
            log.error("Stack trace:", e);
            throw e;
        }
    }

    private String hmacSHA256(String key, String data) {
        try {
            Mac hmac256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac256.init(secretKey);
            byte[] bytes = hmac256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (Exception ex) {
            throw new RuntimeException("Lỗi tạo chữ ký VNPay", ex);
        }
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (Exception ex) {
            throw new RuntimeException("Lỗi tạo chữ ký VNPay", ex);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public boolean verifyVnpayCallback(Map<String, String> params) {
        try {
            String receivedHash = params.get("vnp_SecureHash");
            if (receivedHash == null) {
                log.error("Không tìm thấy vnp_SecureHash trong callback");
                return false;
            }

            // Loại bỏ các field không cần thiết cho việc tính hash
            Map<String, String> paramsForHash = new HashMap<>(params);
            paramsForHash.remove("vnp_SecureHash");
            paramsForHash.remove("vnp_SecureHashType");

            List<String> fieldNames = new ArrayList<>(paramsForHash.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();

            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = paramsForHash.get(fieldName);
                if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            String calculatedHash = hmacSHA512(vnp_HashSecret, hashData.toString()); // Đổi sang SHA512
            log.info("Received hash: {}", receivedHash);
            log.info("Calculated hash: {}", calculatedHash);
            log.info("Hash data: {}", hashData.toString());

            boolean isValid = calculatedHash.equalsIgnoreCase(receivedHash);
            log.info("Hash verification result: {}", isValid);
            return isValid;

        } catch (Exception e) {
            log.error("Lỗi khi verify VNPay callback: {}", e.getMessage());
            return false;
        }
    }
} 