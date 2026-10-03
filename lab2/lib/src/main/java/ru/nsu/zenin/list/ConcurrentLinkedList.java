package ru.nsu.zenin.list;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.NoSuchElementException;

// Transactional changes:
// 1. Create an iterator
// 2. Start transaction - prev and next nodes will be locked
// 3. Iterate to next with adding it to the transaction - next next node will be locked, next unlocked and iterator propagated
// 4. Nodes inside of this locked bounds flagged, so no iterator from inside of this area can start transaction, untill current transaction ends
// 5. Do the transaction - acquire list interface to the locked part of list and do whatever you want, after that all nodes will be unlocked
public class ConcurrentLinkedList<T> implements Iterable<T> {
    private Node<T> sentinel;

    public ConcurrentLinkedList() {
        sentinel = new Node<T>();
        sentinel.setNext(sentinel);
        sentinel.setPrev(sentinel);

        Lock sentNextLock = new ReentrantLock();
        Lock sentPrevLock = new ReentrantLock();

        sentinel.setNextLinkLock(sentNextLock);
        sentinel.setPrevLinkLock(sentPrevLock);
    }

    public void add(T val) {
        Node<T> newNode = new Node<T>();
        newNode.setVal(val);

        sentinel.getPrevLinkLock().lock();

        newNode.setNext(sentinel);
        newNode.setPrev(sentinel.getPrev());

        Lock newLock = new ReentrantLock();

        newNode.setNextLinkLock(sentinel.getPrevLinkLock());
        newNode.setPrevLinkLock(newLock);

        sentinel.getPrev().setNext(newNode);
        sentinel.getPrev().setNextLinkLock(newLock);

        sentinel.setPrev(newNode);

        sentinel.getPrevLinkLock().unlock();
    }

    public TransactionalIterator<T> transactionalIterator() {
        TransactionalIterator<T> it = new TransactionalIterator<T>();
        it.setCurrent(sentinel);

        return it;
    }

    @Override
    public Iterator<T> iterator() {
        return transactionalIterator();
    }

    @Data
    private class Node<U> {
        private Node next, prev;
        private Lock nextLinkLock, prevLinkLock;
        private U val;
    }

    @Data
    public class TransactionalIterator<U> implements Iterator<U> {
        private Node<U> current;
        private List<Node<U>> transaction = new ArrayList<Node<U>>();

        @Override
        public boolean hasNext() {
            return current.getNext() != sentinel;
        }

        @Override
        public U next() {
            current = current.getNext();
            if (current == sentinel) {
                throw new NoSuchElementException();
            }

            return current.getVal();
        }

        public void addNextToTransaction() {
        }

        public void runTransaction(Consumer<ListTransaction<U>> fun) {
            // TODO: Run fun with transaction nodes passed, unlock all locks
        }
    }
}
