package com.example.demo.user.controller;

import com.example.demo.user.dto.BoardRequest;
import com.example.demo.user.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board") // 프론트랑 구분 위해 api 붙이는게 좋음
public class BoardController {

    private final BoardService boardService;

    // 게시글 전체 조회
    @GetMapping("")
    public List<?> boardList() {
        return boardService.findAll();
    }

    // 게시글 작성
    @PostMapping("")
    public ResponseEntity<?> write(@RequestBody BoardRequest request,
                      @AuthenticationPrincipal UserDetails userDetails) {

        boardService.write(
                request.getTitle(),
                request.getContent(),
                userDetails.getUsername()
        );
        return ResponseEntity.ok(Map.of("message", "작성 완료"));
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @AuthenticationPrincipal UserDetails userDetails) {

        boardService.delete(id, userDetails.getUsername());
    }

    // 게시글 상세 조회
    @GetMapping("/{id}")
    public Object detail(@PathVariable Long id) {
        return boardService.findById(id);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id,
                   @RequestBody BoardRequest request,
                   @AuthenticationPrincipal UserDetails userDetails) {

    boardService.update(
            id,
            request.getTitle(),
            request.getContent(),
            userDetails.getUsername()
    );
}
}