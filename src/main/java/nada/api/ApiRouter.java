package nada.api;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class ApiRouter {
    public static Router create(Vertx vertx) {
        Router router = Router.router(vertx);


        return router;
    }
}
