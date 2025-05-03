package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor // Конструктор без аргументов
@AllArgsConstructor // Конструктор со всеми полями
@Table(name = "dishes")
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(unique = true)
    private String title;

    @NotBlank(message = "Anons is required")
    private String anons;

    @NotBlank(message = "Text is required")
    @Column(columnDefinition = "TEXT")
    private String text;

    private String tags;

    @Column(nullable = false)
    private String imagePath;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank(message = "Title is required") String getTitle() {
        return title;
    }

    public void setTitle(@NotBlank(message = "Title is required") String title) {
        this.title = title;
    }

    public @NotBlank(message = "Anons is required") String getAnons() {
        return anons;
    }

    public void setAnons(@NotBlank(message = "Anons is required") String anons) {
        this.anons = anons;
    }

    public @NotBlank(message = "Text is required") String getText() {
        return text;
    }

    public void setText(@NotBlank(message = "Text is required") String text) {
        this.text = text;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    // Lombok создаст конструкторы, геттеры и сеттеры через @Data
}
