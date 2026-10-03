package ru.nsu.zenin.list;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.NoSuchElementException;

public class ConcurrentLinkedList<T> implements Iterable<T> {
    private Node sentinel;

    public ConcurrentLinkedList() {
        sentinel = new Node();
        sentinel.setNext(sentinel);
        sentinel.setPrev(sentinel);

        ReadWriteLock sentNextLock = new ReentrantReadWriteLock();
        ReadWriteLock sentPrevLock = new ReentrantReadWriteLock();

        sentinel.setNextLinkLock(sentNextLock);
        sentinel.setPrevLinkLock(sentPrevLock);
    }

    public void add(T val) {
        Node newNode = new Node();
        newNode.setVal(val);

        sentinel.getPrevLinkLock().writeLock().lock();

        newNode.setNext(sentinel);
        newNode.setPrev(sentinel.getPrev());

        ReadWriteLock newLock = new ReentrantReadWriteLock();

        newNode.setNextLinkLock(sentinel.getPrevLinkLock());
        newNode.setPrevLinkLock(newLock);

        sentinel.getPrev().setNext(newNode);
        sentinel.getPrev().setNextLinkLock(newLock);

        sentinel.setPrev(newNode);

        sentinel.getPrevLinkLock().writeLock().unlock();
    }

    public TransactionalIterator transactionalIterator() {
        return new TransactionalIterator();
    }

    @Override
    public Iterator<T> iterator() {
        List<T> snapshot = new ArrayList<T>();

        try (TransactionalIterator it = transactionalIterator()) {
            while (it.hasNext()) {
                snapshot.add(it.next());
            }
        }

        return snapshot.iterator();
    }

    @Data
    private class Node {
        private Node next, prev;
        private ReadWriteLock nextLinkLock, prevLinkLock;
        private T val;
    }

    @Data
    public class TransactionalIterator implements Iterator<T>, AutoCloseable {
        private Node current = sentinel;
        private List<Node> transaction = new ArrayList<Node>();
        private boolean closed = false;

        private TransactionalIterator() {
            current.getNextLinkLock().readLock().lock();

            // If there is no nodes in list - close iterator
            if (current.getNext() == sentinel) {
                close();
            }
        }

        @Override
        public boolean hasNext() {
            return !closed;
        }

        @Override
        public T next() {
            if (closed) {
                throw new NoSuchElementException();
            }

            Node next = current.getNext();

            // Lock NextLinkLock of the next node and unlock this lock if current node
            next.getNextLinkLock().readLock().lock();
            current.getNextLinkLock().readLock().unlock();

            // Proceed
            current = next;
            next = current.getNext();

            // If iterator reached the end - unlock the lock (iterator is invalidated by (next == sentinel) anyway)
            if (next == sentinel) {
                close();
            }

            return current.getVal();
        }

        @Override
        public void close() {
            if (!closed) {
                current.getNextLinkLock().readLock().unlock();
                closed = true;
            }
        }

        // Adds current node to transaction (locks both of its' links)
        // calls next() after that
        public void addToTransaction() {
        }

        // Runs a fun with current transaction
        // Places Iterator on the last node of transaction after fun (which possibly swaps nodes)
        // Unlock all locks
        public void runTransaction(Consumer<ListTransaction<T>> fun) {
            // TODO: Run fun with transaction nodes passed, unlock all locks
        }
    }
}
