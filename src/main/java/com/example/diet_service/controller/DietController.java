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

    @PostMapping("/{userId}/food")
    public Mono<DietModel> addDiet(@PathVariable String userId, @RequestParam String foodName) {
        return dietService.addDiet(userId, foodName);
    }

    @GetMapping("/{userId}/calories")
    public Mono<Float> calculateTotalCalories(@PathVariable String userId) {
        return dietService.calculateTotalCalories(userId, LocalDate.now());
    }

    @PutMapping("/{dietId}/food")
    public Mono<DietModel> updateDiet(@PathVariable String dietId, @RequestBody FoodDto updatedFood) {
        return dietService.updateDiet(dietId, updatedFood);
    }

    @DeleteMapping("/{dietId}/food/{foodId}")
    public Mono<Void> deleteDiet(@PathVariable String dietId, @PathVariable String foodId) {
        return dietService.deleteDiet(dietId, foodId);
    }
}