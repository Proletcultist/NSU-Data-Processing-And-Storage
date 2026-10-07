package ru.nsu.zenin.testapp;

import ru.nsu.zenin.testapp.exception.UnknownSorterImplementationException;

enum SorterImplementation {
    CONCURRENT_LIST,
    SYNCHRONIZED_LIST;

    public static SorterImplementation fromString(String str) throws UnknownSorterImplementationException {
        return switch (str) {
            case "concurrent" -> SorterImplementation.CONCURRENT_LIST;
            case "synchronized" -> SorterImplementation.SYNCHRONIZED_LIST;
            default -> throw new UnknownSorterImplementationException("Unknown sorter implementation: " + str);
        };
    }
}
