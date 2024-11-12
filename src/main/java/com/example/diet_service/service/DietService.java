package com.example.diet_service.service;

import com.example.diet_service.dto.FoodDto;
import com.example.diet_service.model.DietModel;
import com.example.diet_service.repository.DietRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DietService {

    private final DietRepository dietRepository;
    private final WebClient webClient;
    private final Map<String, List<FoodDto>> temporaryFoodList = new HashMap<>();


    public DietService(DietRepository dietRepository, WebClient.Builder webClientBuilder) {
        this.dietRepository = dietRepository;
        this.webClient = webClientBuilder.baseUrl("https://sharunraj.github.io/foodApi.github.io/FoodAPI.json").build();
    }
    private Mono<FoodDto> fetchFoodDetails(String foodName) {
        return webClient.get()
                .uri("https://sharunraj.github.io/foodApi.github.io/FoodAPI.json") // replace with actual API endpoint
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> Mono.error(new RuntimeException("API request failed")))
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> {
                    for (JsonNode food : jsonNode) {
                        if (food.get("foodName").asText().equalsIgnoreCase(foodName)) {
                            return new FoodDto(
                                    food.get("foodId").asText(),
                                    food.get("foodName").asText(),
                                    food.get("foodFat").asInt(),
                                    food.get("foodProtein").asInt(),
                                    food.get("foodSugar").asInt(),
                                    food.get("foodGrams").asInt(),
                                    food.get("avgCalories").floatValue()
                            );
                        }
                    }
                    return null;
                });
    }

    public Mono<Void> addFood(String userId, String foodName) {
        return fetchFoodDetails(foodName)
                .doOnSuccess(food -> {
                    // Temporary store in the map by userId
                    // Display the food details
                    temporaryFoodList.computeIfAbsent(userId, k -> new ArrayList<>()).add(food);
                    System.out.println("Food Details: " + food);
                })
                .then();
    }

    public Mono<DietModel> saveDietAndCalculateCalories(String userId) {
        // Get the food list for the user
        List<FoodDto> foodList = temporaryFoodList.getOrDefault(userId, new ArrayList<>());

        // Calculate the total calories from the food list in a non-blocking way
        Mono<Float> totalCaloriesMono = Flux.fromIterable(foodList)
                .map(food -> (food.getAvgCalories() * food.getFoodGrams()) / 100) // Calculate calories for each food
                .reduce(0.0f, Float::sum); // Sum the calories for all foods

        // Create a new DietModel object after the total calories are calculated
        return totalCaloriesMono.flatMap(totalCalories -> {
            DietModel dietModel = new DietModel();
            dietModel.setUserId(userId);
            dietModel.setDietDate(LocalDateTime.now()); // Set the current date and time
            dietModel.setFoodList(foodList);
            dietModel.setTotalCaloriesConsumed(totalCalories); // Set the total calories

            // Save the DietModel with the total calories
            return dietRepository.save(dietModel)
                    .doOnSuccess(savedDiet -> temporaryFoodList.remove(userId)); // Clear the temporary list after saving
        });
    }







}