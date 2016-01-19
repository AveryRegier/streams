package com.github.averyregier.streamy.collection;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by avery on 1/15/16.
 */
public class StreamyBagTest {
//    @Test
//    public void testSize() throws Exception {
//        Collection<?> list = new StreamyBag<>(Stream.of(1,2,3));
//
//        assertEquals(3, list.size());
//    }

    @Test
    public void testIsNotEmpty() throws Exception {
        Collection<?> list = new StreamyBag<>(Stream.of(1));
        assertNotEmpty(list);
    }

    private void assertNotEmpty(Collection<?> list) {
        assertFalse(list.isEmpty());
    }

    private <T> StreamyBag<T> bagOf(Queue<T> queue, int limit) {
        Stream<T> stream = Stream.generate(queue::remove).limit(limit);
        return new StreamyBag<>(stream);
    }

    @Test
    public void testIsEmpty() throws Exception {
        StreamyBag<Object> list = new StreamyBag<>(Stream.empty());
        assertTrue(list.isEmpty());
        assertFalse(list.iterator().hasNext());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIsEmptyAfterIteration() throws Exception {
        StreamyBag<Object> list = new StreamyBag<>(Stream.empty());
        assertFalse(list.iterator().hasNext());
        assertTrue(list.isEmpty());
    }

    @Test(expected = NoSuchElementException.class)
    public void testIterator() throws Exception {
        BlockingQueue<Integer> queue = mock(BlockingQueue.class);
        StreamyBag<Integer> list = bagOf(queue, 3);
        Iterator<Integer> iterator = list.iterator();
        assertNotNull(iterator);

        assertOneIteration(list, queue, 1, iterator);

        assertOneIteration(list, queue, 2, iterator);

        assertOneIteration(list, queue, 3, iterator);

        assertFalse(iterator.hasNext());
        assertEquals(3, list.size());
        iterator.next();
    }

    private void assertOneIteration(Collection<Integer> list, BlockingQueue<Integer> queue, int value, Iterator<Integer> iterator) {
        when(queue.remove()).thenReturn(value);
        assertNext(iterator, value);

        assertNotEmpty(list);
    }

    @Test(expected = OutOfMemoryError.class)
    public void testIteratorFailure() throws Exception {
        BlockingQueue<Integer> queue = mock(BlockingQueue.class);
        StreamyBag<Integer> list = bagOf(queue, 3);
        Iterator<Integer> iterator = list.iterator();
        assertNotNull(iterator);
        when(queue.remove()).thenThrow(OutOfMemoryError.class);
        iterator.hasNext();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testContains() throws Exception {
        StreamyBag<?> bag = new StreamyBag<>(Stream.empty());
        bag.contains(new Object());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testContainsAll() throws Exception {
        StreamyBag<?> bag = new StreamyBag<>(Stream.empty());
        bag.containsAll(Collections.emptyList());
    }

    @Test
    public void testAdd() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.empty());
        assertTrue(bag.isEmpty());
        bag.add(1);
        assertNotEmpty(bag);
        assertIteration(bag, 1);
        assertNotEmpty(bag);
    }

    @Test
    public void testAddClean() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.empty());
        bag.add(1);
        assertNotEmpty(bag);
        assertIteration(bag, 1);
        assertNotEmpty(bag);
    }

    @Test
    public void testAddAll() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.empty());
        assertTrue(bag.isEmpty());
        bag.addAll(Arrays.asList(1, 2));
        assertNotEmpty(bag);
        assertIteration(bag, 1,2);
        assertNotEmpty(bag);
    }

    @Test
    public void testAddAllClean() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.empty());
        bag.addAll(Arrays.asList(1, 2));
        assertNotEmpty(bag);
        assertIteration(bag, 1,2);
    }

    @Test
    public void testRemove() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1,2,3));
        assertTrue(bag.remove(2));
        assertIteration(bag, 1,3);
    }

    @Test
    public void testRemoveNull() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1,null,3));
        assertTrue(bag.remove(null));
        assertIteration(bag, 1,3);
    }

    @Test
    public void testRemoveExhausted() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1));
        for(Integer i: bag);
        assertFalse(bag.remove(2));
    }

    @Test
    public void testRemoveAll() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1,2,3,2,4));
        assertTrue(bag.removeAll(Arrays.asList(2,4)));
        assertIteration(bag, 1,3);
    }

    private static void assertNext(Iterator<Integer> iterator, int value) {
        assertTrue(iterator.hasNext());
        assertEquals(new Integer(value), iterator.next());
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveAllNull() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1,2,3,2,4));
        bag.removeAll(null);
    }

    @Test
    public void testRetainAll() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1,2,3,2,4));
        assertTrue(bag.retainAll(Arrays.asList(2,4)));

        assertIteration(bag, 2,2,4);
    }

    @Test(expected = NullPointerException.class)
    public void testRetainAllNull() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1,2,3,2,4));
        bag.retainAll(null);
    }

    @Test
    public void testRemoveIf() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1,2,3));
        assertTrue(bag.removeIf(o->o == 2));
        assertIteration(bag, 1,3);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveIfNull() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1,2,3,2,4));
        bag.retainAll(null);
    }

    @Test
    public void testClear() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1,2,3));
        bag.clear();
        assertTrue(bag.isEmpty());
        bag.add(1);
        assertIteration(bag, 1);
    }

    private static void assertIteration(Collection<Integer> bag, int... values) {
        Iterator<Integer> iterator = bag.iterator();
        for(int i: values) {
            assertNext(iterator, i);
        }
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testToArray() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1,2,3));
        Object[] objects = bag.toArray();
        assertEquals(Object.class, objects.getClass().getComponentType());
        assertEquals(3, objects.length);
        for (int i = 0; i < objects.length; i++) {
            assertEquals(i+1, objects[i]);
        }
    }

    @Test
    public void testToArray1() throws Exception {
        StreamyBag<Integer> bag = new StreamyBag<>(Stream.of(1,2,3));
        Object[] objects = bag.toArray(new Integer[0]);
        assertEquals(Integer.class, objects.getClass().getComponentType());
        assertEquals(3, objects.length);
        for (int i = 0; i < objects.length; i++) {
            assertEquals(i+1, objects[i]);
        }
    }
}