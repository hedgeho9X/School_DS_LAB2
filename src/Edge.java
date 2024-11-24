public class Edge {
    Node start;
    Node end;
    double weight;
    
    public Edge(Node start, Node end) {
        this.start = start;
        this.end = end;
        this.weight = Math.sqrt(
            Math.pow(start.x - end.x, 2) + 
            Math.pow(start.y - end.y, 2)
        );
    }
} 