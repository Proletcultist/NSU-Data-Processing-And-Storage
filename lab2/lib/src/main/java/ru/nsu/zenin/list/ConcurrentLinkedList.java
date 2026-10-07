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
    private final Node frontSentinel;
    private Node backSentinel;

    public ConcurrentLinkedList() {
        backSentinel = new Node(new ReentrantReadWriteLock());
        frontSentinel = new Node(new ReentrantReadWriteLock());

        frontSentinel.setNext(backSentinel);
        frontSentinel.setPrev(backSentinel);
        backSentinel.setNext(frontSentinel);
        backSentinel.setPrev(frontSentinel);
    }

    public void add(T val) {
        Node newNode = new Node(new ReentrantReadWriteLock());

        frontSentinel.getNodeLock().writeLock().lock();
        backSentinel.getNodeLock().writeLock().lock();

        // Add new node between backSentinel and frontSentinel
        newNode.setNext(frontSentinel);
        newNode.setPrev(backSentinel);

        backSentinel.setNext(newNode);
        frontSentinel.setPrev(newNode);

        // Now newNode is backSentinel
        backSentinel.setVal(val);
        Node oldBackSentinel = backSentinel;
        backSentinel = newNode;

        oldBackSentinel.getNodeLock().writeLock().unlock();
        frontSentinel.getNodeLock().writeLock().unlock();
    }

    public TransactionBuilder transactionBuilder() {
        return new TransactionBuilder();
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
        private final ReadWriteLock nodeLock;
        private T val;
    }

    // Iterator for consistent reading, provides a view on some valid state of the list
    public class ReadIterator implements Iterator<T>, AutoCloseable {
        private Node current;
        private boolean closed = false;

        private ReadIterator() {
            this(frontSentinel);
        }

        private ReadIterator(Node current) {
            this.current = current;

            current.getNodeLock().readLock().lock();
        }

        @Override
        public boolean hasNext() {
            return !closed && current.getNext() != backSentinel;
        }

        @Override
        public T next() {
            Node next = current.getNext();

            if (closed || next == backSentinel) {
                throw new NoSuchElementException("No next value available");
            }

            next.getNodeLock().readLock().lock();
            current.getNodeLock().readLock().unlock();

            current = next;
            return current.getVal();
        }

        @Override
        public void close() {
            if (!closed) {
                current.getNodeLock().readLock().unlock();
                closed = true;
            }
        }
    }

    // Iterator-like object for transactions building, provides facility for consistent changes to some part of the list
    public class TransactionBuilder implements AutoCloseable {
        private Node current;
        private ListTransaction transaction = new ListTransaction();
        private boolean buildingTrans = false;

        private TransactionBuilder() {
            this(frontSentinel);
        }

        private TransactionBuilder(Node current) {
            this.current = current;
        }

        @Override
        public void close() {
            transaction.close();
        }

        public T next() {
            Node next;
            if (buildingTrans) {
                next = transaction.addNextToTransaction(current);
            } else {
                next = current.getNext();
            }

            if (next == backSentinel) {
                throw new NoSuchElementException("No next value available");
            }

            current = next;
            return current.getVal();
        }

        public int getTransactionSize() {
            return transaction.size();
        }

        // After that method call 
        // all elements returned by subsequent next() calls will be added to the transaction
        public void startTransaction() {
            buildingTrans = true;
        }

        // End building of transaction
        // Gives ownership over transaction built
        public ListTransaction runTransaction() {
            buildingTrans = false;

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

        public Node addNextToTransaction(Node prev) {
            if (closed) {
                throw new IllegalStateException("Cannot add to closed transaction");
            }

            Node node;
            if (last == null) {
                prev.getNodeLock().writeLock().lock();

                node = prev.getNext();
                if (node == backSentinel) {
                    prev.getNodeLock().writeLock().unlock();
                    throw new NoSuchElementException("No next value available");
                }

                node.getNodeLock().writeLock().lock();
                node.getNext().getNodeLock().writeLock().lock();

                first = node;
                last = node;
            } else if(prev == last) {
                node = prev.getNext();
                if (node == backSentinel) {
                    throw new NoSuchElementException("No next value available");
                }

                node.getNext().getNodeLock().writeLock().lock();

                last = node;
            } else {
                throw new IllegalArgumentException("Cannot add non-consecutive element to transaction");
            }

            size++;
            return node;
        }

        @Override
        public void close() {
            if (!closed) {
                Node cursor;
                for (cursor = last; cursor != first; cursor = cursor.getPrev()) {
                    cursor.getNext().getNodeLock().writeLock().unlock();
                }
                if (cursor != null) {
                    cursor.getNext().getNodeLock().writeLock().unlock();
                    cursor.getNodeLock().writeLock().unlock();
                    cursor.getPrev().getNodeLock().writeLock().unlock();
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
            } else {
                fstNode.setPrev(sndNodePrev);
                fstNode.setNext(sndNodeNext);
                sndNode.setPrev(fstNodePrev);
                sndNode.setNext(fstNodeNext);

                fstNodePrev.setNext(sndNode);
                fstNodeNext.setPrev(sndNode);
                sndNodePrev.setNext(fstNode);
                sndNodeNext.setPrev(fstNode);
            }

            if (first == fstNode) {
                first = sndNode;
            }
            if (last == sndNode) {
                last = fstNode;
            }
        }

        public TransactionBuilder transactionBuilder(int index) {
            return new TransactionBuilder(getNode(index));
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
