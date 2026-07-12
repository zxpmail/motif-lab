package com.motiflab.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

/**
 * AES-256-GCM 静态密钥加密：用于 Provider apiKey 落盘。
 * 关联：ProviderService；配置项 motiflab.security.master-key-file。
 *
 * <p>密文格式：{@code enc:v1:} + base64(iv || ciphertext||tag)。
 * {@link #decrypt(String)} 对无前缀明文直接透传。
 */
@Component
public class CryptoService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_BITS = 256;
    private static final int KEY_BYTES = KEY_BITS / 8;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String PREFIX = "enc:v1:";

    private final SecretKey masterKey;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(@Value("${motiflab.security.master-key-file}") String masterKeyFile) {
        this.masterKey = loadOrGenerate(Paths.get(masterKeyFile));
    }

    /** 从文件加载或首次生成 32 字节主密钥 */
    private SecretKey loadOrGenerate(Path path) {
        try {
            if (Files.exists(path)) {
                byte[] decoded = Base64.getDecoder().decode(Files.readString(path).trim());
                if (decoded.length != KEY_BYTES) {
                    throw new IllegalStateException(
                            "主密钥文件损坏: 期望 " + KEY_BYTES
                                    + " 字节, 实际 " + decoded.length + " (" + path + ")");
                }
                return new SecretKeySpec(decoded, ALGORITHM);
            }
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_BITS, random);
            SecretKey generated = keyGen.generateKey();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, Base64.getEncoder().encodeToString(generated.getEncoded()));
            restrictPermissions(path);
            return generated;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("无法初始化主密钥: " + path, e);
        }
    }

    /** POSIX 下尽量限制为属主读写；Windows 依赖家目录 ACL */
    private void restrictPermissions(Path path) {
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, perms);
        } catch (UnsupportedOperationException | java.io.IOException ignore) {
            // 非 POSIX 或无法改权限时忽略
        }
    }

    /** 明文 → enc:v1:&lt;base64&gt;；null/空原样返回 */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ct, 0, combined, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /** 解密 enc:v1: 值；无前缀则透传 */
    public String decrypt(String stored) {
        if (stored == null || stored.isEmpty() || !stored.startsWith(PREFIX)) {
            return stored;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = new byte[IV_BYTES];
            byte[] ct = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);
            System.arraycopy(combined, IV_BYTES, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /** 脱敏展示：前 3 + •••• + 后 4；过短则 •••• */
    public static String mask(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        int len = key.length();
        if (len <= 8) {
            return "••••";
        }
        return key.substring(0, 3) + "••••" + key.substring(len - 4);
    }
}
