package nada.graph;

import java.util.Set;

import nada.exceptions.SameUsersException;
import nada.exceptions.UserNotFoundException;
import nada.model.ShortestPathResponse;
import nada.model.User;

public interface SocialGraphInterface {
    public User create(User user);

    public void link(User u1, User u2) throws SameUsersException;

    // public boolean knows(User u1, User u2) throws SameUsersException;
    public ShortestPathResponse shortestPath(User u1, User u2) throws SameUsersException;

    public Set<User> listContacts(User u) throws UserNotFoundException;

    public User getUser(Integer id) throws UserNotFoundException;
}
