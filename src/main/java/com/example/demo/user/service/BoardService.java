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

        // 자기 글만 삭제 가능
        if (!board.getUser().getUsername().equals(username)) {
            throw new RuntimeException("권한 없음");
        }

        boardRepository.delete(board);
    }
}
