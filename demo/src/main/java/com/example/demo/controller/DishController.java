package com.example.demo.controller;

import com.example.demo.dto.DishCreateRequest;
import com.example.demo.dto.DishUpdateRequest;
import com.example.demo.entity.Dish;
import com.example.demo.repository.DishRepository;
import com.example.demo.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class DishController {

    private final DishRepository dishRepository;
    private final FileStorageService fileStorageService;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");

    public DishController(DishRepository dishRepository,
                          FileStorageService fileStorageService) {
        this.dishRepository = dishRepository;
        this.fileStorageService = fileStorageService;
    }

    //Создание блюда
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createDish(
            @Valid @ModelAttribute DishCreateRequest request,
            BindingResult bindingResult) {

        // Проверка уникальности названия
        if (dishRepository.existsByTitle(request.getTitle())) {
            bindingResult.rejectValue("title", "error.title", "Title already exists");
        }

        // Валидация файла
        if (request.getImage().isEmpty()) {
            bindingResult.rejectValue("image", "error.image", "Image is required");
        }

        if (bindingResult.hasErrors()) {
            return buildErrorResponse(bindingResult);
        }

        try {
            // Сохранение файла
            String fileName = fileStorageService.storeFile(request.getImage());

            Dish dish = new Dish();
            dish.setTitle(request.getTitle());
            dish.setAnons(request.getAnons());
            dish.setText(request.getText());
            dish.setTags(request.getTags());
            dish.setImagePath(fileName);

            Dish savedDish = dishRepository.save(dish);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("status-text", "Successful creation")
                    .body(Map.of(
                            "status", true,
                            "post_id", savedDish.getId()
                    ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .header("status-text", "Creating error")
                    .body(Map.of(
                            "status", false,
                            "message", e.getMessage()
                    ));
        }
    }

    // Заглушка для редактирования (POST)
    @PostMapping("/{id}")
    public ResponseEntity<?> updateDish(
            @PathVariable Long id,
            @Valid @ModelAttribute DishUpdateRequest request,
            BindingResult bindingResult) {

        // Проверка существования блюда
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Post not found"));

        // Валидация уникальности title
        if (request.getTitle() != null &&
                !request.getTitle().equals(dish.getTitle())) {

            if (dishRepository.existsByTitle(request.getTitle())) {
                bindingResult.rejectValue("title",
                        "unique", "Title must be unique");
            }
        }

        // Валидация изображения
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {
                if (request.getImage().isEmpty()) throw new RuntimeException("File is empty");

                String originalFilename = request.getImage().getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);

                if (!Set.of("jpg", "png").contains(extension.toLowerCase())) {
                    throw new RuntimeException("Invalid file format");
                }

                if (request.getImage().getSize() > 2 * 1024 * 1024) {
                    throw new RuntimeException("File size exceeds 2MB");
                }
            } catch (RuntimeException e) {
                bindingResult.rejectValue("image",
                        "invalid", e.getMessage());
            }
        }

        if (bindingResult.hasErrors()) {
            return buildErrorResponse(bindingResult);
        }

        // Обновление полей
        if (request.getTitle() != null) dish.setTitle(request.getTitle());
        if (request.getAnons() != null) dish.setAnons(request.getAnons());
        if (request.getText() != null) dish.setText(request.getText());
        if (request.getTags() != null) dish.setTags(request.getTags());

        // Сохранение изображения
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String newFilename = fileStorageService.storeFile(request.getImage());
            dish.setImagePath(newFilename);
        }

        Dish updatedDish = dishRepository.save(dish);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header("status-text", "Successful creation")
                .body(Map.of(
                        "status", true,
                        "post", buildResponse(updatedDish)
                ));
    }


    private Map<String, String> getErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return errors;
    }

    // Заглушка для удаления (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDish(@PathVariable Long id) {
        return dishRepository.findById(id)
                .map(dish -> {
                    // Удаление файла изображения
                    try {
                        Path filePath = Paths.get("post_images").resolve(dish.getImagePath());
                        Files.deleteIfExists(filePath);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete image file", e);
                    }
                    // Удаление записи из БД
                    dishRepository.delete(dish);

                    return ResponseEntity.status(HttpStatus.CREATED)
                            .header("status-text", "Successful delete")
                            .body(Map.of("status", true));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("status-text", "Post not found")
                        .body(Map.of("message", Boolean.valueOf("Post not found"))));
    }

    @GetMapping
    public ResponseEntity<?> getAllDishes() {
        List<Dish> dishes = dishRepository.findAll();

        List<Map<String, Object>> response = dishes.stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok()
                .header("status-text", "List posts")
                .body(response);
    }


    //Создает ответ из 1 блюда со всеми атрибутами
    private Map<String, Object> buildResponse(Dish dish) {
        return Map.of(
                "title", dish.getTitle(),
                "datetime", dish.getCreatedAt().format(DATE_FORMATTER),
                "anons", dish.getAnons(),
                "text", dish.getText(),
                "tags", Arrays.asList(dish.getTags().split(",\\s*")),
                "image", "/post_images/" + dish.getImagePath()
        );
    }

    //Создает ответ об ошибке
    private ResponseEntity<?> buildErrorResponse(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .header("status-text", "Creating error")
                .body(Map.of(
                        "status", false,
                        "message", errors
                ));
    }
}