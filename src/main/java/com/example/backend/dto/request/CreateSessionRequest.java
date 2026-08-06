package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description

) {}