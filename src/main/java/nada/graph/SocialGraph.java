package nada.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nada.exceptions.SameUsersException;
import nada.exceptions.UserNotFoundException;
import nada.model.ShortestPathResponse;
import nada.model.User;

public class SocialGraph implements SocialGraphInterface {

    private final ConcurrentHashMap<User, Set<User>> graph = new ConcurrentHashMap<>();

    @Override
    public User create(User user) {
        graph.computeIfAbsent(user, u -> ConcurrentHashMap.newKeySet());
        return user;
    }

    // @Override
    // public boolean knows(User u1, User u2) throws SameUsersException {
    //     if (u1 == u2)
    //         throw new SameUsersException("User " + u1 + "and User " + u2 + "are same");

    //     var neighours = graph.get(u1);

    //     if (neighours == null)
    //         return false;

    //     return neighours.contains(u2);

    // }

    @Override
    public void link(User u1, User u2) throws SameUsersException {
        if (u1.equals(u2)) {
            System.out.println("[FAIL] link: same user " + u1 + " and " + u2);
            throw new SameUsersException("User " + u1 + "and User " + u2 + "are same");
        }
        graph.computeIfAbsent(u1, k -> ConcurrentHashMap.newKeySet()).add(u2);
    }

    @Override
    public ShortestPathResponse shortestPath(User u1, User u2) throws SameUsersException {
        if (u1.equals(u2)) {
            System.out.println("[FAIL] shortestPath: same user " + u1 + " and " + u2);
            throw new SameUsersException("User " + u1 + "and User " + u2 + "are same");
        }

        Queue<User> queue = new ArrayDeque<>(List.of(u1));
        Set<User> visited = new HashSet<>(Set.of(u1));
        Map<User, User> parent = new HashMap<>();
        parent.put(u1, null);

        while (!queue.isEmpty()) {
            var curr = queue.poll();

            if (curr.equals(u2))
                break;

            var neighours = graph.getOrDefault(curr, Set.of());

            for (User nxt : neighours) {
                if (visited.add(nxt)) {
                    parent.put(nxt, curr);
                    queue.add(nxt);
                }
            }
        }

        if (!parent.containsKey(u2)) {
            System.out.println("[FAIL] shortestPath: no path from " + u1 + " to " + u2);
            return new ShortestPathResponse(false, 0, List.of());
        }

        var path = new ArrayList<User>();

        var curr = u2;

        while (curr != null) {
            path.add(curr);
            curr = parent.get(curr);
        }
        
        java.util.Collections.reverse(path);
        return new ShortestPathResponse(true, path.size() - 1, List.copyOf(path));
    }

    @Override
    public Set<User> listContacts(User u) throws UserNotFoundException {
        var contacts = graph.get(u);
        if (contacts == null) {
            System.out.println("[FAIL] listContacts: user not found " + u);
            throw new UserNotFoundException("User " + u + "Not Found");
        }
        return contacts;
    }

    @Override
    public User getUser(Integer id) throws UserNotFoundException {
        var user = graph.entrySet()
                .stream()
                .filter(entry -> entry.getKey().id().equals(id))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (user == null) {
            System.out.println("[FAIL] getUser: id not found " + id);
            throw new UserNotFoundException("User with id = " + id + "Not Found");
        }
        return user;

    }

}
