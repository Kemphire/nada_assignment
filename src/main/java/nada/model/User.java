package nada.model;

public record User(Integer id,String name) {
    static int nextId = 0;

    public User(String name) {
        this(nextId++,name);
    }
}
