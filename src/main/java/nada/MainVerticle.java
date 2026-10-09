package nada;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServer;
import nada.api.ApiRouter;
import nada.exceptions.HttpExceptions;

public class MainVerticle extends VerticleBase {

  private HttpServer server;

  @Override
  public Future<?> start() {

    var router = ApiRouter.create(vertx);
    server = vertx.createHttpServer().requestHandler(router);

    return server.listen(8080)
        .onSuccess(httpServer -> {
          System.out.println(
              "Server running on port " + httpServer.actualPort());
        });
  }
}
