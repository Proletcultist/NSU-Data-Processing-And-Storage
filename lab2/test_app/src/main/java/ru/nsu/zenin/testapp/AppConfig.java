package ru.nsu.zenin.testapp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
class AppConfig {
    private Integer workerThreads;
    private Long inDelay, outDelay;
    private SorterImplementation implementation;
}
