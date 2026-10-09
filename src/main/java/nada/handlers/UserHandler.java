package nada.handlers;

import java.util.List;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import nada.exceptions.HttpExceptions;
import nada.graph.SocialGraphInterface;
import nada.model.ShortestPathResponse;
import nada.model.User;
import nada.store.NetworkManager;

public class UserHandler {
    private static final SocialGraphInterface graph = NetworkManager.getGraph();

    // 1. POST /people {"name":"Asha"} -> 201 {"id":1,"name":"Asha"}
    public void createUser(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            System.out.println("[FAIL] createUser: missing body");
            ctx.fail(new HttpExceptions("Name is required", 400));
            return;
        }
        var name = body.getString("name");
        if (name == null || name.isBlank()) {
            System.out.println("[FAIL] createUser: missing/blank name body=" + body.encode());
            ctx.fail(new HttpExceptions("Name is required", 400));
            return;
        }
        var user = graph.create(new User(name.strip()));
        ctx.response().setStatusCode(201)
                .putHeader("content-type", "application/json")
                .end(JsonObject.mapFrom(user).encode());
    }

    // 2. POST /knows {"a":1,"b":2} -> 204, mutual + idempotent
    public void linkUsers(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            if (body == null || body.getValue("a") == null || body.getValue("b") == null) {
                System.out.println("[FAIL] linkUsers: missing a/b body=" + (body == null ? "null" : body.encode()));
                ctx.fail(new HttpExceptions("Fields 'a' and 'b' are required", 400));
                return;
            }
            Integer a = toInt(body.getValue("a"), "'a' must be an integer");
            Integer b = toInt(body.getValue("b"), "'b' must be an integer");
            // Service layer raises annotated errors: 404 if unknown, 400 if same user.
            var u1 = graph.getUser(a);
            var u2 = graph.getUser(b);
            graph.link(u1, u2);
            ctx.response().setStatusCode(204).end();
        } catch (HttpExceptions e) {
            System.out.println("[FAIL] linkUsers: " + e.getStatusCode() + " " + e.getMessage());
            ctx.fail(e);
        }
    }

    // 3. GET /people/:id/contacts -> 200 [...] or []
    public void listContacts(RoutingContext ctx) {
        try {
            Integer id = toInt(ctx.pathParam("id"), "Invalid id");
            var user = graph.getUser(id);
            ctx.json(graph.listContacts(user));
        } catch (HttpExceptions e) {
            System.out.println(
                    "[FAIL] listContacts id=" + ctx.pathParam("id") + ": " + e.getStatusCode() + " " + e.getMessage());
            ctx.fail(e);
        }
    }

    // 4. GET /path?from=1&to=5 -> 200, only paths of <= 3 hops
    public void shortestPath(RoutingContext ctx) {
        try {
            var fromParam = ctx.queryParam("from");
            var toParam = ctx.queryParam("to");
            if (fromParam.isEmpty() || toParam.isEmpty()) {
                System.out.println("[FAIL] shortestPath: missing from/to");
                ctx.fail(new HttpExceptions("Query params 'from' and 'to' are required", 400));
                return;
            }

            Integer from = toInt(fromParam.getFirst(), "'from' must be an integer");
            Integer to = toInt(toParam.getFirst(), "'to' must be an integer");

            var u1 = graph.getUser(from);
            var u2 = graph.getUser(to);

            ShortestPathResponse result = graph.shortestPath(u1, u2);

            if (!result.connected() || result.hops() > 3) {
                System.out.println("[FAIL] shortestPath: no qualifying path from=" + from + " to=" + to
                        + " connected=" + result.connected() + " hops=" + result.hops());
                ctx.json(new JsonObject().put("connected", false));
                return;
            }
            ctx.json(JsonObject.mapFrom(result));
        } catch (Exception e) {
            System.out.println("[FAIL] shortestPath: " + e.getClass().getSimpleName() + " " + e.getMessage());
            ctx.fail(e);
        }
    }

    private static Integer toInt(Object value, String message) throws HttpExceptions {
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException e) {
                System.out.println("[FAIL] toInt: invalid integer value=" + value);
                throw new HttpExceptions(message, 400);
            }
        }
        System.out.println("[FAIL] toInt: invalid integer value=" + value);
        throw new HttpExceptions(message, 400);
    }
}
