package com.abhishek.portfolio.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
    @NotBlank(message = "Question cannot be blank")
    String question
) {}
