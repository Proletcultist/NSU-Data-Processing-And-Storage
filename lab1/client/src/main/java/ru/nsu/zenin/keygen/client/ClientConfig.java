package ru.nsu.zenin.keygen.client;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.asn1.x500.X500Name;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
class ClientConfig {
    private InetSocketAddress endpoint;
    private X500Name name;
    private Long delay;
    private Boolean fail;
    private String outputName;
    private Boolean merge;
    private Path outputDir;
}
