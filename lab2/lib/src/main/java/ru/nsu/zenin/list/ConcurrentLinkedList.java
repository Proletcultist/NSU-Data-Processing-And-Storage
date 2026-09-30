package ru.nsu.zenin.list;

import lombok.Data;

// Transactional changes:
// 1. Create an iterator
// 2. Start transaction - prev and next nodes will be locked
// 3. Iterate to next with adding it to the transaction - next next node will be locked, next unlocked and iterator propagated
// 4. Nodes inside of this locked bounds flagged, so no iterator from inside of this area can start transaction, untill current transaction ends
// 5. Do the transaction - acquire list interface to the locked part of list and do whatever you want, after that all nodes will be unlocked
public class ConcurrentLinkedList<T> {
    private Node first, last;

    public ConcurrentLinkedList() {
        Node sentinel = new Node();
        sentinel.setNext(sentinel);
        sentinel.setPrev(sentinel);

        first = sentinel;
        last = sentinel;
    }

    public void add

    @Data
    private class Node {
        Node next, prev;
        T val;
    }
}
