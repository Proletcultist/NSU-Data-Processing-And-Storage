package ru.nsu.zenin.util.parser;

import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InetSocketAddressParser {
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("([^:]+):(\\d+)");

    private InetSocketAddressParser() {}

    public static InetSocketAddress parse(String str) throws IllegalArgumentException {
        Matcher m = ADDRESS_PATTERN.matcher(str);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid format of ip + port string");
        }

        try {
            int port = Integer.parseInt(m.group(2));
            return new InetSocketAddress(m.group(1), port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port string");
        }
    }
}
