package com.leonardoramos.resilience4j.controller;

import com.leonardoramos.resilience4j.service.AsyncInventoryService;
import com.leonardoramos.resilience4j.service.InventoryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/products")
public class InventoryController {

    private final InventoryClient inventoryClient;
    private final AsyncInventoryService asyncInventoryService;

    public InventoryController(InventoryClient inventoryClient, AsyncInventoryService asyncInventoryService) {
        this.inventoryClient = inventoryClient;
        this.asyncInventoryService = asyncInventoryService;
    }

    @GetMapping("/{id}/sync")
    public String checkStockSync(@PathVariable String id) {
        return inventoryClient.checkStock(id);
    }

    @GetMapping("/{id}/async")
    public CompletableFuture<String> checkStockAsync(@PathVariable String id) {
        return asyncInventoryService.checkStockAsync(id);
    }
}
