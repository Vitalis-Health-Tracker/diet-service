package com.example.diet_service.controller;

import com.example.diet_service.dto.FoodDto;
import com.example.diet_service.model.DietModel;
import com.example.diet_service.service.DietService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/diet")
public class DietController {

    private final DietService dietService;

    public DietController(DietService dietService) {
        this.dietService = dietService;
    }

    @PostMapping("/{userId}/add-food/{foodName}")
    public Mono<Void> addFood(@PathVariable String userId, @PathVariable String foodName) {
        return dietService.addFood(userId, foodName);
    }
    @PostMapping("/{userId}/add-custom-food")
    public Mono<Void> addCustomFood(@PathVariable String userId, @RequestBody FoodDto customFood) {
        return dietService.addCustomFood(userId, customFood);
    }
    @PostMapping("/{userId}/save-diet")
    public Mono<DietModel> saveDiet(@PathVariable String userId) {
        return dietService.saveDietAndCalculateCalories(userId);
    }
    @PostMapping("/{userId}/update-diet")
    public Mono<DietModel> updateDiet(@PathVariable String userId) {
        return dietService.updateDiet(userId);
    }
    @PostMapping("/{userId}/edit-diet/{foodId}")
    public Mono<DietModel> editDiet(@PathVariable String userId, @PathVariable String foodId, @RequestBody FoodDto updatedFoodDto) {
        return dietService.editDiet(userId, foodId, updatedFoodDto);
    }
    @DeleteMapping("/{userId}/delete-diet/{foodId}")
    public Mono<Void> deleteDiet(@PathVariable String userId, @PathVariable String foodId) {
        return dietService.deleteDiet(userId, foodId).then();
    }
    @GetMapping("/{userId}/get-diet")
    public Mono<DietModel> getDiet(@PathVariable String userId) {
        return dietService.getDietByUserIdAndDate(userId, LocalDateTime.now());
    }
    @GetMapping("{userId}/get-diet-week")
    public Mono<ResponseEntity<DietModel>> getDietWeek(@PathVariable String userId, @RequestParam LocalDateTime startDate, @RequestParam LocalDateTime endDate) {
        return dietService.getDietPerWeek(userId, startDate, endDate)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(null)));
    }
}