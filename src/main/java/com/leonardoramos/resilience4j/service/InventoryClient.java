package com.leonardoramos.resilience4j.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class InventoryClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryClient.class);
    private final RestTemplate restTemplate;

    public InventoryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // A ordem importa: Retry envolve o CircuitBreaker
    @CircuitBreaker(name = "inventoryService")
    @Retry(name = "inventoryService", fallbackMethod = "finalFallback")
    public String checkStock(String productId) {
        logger.info("Chamando serviço de inventário para produto: " + productId);
        // Simulação de URL externa que não está disponível
        return restTemplate.getForObject("http://localhost:9099/api/inventory/" + productId, String.class);
    }

    public String finalFallback(String productId, Throwable t) {
        logger.error("Fallback acionado para produto {}. Causa: {}", productId, t.getMessage());
        return "Estoque Indisponível (Cache/Default) - Recuperado via Fallback Síncrono";
    }
}
