package com.example.diet_service.model;

import com.example.diet_service.dto.FoodDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Diet_Service")
public class DietModel {
    @Id
    private String dietId;
    private String userId;
    private LocalDateTime dietDate;
    private float totalCaloriesConsumed;
    public List<FoodDto> foodList;
}
