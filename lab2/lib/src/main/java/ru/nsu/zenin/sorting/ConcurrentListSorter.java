package ru.nsu.zenin.sorting;

import lombok.RequiredArgsConstructor;
import ru.nsu.zenin.list.ConcurrentLinkedList;

@RequiredArgsConstructor
public class ConcurrentListSorter<T extends Comparable<T>> implements Runnable {
    private final ConcurrentLinkedList<T> list;

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            ConcurrentLinkedList<T>.TransactionalIterator it = list.transactionalIterator();

            while (it.hasNext()) {
                T val = it.next();
                it.addToTransaction();

                if (it.getTransactionSize() == 2) {
                    try (ConcurrentLinkedList<T>.ListTransaction trans = it.runTransaction()) {
                        if (trans.get(0).compareTo(trans.get(1)) > 0) {
                            // TODO: Use swap with delay
                            trans.swap(0, 1);
                        }

                        it = trans.transactionalIterator(0);
                        // TODO: Add optional delay
                    }
                }
            }

            // Close transaction if there is any
            it.runTransaction().close();
        }
    }
}
