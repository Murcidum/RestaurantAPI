package com.example.demo.repository;
import com.example.demo.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DishRepository extends JpaRepository<Dish, Long> {
    // Кастомный метод для проверки уникальности названия
    boolean existsByTitle(String title);

    // Стандартные методы Spring Data JPA:
    // save(), findById(), findAll(), deleteById() и другие
    // будут доступны автоматически
}
