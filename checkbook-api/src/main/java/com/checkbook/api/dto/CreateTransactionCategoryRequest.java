package com.checkbook.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateTransactionCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must be less than 100 characters")
    private String name;

    // Constructors
    public CreateTransactionCategoryRequest() {}

    public CreateTransactionCategoryRequest(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "CreateTransactionCategoryRequest{" +
                "name='" + name + '\'' +
                '}';
    }
}