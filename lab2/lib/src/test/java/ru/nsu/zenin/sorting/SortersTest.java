package ru.nsu.zenin.sorting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import ru.nsu.zenin.list.ConcurrentLinkedList;
import ru.nsu.zenin.sorting.ConcurrentListSorter;
import ru.nsu.zenin.sorting.SyncronizedListSorter;

class SortersTest {
    @Test
    void concurrentListSorterTest() throws Exception {
        ConcurrentLinkedList<String> li = new ConcurrentLinkedList<String>();

        li.add("d");
        li.add("abc");
        li.add("b");

        Thread sorter = new Thread(new ConcurrentListSorter(li, 0, 0));
        sorter.start();

        Thread.sleep(2000);

        Iterator<String> it = li.iterator();
        Assertions.assertEquals(it.next(), "abc");
        Assertions.assertEquals(it.next(), "b");
        Assertions.assertEquals(it.next(), "d");
        Assertions.assertFalse(it.hasNext());

        li.add("c");

        Thread.sleep(2000);

        it = li.iterator();
        Assertions.assertEquals(it.next(), "abc");
        Assertions.assertEquals(it.next(), "b");
        Assertions.assertEquals(it.next(), "c");
        Assertions.assertEquals(it.next(), "d");
        Assertions.assertFalse(it.hasNext());

        sorter.interrupt();
    }

    @Test
    void synchronizedListSorterTest() throws Exception {
        List<String> li = Collections.synchronizedList(new ArrayList<String>());

        li.add("d");
        li.add("abc");
        li.add("b");

        Thread sorter = new Thread(new SyncronizedListSorter(li, 0, 0));
        sorter.start();

        Thread.sleep(2000);

        Iterator<String> it = li.iterator();
        Assertions.assertEquals(it.next(), "abc");
        Assertions.assertEquals(it.next(), "b");
        Assertions.assertEquals(it.next(), "d");
        Assertions.assertFalse(it.hasNext());

        li.add("c");

        Thread.sleep(2000);

        it = li.iterator();
        Assertions.assertEquals(it.next(), "abc");
        Assertions.assertEquals(it.next(), "b");
        Assertions.assertEquals(it.next(), "c");
        Assertions.assertEquals(it.next(), "d");
        Assertions.assertFalse(it.hasNext());

        sorter.interrupt();
    }
}
