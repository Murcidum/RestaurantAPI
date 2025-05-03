package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {

    private final Path rootLocation = Paths.get("post_images");
    private final Set<String> allowedExtensions = Set.of("jpg", "png");
    private final long maxFileSize = 2 * 1024 * 1024; // 2MB

    public FileStorageService() {
        try {
            Files.createDirectories(rootLocation); // Создание папки
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public String storeFile(MultipartFile file) {
        try {
            // Проверка расширения
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            if (!allowedExtensions.contains(extension.toLowerCase())) {
                throw new RuntimeException("Invalid file format. Allowed: jpg, png");
            }

            // Проверка размера
            if (file.getSize() > maxFileSize) {
                throw new RuntimeException("File size exceeds 2MB limit");
            }

            // Генерация уникального имени
            String newFilename = UUID.randomUUID() + "." + extension;
            Path destination = rootLocation.resolve(newFilename);

            // Сохранение с заменой существующих
            Files.copy(file.getInputStream(), destination,
                    StandardCopyOption.REPLACE_EXISTING);

            return newFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}