package com.example.backend.dto.request;

public record AddPlayerRequest(

        String linkedUserId,
        String displayName

) {}
