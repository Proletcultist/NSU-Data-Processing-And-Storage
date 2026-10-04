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

    public ReadIterator readIterator() {
        return new ReadIterator();
    }

    @Override
    public Iterator<T> iterator() {
        List<T> snapshot = new ArrayList<T>();

        try (ReadIterator it = readIterator()) {
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

    // Iterator for consistent reading, provides a view on some valid state of the list
    public class ReadIterator implements Iterator<T>, AutoCloseable {
        private Node current;
        private boolean closed = false;

        private ReadIterator() {
            this(sentinel);
        }

        private ReadIterator(Node current) {
            this.current = current;

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
                throw new NoSuchElementException("No next value available");
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
    }

    // Iterator for transactions building, provides facility for consistent changes to some part of list
    public class TransactionalIterator implements Iterator<T> {
        private Node current;
        private ListTransaction transaction = new ListTransaction();

        private TransactionalIterator() {
            this(sentinel);
        }

        private TransactionalIterator(Node current) {
            this.current = current;
        }

        @Override
        public boolean hasNext() {
            return current.getNext() != sentinel;
        }

        @Override
        public T next() {
            current = current.getNext();
            if (current == sentinel) {
                throw new NoSuchElementException();
            }

            return current.getVal();
        }

        // Adds node with last returned element to transaction
        public void addToTransaction() {
            if (current == sentinel) {
                throw new IllegalStateException("Cannot add to transaction before next() method called");
            }

            transaction.addToTransaction(current);
        }

        // Gives ownership over transaction built with this iterator
        public ListTransaction runTransaction() {
            ListTransaction ret = transaction;
            transaction = new ListTransaction();

            return ret;
        }
    }

    // TODO: Implement as non-circular double-linked list, not array list
    // Continious part of the list with mutually exclusive access for transaction owner
    public class ListTransaction implements AutoCloseable {
        private List<Node> nodes = new ArrayList<Node>();

        void addToTransaction(Node node) {
            if (nodes.isEmpty()) {
                node.getPrevLinkLock().writeLock().lock();
                node.getNextLinkLock().writeLock().lock();

                nodes.add(node);
            } else if(node == nodes.get(nodes.size() - 1).getNext()) {
                node.getNextLinkLock().writeLock().lock();

                nodes.add(node);
            } else {
                throw new IllegalStateException("Cannot add non-consecutive element to transaction");
            }
        }

        // Unlock all nodes
        @Override
        public void close() {
        }

        public T get(int index) {
            Node node = nodes.get(index);
            return node == null ? null : node.getVal();
        }
        
        public void swap(int fst, int snd) {
        }

        public TransactionalIterator transactionalIterator(int index) {
            return new TransactionalIterator(nodes.get(index));
        }

        public ReadIterator readIterator(int index) {
            return new ReadIterator(nodes.get(index));
        }

    }
}
