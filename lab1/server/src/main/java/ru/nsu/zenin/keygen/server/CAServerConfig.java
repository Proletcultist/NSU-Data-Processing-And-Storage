package ru.nsu.zenin.keygen.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Period;

record CAServerConfig(
    @JsonProperty(required = true) String name,
    @JsonProperty(required = true) String endpoint,
    @JsonProperty(required = true) Period certLifetime,
    @JsonProperty(required = true) String privateKeyFile) {}
