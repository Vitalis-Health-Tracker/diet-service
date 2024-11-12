package com.example.diet_service.controller;

import com.example.diet_service.dto.FoodDto;
import com.example.diet_service.model.DietModel;
import com.example.diet_service.service.DietService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/diet")
public class DietController {

    private final DietService dietService;

    public DietController(DietService dietService) {
        this.dietService = dietService;
    }

    @PostMapping("/{userId}/food/{foodName}")
    public Mono<Void> addFood(@PathVariable String userId, @PathVariable String foodName) {
        return dietService.addFood(userId, foodName);
    }
    @PostMapping("/{userId}/save-diet")
    public Mono<DietModel> saveDiet(@PathVariable String userId) {
        return dietService.saveDietAndCalculateCalories(userId);
    }
}