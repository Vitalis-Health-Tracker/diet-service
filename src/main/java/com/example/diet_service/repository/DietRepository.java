package com.example.diet_service.repository;

import com.example.diet_service.model.DietModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface DietRepository extends ReactiveMongoRepository<DietModel,String> {
    Mono<DietModel> findByUserIdAndDietDate(String userId, LocalDateTime dietDate);
}
