package nada.model;

import java.util.concurrent.atomic.AtomicInteger;

public record User(Integer id, String name) {
    private static final AtomicInteger nextId = new AtomicInteger(1);

    public User(String name) {
        this(nextId.getAndIncrement(), name);
    }
}
