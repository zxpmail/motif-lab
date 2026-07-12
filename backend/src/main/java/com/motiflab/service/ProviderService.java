package com.motiflab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motiflab.dto.ProviderDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 基于文件的 LLM Provider 配置存取（避免本 Task 启用 DataSource）。
 * 路径：{@code ${user.home}/.motif-lab/provider.json}；apiKey 加密存储。
 * 关联：CryptoService、LlmClient、ProviderController。
 */
@Component
public class ProviderService {

    private static final Path STORE_PATH =
            Paths.get(System.getProperty("user.home"), ".motif-lab", "provider.json");

    private final CryptoService crypto;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProviderService(CryptoService crypto) {
        this.crypto = crypto;
    }

    /** 读取设置；不存在则返回空默认。GET 永不返回明文密钥。 */
    public ProviderDto get() {
        Stored stored = readStored();
        if (stored == null) {
            return new ProviderDto(null, "", "", false);
        }
        String plain = stored.apiKeyEnc == null || stored.apiKeyEnc.isEmpty()
                ? null
                : crypto.decrypt(stored.apiKeyEnc);
        return new ProviderDto(
                CryptoService.mask(plain),
                stored.baseUrl == null ? "" : stored.baseUrl,
                stored.model == null ? "" : stored.model,
                stored.enabled);
    }

    /**
     * 保存设置。apiKey 为空或空白 → 保留原密钥；非空则加密写入。
     * @return 脱敏后的当前设置
     */
    public ProviderDto save(ProviderDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Provider 设置不能为空");
        }
        Stored existing = readStored();
        if (existing == null) {
            existing = new Stored();
        }
        String rawBase = dto.getBaseUrl() == null ? "" : dto.getBaseUrl().trim();
        // 保存时规范化，避免把 Tepeu 的 /anthropic 地址原样留下导致调用 404
        existing.baseUrl = rawBase.isEmpty() ? "" : LlmClient.normalizeOpenAiCompatibleBaseUrl(rawBase);
        existing.model = dto.getModel() == null ? "" : dto.getModel().trim();
        existing.enabled = dto.isEnabled();

        String incoming = dto.getApiKey();
        if (incoming != null && !incoming.isBlank()) {
            existing.apiKeyEnc = crypto.encrypt(incoming.trim());
        }

        writeStored(existing);
        return get();
    }

    /** 供 LlmClient 使用：解密后的明文密钥；未配置返回 null */
    public ResolvedProvider resolveForCall() {
        Stored stored = readStored();
        if (stored == null) {
            return null;
        }
        String plain = stored.apiKeyEnc == null || stored.apiKeyEnc.isEmpty()
                ? null
                : crypto.decrypt(stored.apiKeyEnc);
        return new ResolvedProvider(
                plain,
                stored.baseUrl == null ? "" : stored.baseUrl.trim(),
                stored.model == null ? "" : stored.model.trim(),
                stored.enabled);
    }

    /** 内部调用用的已解密 Provider */
    public record ResolvedProvider(String apiKey, String baseUrl, String model, boolean enabled) {}

    /** 磁盘 JSON 结构 */
    private static class Stored {
        public String apiKeyEnc;
        public String baseUrl;
        public String model;
        public boolean enabled;
    }

    private Stored readStored() {
        if (!Files.isRegularFile(STORE_PATH)) {
            return null;
        }
        try {
            return mapper.readValue(STORE_PATH.toFile(), Stored.class);
        } catch (IOException e) {
            throw new UncheckedIOException("读取 Provider 配置失败: " + STORE_PATH, e);
        }
    }

    private void writeStored(Stored stored) {
        try {
            if (STORE_PATH.getParent() != null) {
                Files.createDirectories(STORE_PATH.getParent());
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(STORE_PATH.toFile(), stored);
        } catch (IOException e) {
            throw new UncheckedIOException("写入 Provider 配置失败: " + STORE_PATH, e);
        }
    }
}
