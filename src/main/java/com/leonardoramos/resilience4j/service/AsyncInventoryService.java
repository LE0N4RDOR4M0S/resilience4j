package com.leonardoramos.resilience4j.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Service
public class AsyncInventoryService {
    private final RestTemplate restTemplate;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public AsyncInventoryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @TimeLimiter(name = "inventoryService", fallbackMethod = "fallbackTimeout")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackCircuitBreaker")
    public CompletableFuture<String> checkStockAsync(String productId) {
        return CompletableFuture.supplyAsync(() -> {
            // URL fictícia
            return restTemplate.getForObject("http://localhost:9090/api/inventory/" + productId, String.class);
        }, executorService);
    }

    public CompletableFuture<String> fallbackTimeout(String productId, TimeoutException e) {
        return CompletableFuture.completedFuture("Fallback: Operação excedeu o tempo limite (TimeLimiter)");
    }

    public CompletableFuture<String> fallbackCircuitBreaker(String productId, CallNotPermittedException e) {
        return CompletableFuture.completedFuture("Fallback: Circuito Aberto - Chamada bloqueada imediatamente");
    }

    // Fallback genérico para outras exceções
    public CompletableFuture<String> fallbackCircuitBreaker(String productId, Throwable e) {
        return CompletableFuture.completedFuture("Fallback Genérico Async: " + e.getMessage());
    }
}
