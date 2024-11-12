package com.example.diet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodDto {
    private String foodId;
    private String foodName;
    private int foodFat;
    private int foodProtein;
    private int foodSugar;
    private int foodGrams;
    private float avgCalories; //Calories per 100gram


}
