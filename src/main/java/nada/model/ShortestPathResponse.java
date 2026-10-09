package nada.model;

import java.util.List;

public record ShortestPathResponse(Boolean connected, Integer hops, List<User> path) {
}
