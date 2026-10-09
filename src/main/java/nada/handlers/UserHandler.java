package nada.handlers;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import nada.graph.SocialGraphInterface;
import nada.model.User;
import nada.store.NetworkManager;

public class UserHandler {
    public static SocialGraphInterface graph = NetworkManager.getGraph();

    public User createUser(RoutingContext ctx) {
        var name = ctx.body().asJsonObject().getString("name");

        if (name == null || name.isEmpty())
            throw new HttpException(409, "Name is required");
        return graph.create(new User(name));
    }
}
