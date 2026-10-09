package nada.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nada.exceptions.SameUsersException;
import nada.exceptions.UserNotFoundException;
import nada.model.User;

class SocialGraphTest {

    private SocialGraph graph;

    @BeforeEach
    void setup() {
        graph = new SocialGraph();
    }

    @Test
    void unknownUser_throws404() {
        var e = assertThrows(UserNotFoundException.class, () -> graph.getUser(999999));
        assertEquals(404, e.getStatusCode());
        assertThrows(UserNotFoundException.class, () -> graph.listContacts(new User(999999, "ghost")));
    }

    @Test
    void sameUser_returnsSelf() throws Exception {
        var u = graph.create(new User("Asha"));
        var res = graph.shortestPath(u, u);
        assertTrue(res.connected());
        assertEquals(0, res.hops());
        assertEquals(List.of(u), res.path());
    }

    @Test
    void linkSameUser_throws400() {
        var u = graph.create(new User("Asha"));
        var e = assertThrows(SameUsersException.class, () -> graph.link(u, u));
        assertEquals(400, e.getStatusCode());
    }

    @Test
    void hindiName_preserved() throws Exception {
        var u = graph.create(new User("आशा"));
        assertEquals("आशा", graph.getUser(u.id()).name());
    }

    @Test
    void threeHopBoundary_rawHops() throws Exception {
        var a = graph.create(new User("A"));
        var b = graph.create(new User("B"));
        var c = graph.create(new User("C"));
        var d = graph.create(new User("D"));
        var e = graph.create(new User("E"));
       

        graph.link(a, b);
        graph.link(b, c);
        graph.link(c, d);
        graph.link(d, e);

        var toD = graph.shortestPath(a, d);
        assertTrue(toD.connected());
        assertEquals(3, toD.hops());

        // Service has no 3-hop cap; the cap is enforced in the API layer.
        var toE = graph.shortestPath(a, e);
        assertTrue(toE.connected());
        assertEquals(4, toE.hops());
    }
}
