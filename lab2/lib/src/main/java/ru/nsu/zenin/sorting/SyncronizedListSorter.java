package ru.nsu.zenin.sorting;

import lombok.RequiredArgsConstructor;
import java.util.NoSuchElementException;
import java.util.List;

@RequiredArgsConstructor
public class SyncronizedListSorter<T extends Comparable<T>> implements Runnable {
    private final List<T> list;
    private final long outDelay, inDelay;

    @Override
    public void run() {
        try {
            int index = 0;
            while (true) {
                synchronized (list) {
                    if (index + 1 >= list.size()) {
                        index = 0;
                        continue;
                    }

                    if (list.get(index).compareTo(list.get(index + 1)) > 0) {
                        T tmp = list.get(index);
                        list.set(index, list.get(index + 1));

                        if (inDelay > 0) {
                            Thread.sleep(inDelay);
                        }

                        list.set(index + 1, tmp);
                    }

                    index++;
                }

                Thread.sleep(outDelay);
            }
        } catch (InterruptedException ignore) {}
    }
}
