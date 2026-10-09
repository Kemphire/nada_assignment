package nada.store;

import nada.graph.SocialGraph;
import nada.graph.SocialGraphInterface;

public enum NetworkManager {
    INSTANCE;

    static private final SocialGraphInterface graph = new SocialGraph();

    static public SocialGraphInterface getGraph() {
        return graph;
    };
}
