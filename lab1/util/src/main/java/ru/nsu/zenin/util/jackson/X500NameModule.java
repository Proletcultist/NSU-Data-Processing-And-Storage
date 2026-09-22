package ru.nsu.zenin.util.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bouncycastle.asn1.x500.X500Name;

public class X500NameModule extends SimpleModule {
    public X500NameModule() {
        super("X500NameModule");
        addDeserializer(X500Name.class, new X500NameDeserializer());
    }
}
