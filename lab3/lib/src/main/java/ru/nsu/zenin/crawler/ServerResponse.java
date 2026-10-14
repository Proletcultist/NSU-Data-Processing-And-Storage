package ru.nsu.zenin.crawler;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ServerResponse(@JsonProperty(required = true) String message, @JsonProperty(required = true) List<String> successors) {}
