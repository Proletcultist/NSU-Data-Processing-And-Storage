package ru.nsu.zenin.sorting;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import ru.nsu.zenin.list.ConcurrentLinkedList;

@RequiredArgsConstructor
public class ConcurrentListSorter<T extends Comparable<T>> implements Runnable {
    private final ConcurrentLinkedList<T> list;
    private final long outDelay, inDelay;

    @Override
    public void run() {
        try {
            while (true) {
                ConcurrentLinkedList<T>.TransactionIterator it = list.transactionIterator();

                try {
                    while (true) {
                        it.startTransaction();

                        it.next();
                        it.next();

                        try (ConcurrentLinkedList<T>.ListTransaction trans = it.runTransaction()) {
                            if (trans.get(0).compareTo(trans.get(1)) > 0) {
                                // TODO: Use swap with delay
                                trans.swap(0, 1, inDelay);
                            }

                            // Swap transactional iterator with new
                            it.close();
                            it = trans.transactionIterator(0);
                            // TODO: Add optional delay
                        }

                        if (outDelay > 0) {
                            Thread.sleep(outDelay);
                        }
                    }
                } catch (NoSuchElementException ignore) {
                } finally {
                    it.close();
                }
            }
        } catch (InterruptedException ignore) {
        }
    }
}
