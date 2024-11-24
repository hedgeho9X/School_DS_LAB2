import java.util.*;

public class Dijkstra {
    public static List<Node> findShortestPath(List<Node> nodes, List<Edge> edges, 
                                            Node start, Node end) {
        Map<Node, Double> distances = new HashMap<>();
        Map<Node, Node> previousNodes = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(
            Comparator.comparingDouble(distances::get));
        
        // 初始化距离
        for (Node node : nodes) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(start, 0.0);
        queue.add(start);
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            
            if (current == end) {
                break;
            }
            
            // 获取相邻节点
            List<Node> neighbors = getNeighbors(current, edges);
            for (Node neighbor : neighbors) {
                double newDist = distances.get(current) + 
                    getDistance(current, neighbor, edges);
                
                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previousNodes.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }
        
        return reconstructPath(start, end, previousNodes);
    }
    
    private static List<Node> getNeighbors(Node node, List<Edge> edges) {
        List<Node> neighbors = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.start == node) {
                neighbors.add(edge.end);
            } else if (edge.end == node) {
                neighbors.add(edge.start);
            }
        }
        return neighbors;
    }
    
    private static double getDistance(Node n1, Node n2, List<Edge> edges) {
        for (Edge edge : edges) {
            if ((edge.start == n1 && edge.end == n2) || 
                (edge.start == n2 && edge.end == n1)) {
                return edge.weight;
            }
        }
        return Double.POSITIVE_INFINITY;
    }
    
    private static List<Node> reconstructPath(Node start, Node end, 
                                            Map<Node, Node> previousNodes) {
        List<Node> path = new ArrayList<>();
        Node current = end;
        
        while (current != null) {
            path.add(0, current);
            current = previousNodes.get(current);
        }
        
        return path.get(0) == start ? path : new ArrayList<>();
    }
} 