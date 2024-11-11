package com.example.diet_service.service;

import com.example.diet_service.dto.FoodDto;
import com.example.diet_service.model.DietModel;
import com.example.diet_service.repository.DietRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@Service
public class DietService {

    private final DietRepository dietRepository;
    private final WebClient webClient;

    public DietService(DietRepository dietRepository, WebClient.Builder webClientBuilder) {
        this.dietRepository = dietRepository;
        this.webClient = webClientBuilder.baseUrl("http://food-service/api").build();
    }

    public Mono<DietModel> addDiet(String userId, String foodName) {
        return fetchFoodDetails(foodName)
                .flatMap(food -> dietRepository.findByUserIdAndDietDate(userId, LocalDate.now())
                        .switchIfEmpty(Mono.defer(() -> {
                            DietModel newDiet = new DietModel();
                            newDiet.setUserId(userId);
                            newDiet.setDietDate(LocalDate.now());
                            newDiet.setFoodList(List.of(food));
                            return dietRepository.save(newDiet);
                        }))
                        .flatMap(diet -> {
                            diet.getFoodList().add(food);
                            return dietRepository.save(diet);
                        }));
    }

    public Mono<Float> calculateTotalCalories(String userId, LocalDate dietDate) {
        return dietRepository.findByUserIdAndDietDate(userId, dietDate)
                .flatMapMany(diet -> Flux.fromIterable(diet.getFoodList()))
                .map(FoodDto::getCaloriesConsumed)
                .reduce(0.0f, Float::sum);
    }

    public Mono<DietModel> updateDiet(String dietId, FoodDto updatedFood) {
        return dietRepository.findById(dietId)
                .flatMap(diet -> {
                    diet.getFoodList().stream()
                            .filter(f -> f.getFoodId().equals(updatedFood.getFoodId()))
                            .findFirst()
                            .ifPresent(existingFood -> {
                                existingFood.setFoodName(updatedFood.getFoodName());
                                existingFood.setFoodFat(updatedFood.getFoodFat());
                                existingFood.setFoodSugar(updatedFood.getFoodSugar());
                                existingFood.setFoodProtein(updatedFood.getFoodProtein());
                                existingFood.setCaloriesConsumed(updatedFood.getCaloriesConsumed());
                            });
                    return dietRepository.save(diet);
                });
    }

    public Mono<Void> deleteDiet(String dietId, String foodId) {
        return dietRepository.findById(dietId)
                .flatMap(diet -> {
                    diet.getFoodList().removeIf(food -> food.getFoodId().equals(foodId));
                    return dietRepository.save(diet);
                })
                .then();
    }

    private Mono<FoodDto> fetchFoodDetails(String foodName) {
        return webClient.get()
                .uri("/foods/{name}", foodName)
                .retrieve()
                .bodyToMono(FoodDto.class);
    }
}