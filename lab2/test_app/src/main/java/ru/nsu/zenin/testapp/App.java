package ru.nsu.zenin.testapp;

import ru.nsu.zenin.list.ConcurrentLinkedList;
import ru.nsu.zenin.sorting.ConcurrentListSorter;

public class App {
    public static void main(String[] args) throws Exception {
        ConcurrentLinkedList<Integer> li = new ConcurrentLinkedList<Integer>();

        li.add(3);
        li.add(2);
        li.add(1);

        for (Integer i : li) {
            System.out.println(i);
        }

        Thread sorter = new Thread(new ConcurrentListSorter<Integer>(li));
        sorter.start();

        Thread.sleep(100);

        for (Integer i : li) {
            System.out.println(i);
        }
    }
}
