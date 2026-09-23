package ru.nsu.zenin.keygen.server;

import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.time.Period;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.asn1.x500.X500Name;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
class CAServerConfig {
    private X500Name name;
    private InetSocketAddress endpoint;
    private Period certLifetime;
    private Path privateKeyFile;
    private Integer workerThreads;
}
