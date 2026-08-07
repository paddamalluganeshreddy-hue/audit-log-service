package com.example.audit.api;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuditWriteRequest(
        @NotBlank @Size(max = 128) String eventType,
        @NotBlank @Size(max = 128) String actorId,
        @NotBlank @Size(max = 128) String resourceType,
        @NotBlank @Size(max = 256) String resourceId,
        @NotNull JsonNode payload) { }
