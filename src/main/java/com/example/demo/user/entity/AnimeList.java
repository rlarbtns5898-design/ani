package com.example.demo.user.entity;

import com.example.demo.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
    name = "anime_list",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "animeId"})
)
public class AnimeList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long animeId;      // mal_id
    private String title;
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}