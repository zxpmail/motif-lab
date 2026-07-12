package com.motiflab.controller;

import com.motiflab.dto.ApiResponse;
import com.motiflab.dto.ProviderDto;
import com.motiflab.service.ProviderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * LLM Provider 设置 API。
 * 关联：ProviderService、ProviderDto。
 */
@RestController
@RequestMapping("/api/provider")
public class ProviderController {

    private final ProviderService providers;

    public ProviderController(ProviderService providers) {
        this.providers = providers;
    }

    /** 读取设置（apiKey 已脱敏） */
    @GetMapping
    public ApiResponse<ProviderDto> get() {
        return ApiResponse.ok(providers.get());
    }

    /** 保存设置；apiKey 空白表示保留原密钥 */
    @PutMapping
    public ApiResponse<ProviderDto> put(@RequestBody ProviderDto body) {
        return ApiResponse.ok(providers.save(body));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.fail(ex.getMessage());
    }
}
