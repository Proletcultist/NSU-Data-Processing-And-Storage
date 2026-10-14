package ru.nsu.zenin.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ServerResponse(
        @JsonProperty(required = true) String message,
        @JsonProperty(required = true) List<String> successors
) {}
