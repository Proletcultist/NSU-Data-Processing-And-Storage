package ru.nsu.zenin.testapp;

import ru.nsu.zenin.list.ConcurrentLinkedList;

public class App {
    public static void main(String[] args) {
        ConcurrentLinkedList<Integer> li = new ConcurrentLinkedList<Integer>();

        li.add(1);
        li.add(2);

        for (Integer i : li) {
            System.out.println(i);
            if (i.equals(1)) {
                li.add(3);
            }
        }

        ConcurrentLinkedList.TransactionalIterator it = li.transactionalIterator();

        it.next();
        it.addToTransaction();
        it.next();
        it.addToTransaction();
        it.next();
        it.addToTransaction();

        System.out.println("");

        try (ConcurrentLinkedList.ListTransaction trans = it.runTransaction()) {
            trans.swap(1, 0);
        }

        System.out.println("");

        for (Integer i : li) {
            System.out.println(i);
        }
    }
}
