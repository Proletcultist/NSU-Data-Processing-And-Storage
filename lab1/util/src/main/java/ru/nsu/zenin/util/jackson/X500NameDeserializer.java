package ru.nsu.zenin.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.bouncycastle.asn1.x500.X500Name;

class X500NameDeserializer extends JsonDeserializer<X500Name> {
    @Override
    public X500Name deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String name = jp.getCodec().readValue(jp, String.class);

        try {
            return new X500Name(name);
        } catch (IllegalArgumentException e) {
            throw ctxt.weirdStringException(name, X500Name.class, e.getMessage());
        }
    }
}
