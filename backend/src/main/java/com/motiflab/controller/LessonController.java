package com.motiflab.controller;

import com.motiflab.dto.AnswerRequest;
import com.motiflab.dto.ApiResponse;
import com.motiflab.dto.StartLessonRequest;
import com.motiflab.model.MotifSession;
import com.motiflab.service.MotifTutorService;
import com.motiflab.service.MotifTutorService.AnswerResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授课会话 REST API。
 * 关联：MotifTutorService、ApiResponse、StartLessonRequest、AnswerRequest。
 */
@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final MotifTutorService tutor;

    public LessonController(MotifTutorService tutor) {
        this.tutor = tutor;
    }

    /** 开始一课 */
    @PostMapping
    public ApiResponse<MotifSession> start(@RequestBody StartLessonRequest request) {
        return ApiResponse.ok(tutor.start(request));
    }

    /** 按 id 取会话 */
    @GetMapping("/{id}")
    public ApiResponse<MotifSession> get(@PathVariable String id) {
        return ApiResponse.ok(tutor.get(id));
    }

    /** 升简版等级 */
    @PostMapping("/{id}/simplify")
    public ApiResponse<MotifSession> simplify(@PathVariable String id) {
        return ApiResponse.ok(tutor.simplify(id));
    }

    /** 换场景重写 */
    @PostMapping("/{id}/rewrite")
    public ApiResponse<MotifSession> rewrite(@PathVariable String id) {
        return ApiResponse.ok(tutor.rewrite(id));
    }

    /** 提交检验题答案 */
    @PostMapping("/{id}/answer")
    public ApiResponse<AnswerResult> answer(@PathVariable String id, @RequestBody AnswerRequest body) {
        return ApiResponse.ok(tutor.answerResult(id, body.getQuizId(), body.getChoiceIndex()));
    }

    /** 会话/题目不存在等参数错误 → 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.fail(ex.getMessage());
    }
}
