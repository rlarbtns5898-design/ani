package com.example.demo.user.service;

import com.example.demo.user.entity.Board;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.BoardRepository;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public List<Board> findAll(){
        return boardRepository.findAll();
    }
    public Board findById(Long id) {
        return boardRepository.findById(id)
                .orElseThrow();
    }
    public void write(String title, String content, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow();

        Board board = Board.builder()
                .title(title)
                .content(content)
                .user(user)
                .build();

        boardRepository.save(board);
    }

    public void delete(Long id, String username) {
        Board board = boardRepository.findById(id)
                .orElseThrow();


        if (!board.getUser().getUsername().equals(username)) {
            throw new RuntimeException("권한 없음");
        }

        boardRepository.delete(board);
    }

    public void update(Long id, String title, String content, String username) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시글 없음"));
    
        // 작성자 체크 (중요)
        if (!board.getUser().getUsername().equals(username)) {
            throw new RuntimeException("수정 권한 없음");
        }
    
        board.setTitle(title);
        board.setContent(content);
    
        boardRepository.save(board);
    }
}
