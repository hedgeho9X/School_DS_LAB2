import java.util.Objects;

public class Node {
    String name;
    int x, y;
    
    public Node(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return x == node.x && y == node.y && Objects.equals(name, node.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, x, y);
    }
} 