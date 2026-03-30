package com.example.demo.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private String content;
    private String username;
}
