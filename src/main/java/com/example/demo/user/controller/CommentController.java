package com.example.demo.user.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.user.dto.CommentDTO;
import com.example.demo.user.entity.Board;
import com.example.demo.user.entity.Comment;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.BoardRepository;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.user.repository.CommentRepository;

import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentController {

private final CommentRepository commentRepository;
private final UserRepository userRepository;
private final BoardRepository boardRepository;

@PostMapping("/{boardId}")
public ResponseEntity<?> createComment(
        @PathVariable Long boardId,
        @RequestBody Map<String, String> request,
        @AuthenticationPrincipal UserDetails userDetails
) {
    User user = userRepository.findByUsername(userDetails.getUsername())
        .orElseThrow();

    Board board = boardRepository.findById(boardId).orElseThrow();

    Comment comment = new Comment();
    comment.setContent(request.get("content"));
    comment.setUser(user);
    comment.setBoard(board);

    commentRepository.save(comment);

    return ResponseEntity.ok().build();
}

@GetMapping("/{boardId}")
public List<CommentDTO> getComments(@PathVariable Long boardId) {
    return commentRepository.findByBoardId(boardId)
            .stream()
            .map(c -> new CommentDTO(
                    c.getId(),
                    c.getContent(),
                    c.getUser().getUsername()
            ))
            .toList();
}

@DeleteMapping("/{commentId}")
public ResponseEntity<?> deleteComment(
        @PathVariable Long commentId,
        @AuthenticationPrincipal UserDetails userDetails
) {
    Comment comment = commentRepository.findById(commentId).orElseThrow();

    if (!comment.getUser().getUsername().equals(userDetails.getUsername())) {
        throw new RuntimeException("권한 없음");
    }

    commentRepository.delete(comment);

    return ResponseEntity.ok().build();
}

@PutMapping("/{commentId}")
public ResponseEntity<?> updateComment(
        @PathVariable Long commentId,
        @RequestBody Map<String, String> request,
        @AuthenticationPrincipal UserDetails userDetails
) {
    Comment comment = commentRepository.findById(commentId).orElseThrow();

    // 작성자 검증
    if (!comment.getUser().getUsername().equals(userDetails.getUsername())) {
        throw new RuntimeException("권한 없음");
    }

    // 내용 수정
    comment.setContent(request.get("content"));
    commentRepository.save(comment);

    return ResponseEntity.ok().build();
}


    
}
