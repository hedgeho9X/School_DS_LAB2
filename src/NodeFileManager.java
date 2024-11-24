import java.io.*;
import java.util.*;

public class NodeFileManager {
    private static final String NODES_FILE = "nodes.txt";
    private static final String EDGES_FILE = "edges.txt";
    
    public static void saveNodes(List<Node> nodes) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(NODES_FILE))) {
            for (Node node : nodes) {
                writer.println(node.name + "," + node.x + "," + node.y);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void saveEdges(List<Edge> edges) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(EDGES_FILE))) {
            for (Edge edge : edges) {
                writer.println(edge.start.name + "," + edge.end.name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static List<Node> loadNodes() {
        List<Node> nodes = new ArrayList<>();
        File file = new File(NODES_FILE);
        if (!file.exists()) return nodes;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                nodes.add(new Node(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nodes;
    }
    
    public static List<Edge> loadEdges(List<Node> nodes) {
        List<Edge> edges = new ArrayList<>();
        File file = new File(EDGES_FILE);
        if (!file.exists()) return edges;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                Node start = findNodeByName(nodes, parts[0]);
                Node end = findNodeByName(nodes, parts[1]);
                if (start != null && end != null) {
                    edges.add(new Edge(start, end));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return edges;
    }
    
    private static Node findNodeByName(List<Node> nodes, String name) {
        return nodes.stream()
                   .filter(n -> n.name.equals(name))
                   .findFirst()
                   .orElse(null);
    }
} 