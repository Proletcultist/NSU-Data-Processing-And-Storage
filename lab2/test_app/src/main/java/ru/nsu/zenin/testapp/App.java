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

        System.out.println("");

        for (Integer i : li) {
            System.out.println(i);
        }
    }
}
