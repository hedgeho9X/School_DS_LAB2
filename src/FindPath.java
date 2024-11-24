import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class FindPath extends JFrame {
    private ImageIcon mapImage;
    private List<Node> nodes;
    private Node startNode = null;
    private Node endNode = null;
    private List<Edge> edges;
    private List<List<Node>> allPaths = new ArrayList<>();
    private List<Node> shortestPath = null;
    private boolean showGraph = true;
    private boolean isAddingNode = false;
    private boolean isDeletingNode = false;
    private boolean isEditingNode = false;
    private JButton activeButton = null;
    private JButton addNodeBtn;
    private JButton deleteNodeBtn;
    private JButton editNodeBtn;
    private JTextArea pathInfoArea;
    private ImageIcon locationIcon;
    private Point firstClickPoint = null;
    private Point secondClickPoint = null;
    private StringBuilder debugInfo = new StringBuilder();
    private JPanel mapPanel;

    // ��ӻ����������
    private static final double PIXELS_PER_METER = calculatePixelsPerMeter(
        new Point(738, 551), 
        new Point(751, 653), 
        100.0  // ʵ�ʾ���100��
    );

    public FindPath() {
        setTitle("У԰����ϵͳ");
        setSize(1400, 1000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // ��ʼ�����б�
        edges = new ArrayList<>();
        
        // �޸�ͼƬ���غ���ʾ
        try {
            mapImage = new ImageIcon("D:\\tecentQQ_files\\IMG_20241124_190533.jpg");
            // ����ͼƬ��СΪ���ڵ����ϲ���
            Image img = mapImage.getImage();
            Image scaledImg = img.getScaledInstance(1000, 700, Image.SCALE_SMOOTH);
            mapImage = new ImageIcon(scaledImg);
        } catch (Exception e) {
            System.err.println("�޷����ص�ͼͼƬ��" + e.getMessage());
            e.printStackTrace();
        }
        
        // ��ʼ���ڵ�ͱ�
        nodes = NodeFileManager.loadNodes();
        if (nodes.isEmpty()) {
            nodes = new ArrayList<>();
            initializeGraph();
        } else {
            edges = NodeFileManager.loadEdges(nodes);
        }
        
        // ��������壬ʹ�� BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // ������������õ�ͼ
        mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.drawImage(mapImage.getImage(), 10, 10, mapImage.getIconWidth(), mapImage.getIconHeight(), this);
                if (showGraph) {
                    drawGraph(g2d);
                }
                drawPaths(g2d);
            }
        };
        mapPanel.setPreferredSize(new Dimension(1000, 800));
        
        // ������������� mapPanel
        mapPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });
        
        // �����Ҳ���Ϣ���
        pathInfoArea = new JTextArea();
        pathInfoArea.setEditable(false);
        pathInfoArea.setFont(new Font("΢���ź�", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(pathInfoArea);
        scrollPane.setPreferredSize(new Dimension(300, 800));
        
        // �޸Ŀ�����岼��
        JPanel controlPanel = createControlPanel();
        
        // ��װ���
        mainPanel.add(mapPanel, BorderLayout.CENTER);
        mainPanel.add(scrollPane, BorderLayout.EAST);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        try {
            locationIcon = new ImageIcon("C:\\Users\\Jeery\\Downloads\\��Pngtree��positioning red cartoon illustration icon_4621669.png");
            // ����ͼ���СΪ30x30����
            Image img = locationIcon.getImage();
            Image newImg = img.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
            locationIcon = new ImageIcon(newImg);
        } catch (Exception e) {
            System.err.println("�޷�����λ��ͼ��: " + e.getMessage());
        }
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(2, 3, 10, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // ��ʼ����ť
        addNodeBtn = new JButton("��ӽڵ�");
        deleteNodeBtn = new JButton("ɾ���ڵ�");
        editNodeBtn = new JButton("�༭�ڵ�");
        JButton toggleGraphBtn = new JButton("��ʾ/����ͼ");
        JButton resetBtn = new JButton("����");
        
        // ���ð�ť�¼�
        addNodeBtn.addActionListener(e -> {
            isAddingNode = !isAddingNode;
            isDeletingNode = false;
            isEditingNode = false;
            setActiveButton(addNodeBtn, isAddingNode);
            setActiveButton(deleteNodeBtn, false);
            setActiveButton(editNodeBtn, false);
            resetPath(); // ����·��״̬
        });
        
        deleteNodeBtn.addActionListener(e -> {
            isDeletingNode = !isDeletingNode;
            isAddingNode = false;
            isEditingNode = false;
            setActiveButton(deleteNodeBtn, isDeletingNode);
            setActiveButton(addNodeBtn, false);
            setActiveButton(editNodeBtn, false);
            resetPath(); // ����·��״̬
        });
        
        editNodeBtn.addActionListener(e -> {
            isEditingNode = !isEditingNode;
            isAddingNode = false;
            isDeletingNode = false;
            setActiveButton(editNodeBtn, isEditingNode);
            setActiveButton(addNodeBtn, false);
            setActiveButton(deleteNodeBtn, false);
            resetPath(); // ����·��״̬
        });
        
        toggleGraphBtn.addActionListener(e -> {
            showGraph = !showGraph;
            repaint();
        });
        
        resetBtn.addActionListener(e -> {
            resetPath();
            repaint();
        });
        
        // ��Ӱ�ť�����
        controlPanel.add(addNodeBtn);
        controlPanel.add(deleteNodeBtn);
        controlPanel.add(editNodeBtn);
        controlPanel.add(toggleGraphBtn);
        controlPanel.add(resetBtn);
        
        return controlPanel;
    }

    private void showConnectionDialog(Node node) {
        // ������ѡ���б�
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        Map<JCheckBox, Node> checkBoxMap = new HashMap<>();
        
        for (Node other : nodes) {
            if (other != node) {
                boolean isConnected = edges.stream().anyMatch(edge ->
                    (edge.start == node && edge.end == other) ||
                    (edge.start == other && edge.end == node));
                    
                JCheckBox checkBox = new JCheckBox(other.name, isConnected);
                checkBoxMap.put(checkBox, other);
                panel.add(checkBox);
            }
        }
        
        // ��ӹ�������Դ�������ڵ�
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(300, 400));
        
        // ��ʾ�Ի���
        int result = JOptionPane.showConfirmDialog(this, scrollPane,
            "ѡ��Ҫ���ӵĽڵ�", JOptionPane.OK_CANCEL_OPTION);
            
        if (result == JOptionPane.OK_OPTION) {
            // ��������
            edges.removeIf(edge -> edge.start == node || edge.end == node);
            
            for (Map.Entry<JCheckBox, Node> entry : checkBoxMap.entrySet()) {
                if (entry.getKey().isSelected()) {
                    edges.add(new Edge(node, entry.getValue()));
                }
            }
            
            NodeFileManager.saveEdges(edges);
            repaint();
        }
    }

    // ��Ӷ���ķ���

    private void initializeGraph() {
        // ��ʼ��ͼ�Ľڵ�ͱ�
        // ʾ����addNode("ͼ���", 100, 200);
    }

    private void drawGraph(Graphics2D g2d) {
        // ����ͼ�Ľڵ�ͱ�
        g2d.setColor(Color.GRAY);
        for (Edge edge : edges) {
            g2d.drawLine(edge.start.x, edge.start.y, edge.end.x, edge.end.y);
        }
        for (Node node : nodes) {
            g2d.setColor(Color.BLUE);
            g2d.fillOval(node.x - 5, node.y - 5, 10, 10);
        }
    }

    private void drawPaths(Graphics2D g2d) {
        // ����·��
        if (shortestPath != null) {
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(5.0f));
            for (int i = 0; i < shortestPath.size() - 1; i++) {
                Node current = shortestPath.get(i);
                Node next = shortestPath.get(i + 1);
                g2d.drawLine(current.x, current.y, next.x, next.y);
            }
        }
    }

    private Node findNearestNode(int x, int y) {
        System.out.println("------");
        Node nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Node node : nodes) {
            double dist = Math.sqrt(Math.pow(node.x - x, 2) + Math.pow(node.y - y, 2));
            if (dist < minDist) {
                minDist = dist;
                nearest = node;
            }
            System.out.println(node.name + ":" + dist + "->" + node.x + "," + node.y) ;
        }
        System.out.println(nearest.name +" "+ minDist) ;
        return nearest;
    }

    private void handleMouseClick(MouseEvent e) {
        // ���ǵ�ͼ����ƫ����
        int x = e.getX() - 10;
        int y = e.getY() - 10;
        
        if (isAddingNode) {
            handleAddNode(e);
        } else if (isDeletingNode) {
            handleDeleteNode(e);
        } else if (isEditingNode) {
            handleEditNode(e);
        } else {
            // ������ͨ��·��ѡ����
            if (startNode == null) {
                firstClickPoint = new Point(x, y);
                startNode = findNearestNode(x, y);
            } else if (endNode == null) {
                secondClickPoint = new Point(x, y);
                endNode = findNearestNode(x, y);
                findPaths();
            } else {
                debugInfo.setLength(0);
                firstClickPoint = new Point(x, y);
                secondClickPoint = null;
                startNode = findNearestNode(x, y);
                endNode = null;
                shortestPath = null;
                allPaths.clear();
            }
            
            updatePathInfo();
            repaint();
        }
    }

    private void setActiveButton(JButton button, boolean isActive) {
        if (activeButton != null) {
            activeButton.setBackground(null);
        }
        if (isActive) {
            button.setBackground(Color.LIGHT_GRAY);
            activeButton = button;
        } else {
            activeButton = null;
        }
    }

    private void resetPath() {
        startNode = null;
        endNode = null;
        shortestPath = null;
        allPaths.clear();
        pathInfoArea.setText("");
    }

    private void findPaths() {
        if (startNode != null && endNode != null) {
            // ʹ��Dijkstra�㷨�ҵ����·��
            shortestPath = Dijkstra.findShortestPath(nodes, edges, startNode, endNode);
            
            // ����·����Ϣ��ʾ
            StringBuilder info = new StringBuilder();
            info.append("���·����\n");
            if (shortestPath != null) {
                for (int i = 0; i < shortestPath.size(); i++) {
                    info.append(shortestPath.get(i).name);
                    if (i < shortestPath.size() - 1) {
                        info.append(" -> ");
                    }
                }
            }
            pathInfoArea.setText(info.toString());
        }
    }

    private void updatePathInfo() {
        StringBuilder info = new StringBuilder();
        

        
        // �����·�������·����Ϣ
        if (shortestPath != null && !shortestPath.isEmpty()) {
            info.append("=== ·����Ϣ ===\n");
            info.append("���·����\n");
            for (int i = 0; i < shortestPath.size(); i++) {
                Node node = shortestPath.get(i);
                info.append(String.format("%s (%d, %d)", node.name, node.x, node.y));
                if (i < shortestPath.size() - 1) {
                    info.append(" ->\n");
                }
            }
            info.append("\n\n");
            
            // ���·��������Ϣ��ͬʱ��ʾ���غ�ʵ�ʾ��룩
            double pathLengthPixels = calculatePathLength(shortestPath) * PIXELS_PER_METER;
            double pathLengthMeters = calculatePathLength(shortestPath);
            info.append(String.format("·������: %.1f �� (%.0f ����)\n", 
                pathLengthMeters, pathLengthPixels));
        }
        
        pathInfoArea.setText(info.toString());
    }

    private double calculatePathLength(List<Node> path) {
        double pixelLength = 0;
        if (path.size() < 2) return 0;
        
        // ����ӵ��λ�õ���һ���ڵ�ľ���
        if (firstClickPoint != null) {
            pixelLength += Math.sqrt(
                Math.pow(firstClickPoint.x - path.get(0).x, 2) +
                Math.pow(firstClickPoint.y - path.get(0).y, 2)
            );
        }
        
        // ����ڵ��ľ���
        for (int i = 0; i < path.size() - 1; i++) {
            Node current = path.get(i);
            Node next = path.get(i + 1);
            for (Edge edge : edges) {
                if ((edge.start == current && edge.end == next) ||
                    (edge.start == next && edge.end == current)) {
                    pixelLength += edge.weight;
                    break;
                }
            }
        }
        
        // ��������һ���ڵ㵽�յ�ľ���
        if (secondClickPoint != null && !path.isEmpty()) {
            Node lastNode = path.get(path.size() - 1);
            pixelLength += Math.sqrt(
                Math.pow(secondClickPoint.x - lastNode.x, 2) +
                Math.pow(secondClickPoint.y - lastNode.y, 2)
            );
        }
        
        // ת��Ϊʵ�ʾ��루�ף�
        return pixelLength / PIXELS_PER_METER;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // ���ÿ����
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // ����������ϸ
        g2d.setStroke(new BasicStroke(5.0f));

        // ���ƴӵ��λ�õ�����ڵ������
        g2d.setColor(new Color(255, 0, 0));
        if (firstClickPoint != null && startNode != null) {
            // ���ǵ�ͼƫ������������
            g2d.drawLine(
                firstClickPoint.x + 12,  // �ӻ�ƫ����
                firstClickPoint.y + 37,
                startNode.x + 9, 
                startNode.y + 28
            );
        }
        if (secondClickPoint != null && endNode != null) {
            g2d.drawLine(
                secondClickPoint.x + 12,  // �ӻ�ƫ����
                secondClickPoint.y + 37,
                endNode.x + 9,
                endNode.y + 28
            );
        }

        // ����λ��ͼ��
        if (firstClickPoint != null) {
            drawLocationIcon(g2d, firstClickPoint.x + 15, firstClickPoint.y + 30);
        }
        if (secondClickPoint != null) {
            drawLocationIcon(g2d, secondClickPoint.x + 15, secondClickPoint.y + 30);
        }
    }

    private void drawLocationIcon(Graphics2D g2d, int x, int y) {
        // ����ͼ�꣬ʹ������λ�ڵ��λ��
        g2d.drawImage(locationIcon.getImage(), 
                      x - locationIcon.getIconWidth()/2, 
                      y - locationIcon.getIconHeight()/2, 
                      locationIcon.getIconWidth(), 
                      locationIcon.getIconHeight(), 
                      null);
    }

    // ��Ӽ��㻻������ķ���
    private static double calculatePixelsPerMeter(Point p1, Point p2, double realDistanceInMeters) {
        double pixelDistance = Math.sqrt(
            Math.pow(p2.x - p1.x, 2) + 
            Math.pow(p2.y - p1.y, 2)
        );
        return pixelDistance / realDistanceInMeters;  // ����/��
    }

    // ��ӽڵ㴦����
    private void handleAddNode(MouseEvent e) {
        int x = e.getX() - 10;
        int y = e.getY() - 10;
        String name = JOptionPane.showInputDialog(this, "������ڵ����ƣ�");
        if (name != null && !name.trim().isEmpty()) {
            Node newNode = new Node(name.trim(), x, y);
            nodes.add(newNode);
            NodeFileManager.saveNodes(nodes);
            
            // ѯ���Ƿ��������
            int choice = JOptionPane.showConfirmDialog(this, 
                "�Ƿ�����������ڵ�����ӣ�", 
                "�������", 
                JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) {
                showConnectionDialog(newNode);
            }
            
            isAddingNode = false;
            setActiveButton(addNodeBtn, false);
            repaint();
        }
    }

    // ɾ���ڵ㴦����
    private void handleDeleteNode(MouseEvent e) {
        int x = e.getX() - 10;
        int y = e.getY() - 10;
        Node nearestNode = findNearestNode(x, y);
        if (nearestNode != null) {
            int choice = JOptionPane.showConfirmDialog(this,
                "ȷ��Ҫɾ���ڵ� " + nearestNode.name + " ��",
                "ɾ���ڵ�",
                JOptionPane.YES_NO_OPTION);
                
            if (choice == JOptionPane.YES_OPTION) {
                nodes.remove(nearestNode);
                // ɾ��???�ýڵ���ص����б�
                edges.removeIf(edge -> 
                    edge.start == nearestNode || edge.end == nearestNode);
                NodeFileManager.saveNodes(nodes);
                NodeFileManager.saveEdges(edges);
                
                isDeletingNode = false;
                setActiveButton(deleteNodeBtn, false);
                repaint();
            }
        }
    }

    // �༭�ڵ㴦����
    private void handleEditNode(MouseEvent e) {
        int x = e.getX() - 10;
        int y = e.getY() - 10;
        Node nearestNode = findNearestNode(x, y);
        if (nearestNode != null) {
            String newName = JOptionPane.showInputDialog(this,
                "�������µĽڵ����ƣ�",
                nearestNode.name);
                
            if (newName != null && !newName.trim().isEmpty()) {
                nearestNode.name = newName.trim();
                NodeFileManager.saveNodes(nodes);
                
                // ѯ���Ƿ��޸�����
                int choice = JOptionPane.showConfirmDialog(this,
                    "�Ƿ��޸Ľڵ����ӣ�",
                    "�޸�����",
                    JOptionPane.YES_NO_OPTION);
                    
                if (choice == JOptionPane.YES_OPTION) {
                    showConnectionDialog(nearestNode);
                }
                
                isEditingNode = false;
                setActiveButton(editNodeBtn, false);
                repaint();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FindPath app = new FindPath();
            app.setVisible(true);
        });
    }
}
