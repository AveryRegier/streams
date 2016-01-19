package com.github.averyregier.streams.streamyserver;

import com.github.averyregier.streamy.collection.StreamyBag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;

@RestController
public class GreetingController {

    @RequestMapping("/numbers")
    public Collection<Integer> numbers(@RequestParam(value="howMany", defaultValue="1000") int howMany) {

        Runtime runtime = Runtime.getRuntime();
        return new StreamyBag(Stream.iterate(0, n->n+1)
                .peek(i -> {
                    if(i % 100000 == 0) runtime.gc();
                    System.out.println((runtime.totalMemory()-runtime.freeMemory())/(double)runtime.totalMemory());
                })
                .limit(howMany));
    }

    private Supplier<Double> getLongSupplier(Runtime runtime) {
        return () -> (runtime.totalMemory()-runtime.freeMemory())/(double)runtime.totalMemory();
    }
}