//usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.smallrye.reactive:smallrye-mutiny-vertx-redis-client:1.1.0
//DEPS io.smallrye.reactive:mutiny:0.7.0
//DEPS org.testcontainers:testcontainers:1.14.3
//DEPS org.slf4j:slf4j-nop:1.7.30

package com.example.jbang;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.RedisClient;
import org.testcontainers.containers.GenericContainer;

/**
 * run below before running
 * docker pull testcontainersofficial/ryuk:0.3.0
 */
public class RedisAndMutiny {

    public static void main(String[] args) {
        GenericContainer<?> container = new GenericContainer<>("redis:alpine")
                .withExposedPorts(6379);
        container.start();

        Vertx vertx = Vertx.vertx();
        RedisClient redis = RedisClient.create(vertx, new JsonObject()
                .put("port", container.getMappedPort(6379))
                .put("host", container.getContainerIpAddress()));

        JsonObject luke = new JsonObject()
                .put("name", "Luke Skywalker")
                .put("birth_year", "19BBY");

        JsonObject c3p0 = new JsonObject()
                .put("name", "C-3PO")
                .put("birth_year", "112BBY");

        JsonObject r2d2 = new JsonObject()
                .put("name", "R2-D2")
                .put("birth_year", "33BBY");

        JsonObject vader = new JsonObject()
                .put("name", "Darth Vader")
                .put("birth_year", "41.9BBY");

        Uni.combine().all().unis(
                redis.hmset("1", luke),
                redis.hmset("2", c3p0),
                redis.hmset("3", r2d2),
                redis.hmset("4", vader)
        ).asTuple()

                .await().indefinitely();

        // Item inserted

        Uni<JsonArray> result = redis.keys("*")
                .onItem().transformToMulti(keys -> Multi.createFrom().iterable(keys))
                .onItem().castTo(String.class)
                .onItem().transformToUniAndMerge(key -> redis.hgetall(key))
                .collectItems().in(() -> new JsonArray(), (arr, obj) -> arr.add(obj));

        System.out.println(result.await().indefinitely());

        vertx.closeAndAwait();
        container.stop();
    }

}
