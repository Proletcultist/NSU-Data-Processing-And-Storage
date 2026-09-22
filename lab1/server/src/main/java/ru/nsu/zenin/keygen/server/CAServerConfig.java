package ru.nsu.zenin.keygen.server;

import java.time.Period;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
class CAServerConfig {
    private String name;
    private String endpoint;
    private Period certLifetime;
    private String privateKeyFile;
    private Integer workerThreads;
}
