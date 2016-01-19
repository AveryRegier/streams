package com.github.averyregier.streamy.collection;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This collection is meant to be a facade for a Stream, in places where you need a collection interface, but
 * don't want to pull the contents of the stream into memory yet.  Its primary use case is to pass a stream of
 * objects from a backend store and supply them to a serialization or unmarshalling framework such as JAXB.
 * Extremely large result sets are thus never created in memory all at once.
 *
 * This collection supports a single major operation of iterating over the contents.  So you can do a single
 * iterator(), stream(), forEach(), or toArray(), and get all the contents of the stream, but once you do the contents
 * are not 'saved' for later use or iterations.
 *
 * The behavior of this collection when using major operations in parallel within different threads is undefined,
 * except for stream().parallel().  The incoming stream may be a parallel stream without issues.  This collection
 * is not a queue and does not guarantee queue semantics.
 *
 * Collection modification methods such as removeIf(), remove(), add(), addAll(), retainAll() are supported, but they
 * will not actively iterate the collection and change the contents of it.  Instead, removal operations will add a
 * filter to the underlying stream, and add*() methods will concatenate the contents to the current stream.  Thus, if
 * you remove an object it will be suppressed from what is on the current stream, but if you add more of the same
 * object then those will not be suppressed.  This preserves the normal semantics of Collection modification, but
 * when the methods return 'true', they really mean 'There is a possibility this will modify the collection in the
 * future", and 'false' means 'There is no way this operation could modify the contents of this collection'.
 * Basically, you can use these methods normally and you shouldn't be surprised by the results.
 *
 * For a collection such as this, what should size() return?  For the primary use case, you usually need to report the
 * total number of objects that were returned to the client via a major operation.  However, the memory efficiency of
 * this collection can be greatly influenced by when you make the call to size().  If you do so before a major
 * operation, then we are forced to work through the stream early and keep the contents so that we can still guarantee
 * a major operation returning all of the values. Future versions of this collection may need to take a size() strategy
 * to help with making wise decisions.  Other options would be always returning zero, since there is actually nothing
 * ever in the collection itself, or just return the number of items returned thus far.
 *
 * Search operations, contains() and containsAll() are not supported as they would force either being destructive in
 * order to complete the search, and thus sacrifice being able to do those searches in any order, or load all the
 * contents into memory, in which case, it would be better for you to just use a different collection.
 *
 * Using toArray() with a collection such as this is a bad idea, as it forces all the contents to be in memory at once.
 * However, toArray() is required for copy constructors to work since they don't use streams or iteration due to
 * backwards compatibility concerns.  For this reason, toArray() is supported, but it just collects the remaining
 * results into an ArrayList and uses that get an array.  Note that two large arrays end up getting created, which
 * could result in OutOfMemory errors just from not being able to allocate both large arrays.  Since we can't tell
 * up-front how big an array is needed its really hard to do better than this.
 */
public class StreamyBag<T> implements Collection<T> {
    private Stream<? extends T> stream;
    private boolean knownNotEmpty = false;
    private int count = 0;
    private Iterator<? extends T> child = null;

    public StreamyBag(Stream<T> stream) {
        replaceStream(stream);
    }

    private void replaceStream(Stream<? extends T> stream) {
        this.stream = stream.onClose(()->{
            if(this.stream == stream) this.stream = null;
        });
    }

    /**
     * If you want the full benefits of this collection, don't use size until you have consumed the contents.
     * This is very slow and will not have the memory benefits you want otherwise.
     * @return The total number of items that have been added (excluding removals) to this collection since
     * construction or the last clear() operation.
     */
    public int size() {
//        if(stream == null)
            return count;

//        List<T> collected = stream().collect(Collectors.toList());
//        replaceStream(collected.stream());
//        return count + collected.size();
    }

    public boolean isEmpty() {
        return !(knownNotEmpty || stream != null && getChildIterator().hasNext());
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("This bag does not support search operations");
    }

    public Iterator<T> iterator() {
        if(stream == null) throw new IllegalStateException("Only supports a single iteration");
        Iterator<? extends T> child = getChildIterator();
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                boolean hasNext = child.hasNext();
                if(hasNext) knownNotEmpty = true;
                else stream.close();
                return hasNext;
            }

            @Override
            public T next() {
                T next = child.next();
                count++;
                return next;
            }
        };
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return stream().collect(Collectors.toList()).toArray(a);
    }

    @Override
    public synchronized boolean add(T t) {
        return addStream(Stream.of(t));
    }

    private boolean addStream(Stream<? extends T> s) {
        if(stream == null) {
            replaceStream(s);
        } else if(child != null) {
            replaceStream(Stream.concat(asStream(child), s));
            child = stream.iterator();
        } else {
            replaceStream(Stream.concat(stream, s));
        }
        return true;
    }

    /**
     * This method will ensure that if the given object is encountered on the stream after this point,
     * it will be filtered out, unless the stream is exhausted already.
     * If you add additional items after the removal that match, they will not be removed.
     * @param toRemove an object to filter out of future results.  Nulls are supported.
     * @return true if there are elements left in the stream, false if the stream is exhausted at this time.
     */
    @Override
    public boolean remove(Object toRemove) {
        if(stream == null) return false;
        replaceStream(stream.filter(o->
                o != toRemove &&
                        (o == null ||
                         toRemove == null ||
                         o.hashCode() != toRemove.hashCode() ||
                         !o.equals(toRemove))));
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("This bag does not support search operations");
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return addStream(c.stream());
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return retainIf(o -> !c.contains(o));
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return retainIf(c::contains);
    }

    private boolean retainIf(Predicate<? super T> predicate) {
        if(stream == null) return false;
        replaceStream(stream.filter(predicate));
        return true;
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        Objects.requireNonNull(filter);
        return retainIf(filter.negate());
    }

    @Override
    public synchronized void clear() {
        stream = null;
        child = null;
        knownNotEmpty = false;
        count = 0;
    }

    @Override
    public Stream<T> stream() {
        if(stream == null) return Stream.empty();
        return stream
                .map(a->(T)a)
                .peek((o)->{
                    knownNotEmpty = true;
                    count++;
                });
    }

    private synchronized Iterator<? extends T> getChildIterator() {
        if(child == null) {
            child = stream.iterator();
        }
        return child;
    }

    private static <T> Stream<T> asStream(Iterator<T> sourceIterator) {
        return asStream(sourceIterator, false);
    }

    private static <T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
    }

}
