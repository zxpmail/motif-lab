package com.motiflab.config;

import com.motiflab.service.AnimationGenerator;
import com.motiflab.service.ConceptNormalizer;
import com.motiflab.service.DemoCache;
import com.motiflab.service.MotifTutorService;
import com.motiflab.service.StoryboardService;
import com.motiflab.service.TeachingPackGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * Motif Lab 领域 Bean 装配：规范化、分镜、demo 缓存、授课服务。
 * Crypto/Provider/Llm/AnimationGenerator 由 @Component 注册。
 * 关联：application.yml 中 motiflab.* 配置项。
 */
@Configuration
public class MotifConfig {

    @Value("${motiflab.demo-cache-dir}")
    private String demoCacheDir;

    @Value("${motiflab.protocol-version}")
    private String protocolVersion;

    /** 概念名规范化器 */
    @Bean
    public ConceptNormalizer conceptNormalizer() {
        return new ConceptNormalizer();
    }

    /** 分镜服务（依赖规范化器） */
    @Bean
    public StoryboardService storyboardService(ConceptNormalizer conceptNormalizer) {
        return new StoryboardService(conceptNormalizer);
    }

    /** Demo HTML 缓存；目录在构造时创建 */
    @Bean
    public DemoCache demoCache() {
        return new DemoCache(Path.of(demoCacheDir), protocolVersion);
    }

    /** 授课会话状态机（注入可选的 AnimationGenerator + TeachingPackGenerator） */
    @Bean
    public MotifTutorService motifTutorService(
            ConceptNormalizer conceptNormalizer,
            StoryboardService storyboardService,
            DemoCache demoCache,
            AnimationGenerator animationGenerator,
            TeachingPackGenerator teachingPackGenerator) {
        return new MotifTutorService(
                conceptNormalizer, storyboardService, demoCache, animationGenerator, teachingPackGenerator);
    }
}
