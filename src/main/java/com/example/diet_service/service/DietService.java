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
import java.time.temporal.ChronoUnit;
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

    // Method to add custom food that the user provides manually
    public Mono<Void> addCustomFood(String userId, FoodDto customFood) {
        // Add the custom food directly to the user's temporary food list
        temporaryFoodList.computeIfAbsent(userId, k -> new ArrayList<>()).add(customFood);
        System.out.println("Custom Food Added: " + customFood);
        return Mono.empty(); // No return value needed, just update the map
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
    // Update the user's diet for the current date by adding new food and recalculating the total calories
    public Mono<DietModel> updateDiet(String userId) {
        // Get the current date and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Get the start of the day (00:00:00)
        LocalDateTime todayStart = currentDateTime.truncatedTo(ChronoUnit.DAYS);

        // Get the end of the day (23:59:59.999999999)
        LocalDateTime todayEnd = currentDateTime.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        // First, check if a diet entry for the user already exists for today
        return dietRepository.findByUserIdAndDietDateBetween(userId, todayStart, todayEnd)
                .flatMap(existingDiet -> {
                    // If a diet entry already exists, add the new food items from the temporary list
                    List<FoodDto> currentFoodList = existingDiet.getFoodList();
                    List<FoodDto> newFoodList = temporaryFoodList.getOrDefault(userId, new ArrayList<>());
                    currentFoodList.addAll(newFoodList); // Add new foods to the existing list

                    // Recalculate total calories based on the updated food list
                    float totalCalories = (float) currentFoodList.stream()
                            .mapToDouble(food -> (food.getAvgCalories() * food.getFoodGrams()) / 100) // Calculate calories for each food item
                            .sum();

                    // Set the updated food list and total calories
                    existingDiet.setFoodList(currentFoodList);
                    existingDiet.setTotalCaloriesConsumed(totalCalories);

                    // Save the updated DietModel
                    return dietRepository.save(existingDiet)
                            .doOnSuccess(savedDiet -> {
                                // Clear the temporary food list after saving
                                temporaryFoodList.remove(userId);
                                System.out.println("Diet updated for user: " + userId);
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("No diet entry found for user on current date")));
    }

    public Mono<DietModel> editDiet(String userId, String foodId, FoodDto updatedFoodDto) {
        // Get the current date and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Get the start of the day (00:00:00)
        LocalDateTime todayStart = currentDateTime.truncatedTo(ChronoUnit.DAYS);

        // Get the end of the day (23:59:59.999999999)
        LocalDateTime todayEnd = currentDateTime.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        // Find the existing diet entry for the user today
        return dietRepository.findByUserIdAndDietDateBetween(userId, todayStart, todayEnd)
                .flatMap(existingDiet -> {
                    // Get the current food list
                    List<FoodDto> currentFoodList = existingDiet.getFoodList();

                    // Find the food item to edit
                    FoodDto foodToEdit = currentFoodList.stream()
                            .filter(food -> food.getFoodId().equals(foodId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Food item not found"));

                    // Update the food details with the new values from updatedFoodDto
                    foodToEdit.setFoodName(updatedFoodDto.getFoodName());
                    foodToEdit.setFoodGrams(updatedFoodDto.getFoodGrams());
                    foodToEdit.setAvgCalories(updatedFoodDto.getAvgCalories());
                    foodToEdit.setFoodFat(updatedFoodDto.getFoodFat());
                    foodToEdit.setFoodProtein(updatedFoodDto.getFoodProtein());
                    foodToEdit.setFoodSugar(updatedFoodDto.getFoodSugar());

                    // Recalculate total calories after the update
                    float totalCalories = (float) currentFoodList.stream()
                            .mapToDouble(food -> (food.getAvgCalories() * food.getFoodGrams()) / 100)
                            .sum();

                    // Set the updated food list and total calories
                    existingDiet.setFoodList(currentFoodList);
                    existingDiet.setTotalCaloriesConsumed(totalCalories);

                    // Save the updated DietModel
                    return dietRepository.save(existingDiet)
                            .doOnSuccess(savedDiet -> {
                                System.out.println("Diet updated for user: " + userId);
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("No diet entry found for user on current date")));
    }

    public Mono<DietModel> deleteDiet(String userId, String foodId) {
        // Get the current date and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Get the start of the day (00:00:00)
        LocalDateTime todayStart = currentDateTime.truncatedTo(ChronoUnit.DAYS);

        // Get the end of the day (23:59:59.999999999)
        LocalDateTime todayEnd = currentDateTime.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        // Find the existing diet entry for the user today
        return dietRepository.findByUserIdAndDietDateBetween(userId, todayStart, todayEnd)
                .flatMap(existingDiet -> {
                    // Get the current food list
                    List<FoodDto> currentFoodList = existingDiet.getFoodList();

                    // Find and remove the food item based on foodId
                    FoodDto foodToRemove = currentFoodList.stream()
                            .filter(food -> food.getFoodId().equals(foodId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Food item not found"));

                    // Remove the food item from the list
                    currentFoodList.remove(foodToRemove);

                    // Recalculate total calories after removing the food item
                    float totalCalories = (float) currentFoodList.stream()
                            .mapToDouble(food -> (food.getAvgCalories() * food.getFoodGrams()) / 100)
                            .sum();

                    // Set the updated food list and total calories
                    existingDiet.setFoodList(currentFoodList);
                    existingDiet.setTotalCaloriesConsumed(totalCalories);

                    // Save the updated DietModel
                    return dietRepository.save(existingDiet)
                            .doOnSuccess(savedDiet -> {
                                System.out.println("Food deleted from diet for user: " + userId);
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("No diet entry found for user on current date")));
    }

    public Mono<DietModel> getDietByUserIdAndDate(String userId, LocalDateTime date) {
        LocalDateTime todayStart = date.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime todayEnd = date.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        return dietRepository.findByUserIdAndDietDateBetween(userId, todayStart, todayEnd)
                .switchIfEmpty(Mono.error(new RuntimeException("No diet entry found for user on the specified date")));
    }




}