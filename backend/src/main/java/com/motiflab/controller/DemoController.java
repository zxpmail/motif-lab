package com.motiflab.controller;

import com.motiflab.model.MotifSession;
import com.motiflab.service.DemoCache;
import com.motiflab.service.MotifTutorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 演示 HTML 文件 HTTP 出口（iframe 加载）。
 * 关联：MotifTutorService、DemoCache；前端 iframe sandbox="allow-scripts"。
 * 注意：不设置会阻断脚本的 CSP。
 */
@RestController
public class DemoController {

    private final MotifTutorService tutor;
    private final DemoCache demoCache;

    public DemoController(MotifTutorService tutor, DemoCache demoCache) {
        this.tutor = tutor;
        this.demoCache = demoCache;
    }

    /** 按会话 id 返回当前 level 的演示 HTML */
    @GetMapping(value = "/api/demos/{sessionId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getDemo(@PathVariable String sessionId) {
        MotifSession session;
        try {
            session = tutor.get(sessionId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        Optional<Path> path = demoCache.resolve(session.getConceptId(), session.getLevel());
        if (path.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "演示文件不存在");
        }

        try {
            String html = Files.readString(path.get(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "读取演示文件失败");
        }
    }
}
