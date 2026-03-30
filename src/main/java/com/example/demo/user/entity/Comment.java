package com.example.demo.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Comment {

    @Id @GeneratedValue
    private Long id;

    private String content;

    @ManyToOne
    private User user; // 작성자

    @ManyToOne
    private Board board; // 어떤 글에 달린 댓글
}
