package nada;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServer;
import nada.api.ApiRouter;

public class MainVerticle extends VerticleBase {

  private HttpServer server;

  @Override
  public Future<?> start() {

    var router = ApiRouter.create(vertx);
    server = vertx.createHttpServer().requestHandler(router);

    var port = Integer.parseInt(
        System.getenv().getOrDefault("VERTX_APPLICATION_PORT", "8080"));
    return server.listen(port)
        .onSuccess(httpServer -> {
          System.out.println(
              "Server running on port " + httpServer.actualPort());
        });
  }
}
