package nada.api;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import nada.exceptions.HttpExceptions;
import nada.handlers.UserHandler;

public class ApiRouter {
    public static Router create(Vertx vertx) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        var handler = new UserHandler();
        router.post("/people").handler(handler::createUser);
        router.post("/knows").handler(handler::linkUsers);
        router.get("/people/:id/contacts").handler(handler::listContacts);
        router.get("/path").handler(handler::shortestPath);

        router.route().failureHandler(ctx -> {
            if (ctx.response().ended()) {
                System.out.println("[FAIL] failureHandler: response already ended, failure=" + ctx.failure());
                return;
            }
            var failure = ctx.failure();
            if (failure instanceof HttpExceptions http) {
                System.out.println("[FAIL] " + ctx.request().method() + " " + ctx.request().path()
                        + " -> " + http.getStatusCode() + " " + http.getMessage());
                ctx.response()
                        .setStatusCode(http.getStatusCode())
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("detail", http.getMessage())
                                .encode());
            } else {
                System.out.println("[FAIL] " + ctx.request().method() + " " + ctx.request().path()
                        + " -> unexpected " + failure);
                int status = ctx.statusCode() >= 400 ? ctx.statusCode() : 500;
                ctx.response()
                        .setStatusCode(status)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("status", status)
                                .put("detail", failure.getMessage())
                                .encode());
            }
        });

        return router;
    }
}
