package com.github.averyregier.streams.streamyserver;

import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * Created by avery on 1/18/16.
 */
public class Client {

    public static void main(String... args) throws ExecutionException, InterruptedException {
        AsyncRestTemplate restTemplate = new AsyncRestTemplate();
        ListenableFuture<ResponseEntity<Collection>> future = restTemplate.getForEntity(
                "http://localhost:8080/numbers?howMany=1000000",
                Collection.class);
        future.addCallback(new ListenableFutureCallback<ResponseEntity<Collection>>() {
            @Override
            public void onFailure(Throwable ex) {

            }

            @Override
            public void onSuccess(ResponseEntity<Collection> result) {

            }
        });
        ResponseEntity<Collection> entity = future.get();
        Collection body = entity.getBody();
        body.forEach(System.out::println);
    }

}
