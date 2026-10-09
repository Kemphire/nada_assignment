package nada.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
class ApiRouterTest {

    private HttpServer server;
    private WebClient client;
    private int port;

    @BeforeEach
    void setup(Vertx vertx, VertxTestContext ctx) {
        vertx.createHttpServer()
                .requestHandler(ApiRouter.create(vertx))
                .listen(0)
                .onComplete(ctx.succeeding(s -> {
                    server = s;
                    port = s.actualPort();
                    client = WebClient.create(vertx);
                    ctx.completeNow();
                }));
    }

    @AfterEach
    void teardown(VertxTestContext ctx) {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.close().onComplete(ctx.succeedingThenComplete());
        } else {
            ctx.completeNow();
        }
    }

    private Future<Integer> createId(String name) {
        return client.post(port, "localhost", "/people")
                .sendJsonObject(new JsonObject().put("name", name))
                .compose(resp -> {
                    if (resp.statusCode() != 201) {
                        return Future.failedFuture("POST /people -> " + resp.statusCode()
                                + " " + resp.bodyAsString());
                    }
                    return Future.succeededFuture(resp.bodyAsJsonObject().getInteger("id"));
                });
    }

    private Future<Void> linkBothWays(int x, int y) {
        return client.post(port, "localhost", "/knows")
                .sendJsonObject(new JsonObject().put("a", x).put("b", y))
                .compose(r1 -> {
                    if (r1.statusCode() != 204) {
                        return Future.failedFuture("POST /knows -> " + r1.statusCode()
                                + " " + r1.bodyAsString());
                    }
                    return client.post(port, "localhost", "/knows")
                            .sendJsonObject(new JsonObject().put("a", y).put("b", x));
                })
                .compose(r2 -> {
                    if (r2.statusCode() != 204) {
                        return Future.failedFuture("POST /knows (reverse) -> " + r2.statusCode()
                                + " " + r2.bodyAsString());
                    }
                    return Future.succeededFuture();
                });
    }

    @Test
    void hindiName_roundTrips(Vertx vertx, VertxTestContext ctx) {
        client.post(port, "localhost", "/people")
                .sendJsonObject(new JsonObject().put("name", "आशा"))
                .onComplete(ctx.succeeding(resp -> {
                    ctx.verify(() -> {
                        assertEquals(201, resp.statusCode());
                        assertEquals("आशा", resp.bodyAsJsonObject().getString("name"));
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void createBadRequest_400(Vertx vertx, VertxTestContext ctx) {
        client.post(port, "localhost", "/people")
                .putHeader("content-type", "application/json")
                .sendBuffer(Buffer.buffer("{bad json"))
                .compose(r1 -> {
                    ctx.verify(() -> assertEquals(400, r1.statusCode(), "invalid JSON"));
                    return client.post(port, "localhost", "/people")
                            .sendJsonObject(new JsonObject());
                })
                .compose(r2 -> {
                    ctx.verify(() -> assertEquals(400, r2.statusCode(), "missing name"));
                    return client.post(port, "localhost", "/people")
                            .sendJsonObject(new JsonObject().put("name", ""));
                })
                .compose(r3 -> {
                    ctx.verify(() -> assertEquals(400, r3.statusCode(), "empty name"));
                    return client.post(port, "localhost", "/people")
                            .sendJsonObject(new JsonObject().put("name", "   "));
                })
                .onComplete(ctx.succeeding(r4 -> {
                    ctx.verify(() -> assertEquals(400, r4.statusCode(), "blank name"));
                    ctx.completeNow();
                }));
    }

    @Test
    void linkSameId_400(Vertx vertx, VertxTestContext ctx) {
        createId("Solo-" + System.nanoTime())
                .compose(id -> client.post(port, "localhost", "/knows")
                        .sendJsonObject(new JsonObject().put("a", id).put("b", id)))
                .onComplete(ctx.succeeding(resp -> {
                    ctx.verify(() -> assertEquals(400, resp.statusCode()));
                    ctx.completeNow();
                }));
    }

    @Test
    void unknownPerson_404(Vertx vertx, VertxTestContext ctx) {
        client.get(port, "localhost", "/people/999999/contacts")
                .send()
                .onComplete(ctx.succeeding(resp -> {
                    ctx.verify(() -> {
                        assertEquals(404, resp.statusCode());
                        assertTrue(resp.bodyAsJsonObject().containsKey("detail"),
                                "expected {\"detail\":...} got " + resp.bodyAsString());
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void pathParamsMissingOrMalformed_400(Vertx vertx, VertxTestContext ctx) {
        client.get(port, "localhost", "/path").send()
                .compose(r1 -> {
                    ctx.verify(() -> assertEquals(400, r1.statusCode(), "missing from/to"));
                    return client.get(port, "localhost", "/path")
                            .addQueryParam("from", "1").send();
                })
                .compose(r2 -> {
                    ctx.verify(() -> assertEquals(400, r2.statusCode(), "missing to"));
                    return client.get(port, "localhost", "/path")
                            .addQueryParam("from", "abc").addQueryParam("to", "1").send();
                })
                .onComplete(ctx.succeeding(r3 -> {
                    ctx.verify(() -> assertEquals(400, r3.statusCode(), "malformed from"));
                    ctx.completeNow();
                }));
    }

    @Test
    void pathSameUser_returnsSelf(Vertx vertx, VertxTestContext ctx) {
        createId("Solo-" + System.nanoTime())
                .compose(id -> client.get(port, "localhost", "/path")
                        .addQueryParam("from", String.valueOf(id))
                        .addQueryParam("to", String.valueOf(id)).send())
                .onComplete(ctx.succeeding(resp -> {
                    ctx.verify(() -> {
                        assertEquals(200, resp.statusCode());
                        var body = resp.bodyAsJsonObject();
                        assertTrue(body.getBoolean("connected"));
                        assertEquals(0, body.getInteger("hops"));
                        assertEquals(1, body.getJsonArray("path").size());
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void pathThreeHopBoundary(Vertx vertx, VertxTestContext ctx) {
        var tag = String.valueOf(System.nanoTime());
        var names = List.of("A-" + tag, "B-" + tag, "C-" + tag, "D-" + tag, "E-" + tag);

        Future<List<Integer>> idsFuture = Future.succeededFuture(new ArrayList<>());
        for (var n : names) {
            idsFuture = idsFuture.compose(ids -> createId(n).map(id -> {
                ids.add(id);
                return ids;
            }));
        }

        idsFuture.compose(ids -> {
            Future<Void> links = Future.succeededFuture();
            for (int i = 0; i + 1 < ids.size(); i++) {
                int x = ids.get(i);
                int y = ids.get(i + 1);
                links = links.compose(v -> linkBothWays(x, y));
            }
            return links.map(ids);
        }).compose(ids -> client.get(port, "localhost", "/path")
                .addQueryParam("from", String.valueOf(ids.get(0)))
                .addQueryParam("to", String.valueOf(ids.get(3))).send()
                .map(resp -> {
                    ctx.verify(() -> {
                        assertEquals(200, resp.statusCode());
                        assertTrue(resp.bodyAsJsonObject().getBoolean("connected"),
                                "A->D should be connected, got " + resp.bodyAsString());
                        assertEquals(3, resp.bodyAsJsonObject().getInteger("hops"));
                    });
                    return ids;
                })).compose(ids -> client.get(port, "localhost", "/path")
                        .addQueryParam("from", String.valueOf(ids.get(0)))
                        .addQueryParam("to", String.valueOf(ids.get(4))).send())
                .onComplete(ctx.succeeding(resp -> {
                    ctx.verify(() -> {
                        assertEquals(200, resp.statusCode());
                        assertFalse(resp.bodyAsJsonObject().getBoolean("connected"),
                                "A->E (4 hops) should not be connected, got " + resp.bodyAsString());
                    });
                    ctx.completeNow();
                }));
    }

    // Keep unused-import guard quiet if Vert.x changes Buffer/HttpResponse re-exports.
    @SuppressWarnings("unused")
    private static Class<?>[] referencedTypes() {
        return new Class<?>[] { Buffer.class, HttpResponse.class };
    }
}
