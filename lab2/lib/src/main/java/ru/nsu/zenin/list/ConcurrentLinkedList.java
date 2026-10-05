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
        private Node current, next;
        private ListTransaction transaction = new ListTransaction();

        private TransactionalIterator() {
            this(sentinel);
        }

        private TransactionalIterator(Node current) {
            this.current = current;
            this.next = current.getNext();
        }

        @Override
        public boolean hasNext() {
            return next != sentinel;
        }

        @Override
        public T next() {
            if (next == sentinel) {
                throw new NoSuchElementException("No next value available");
            }

            current = next;
            next = current.getNext();

            return current.getVal();
        }

        public int getTransactionSize() {
            return transaction.size();
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

    // Continious part of the list with mutually exclusive access for transaction owner
    public class ListTransaction implements AutoCloseable {
        private Node first = null, last = null;
        private int size = 0;
        private boolean closed = false;

        void addToTransaction(Node node) {
            if (closed) {
                throw new IllegalStateException("Cannot add to closed transaction");
            }

            if (last == null) {
                node.getPrevLinkLock().writeLock().lock();
                node.getNextLinkLock().writeLock().lock();

                first = node;
                last = node;
            } else if(node == last.getNext()) {
                node.getNextLinkLock().writeLock().lock();

                last = node;
            } else {
                throw new IllegalArgumentException("Cannot add non-consecutive element to transaction");
            }

            size++;
        }

        @Override
        public void close() {
            if (!closed) {
                Node cursor;
                for (cursor = first; cursor != last; cursor = cursor.getNext()) {
                    cursor.getPrevLinkLock().writeLock().unlock();
                }
                if (cursor != null) {
                    cursor.getPrevLinkLock().writeLock().unlock();
                    cursor.getNextLinkLock().writeLock().unlock();
                }

                closed = true;
            }
        }

        public T get(int index) {
            return getNode(index).getVal();
        }

        public int size() {
            return size;
        }
        
        public void swap(int fst, int snd) {
            if (fst == snd) {
                return;
            } else if (snd < fst) {
                int tmp = fst;
                fst = snd;
                snd = tmp;
            }

            Node fstNode = getNode(fst);
            Node sndNode = getNode(snd);

            Node fstNodePrev = fstNode.getPrev();
            Node fstNodeNext = fstNode.getNext();

            Node sndNodePrev = sndNode.getPrev();
            Node sndNodeNext = sndNode.getNext();

            if (fstNodeNext == sndNode) {
                fstNode.setPrev(sndNode);
                fstNode.setNext(sndNodeNext);
                sndNode.setPrev(fstNodePrev);
                sndNode.setNext(fstNode);

                fstNodePrev.setNext(sndNode);
                sndNodeNext.setPrev(fstNode);

                fstNode.setPrevLinkLock(fstNode.getNextLinkLock());
                fstNode.setNextLinkLock(sndNodeNext.getPrevLinkLock());
                sndNode.setNextLinkLock(sndNode.getPrevLinkLock());
                sndNode.setPrevLinkLock(fstNodePrev.getNextLinkLock());
            } else {
                fstNode.setPrev(sndNodePrev);
                fstNode.setNext(sndNodeNext);
                sndNode.setPrev(fstNodePrev);
                sndNode.setNext(fstNodeNext);

                fstNodePrev.setNext(sndNode);
                fstNodeNext.setPrev(sndNode);
                sndNodePrev.setNext(fstNode);
                sndNodeNext.setPrev(fstNode);

                fstNode.setPrevLinkLock(sndNodePrev.getNextLinkLock());
                fstNode.setNextLinkLock(sndNodeNext.getPrevLinkLock());
                sndNode.setPrevLinkLock(fstNodePrev.getNextLinkLock());
                sndNode.setNextLinkLock(fstNodeNext.getPrevLinkLock());
            }

            if (first == fstNode) {
                first = sndNode;
            }
            if (last == sndNode) {
                last = fstNode;
            }
        }

        public TransactionalIterator transactionalIterator(int index) {
            return new TransactionalIterator(getNode(index));
        }

        public ReadIterator readIterator(int index) {
            return new ReadIterator(getNode(index));
        }

        private Node getNode(int index) {
            if (closed) {
                throw new IllegalStateException("Cannot get from closed transaction");
            }

            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index out of bounds");
            }

            Node cursor = first;
            while (index-- > 0) {
                cursor = cursor.getNext();
            }

            return cursor;
        }

    }
}
