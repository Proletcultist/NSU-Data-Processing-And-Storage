package ru.nsu.zenin.keygen.server;

import java.time.Period;

record CAServerConfig(String name, String endpoint, Period certLifetime, String privateKeyFile) {}
