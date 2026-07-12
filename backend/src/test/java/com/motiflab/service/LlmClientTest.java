package com.motiflab.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmClientTest {

    @Test
    void normalize_rewritesDeepSeekAnthropicToV1() {
        assertEquals(
                "https://api.deepseek.com/v1",
                LlmClient.normalizeOpenAiCompatibleBaseUrl("https://api.deepseek.com/anthropic"));
        assertEquals(
                "https://api.deepseek.com/v1",
                LlmClient.normalizeOpenAiCompatibleBaseUrl("https://api.deepseek.com/anthropic/"));
    }

    @Test
    void normalize_addsV1ForHostOnly() {
        assertEquals(
                "https://api.deepseek.com/v1",
                LlmClient.normalizeOpenAiCompatibleBaseUrl("https://api.deepseek.com"));
        assertEquals(
                "https://api.openai.com/v1",
                LlmClient.normalizeOpenAiCompatibleBaseUrl("https://api.openai.com"));
    }

    @Test
    void normalize_keepsExistingV1() {
        assertEquals(
                "https://api.deepseek.com/v1",
                LlmClient.normalizeOpenAiCompatibleBaseUrl("https://api.deepseek.com/v1"));
    }
}
