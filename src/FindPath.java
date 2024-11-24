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

    // 添加换算比例常量
    private static final double PIXELS_PER_METER = calculatePixelsPerMeter(
        new Point(738, 551), 
        new Point(751, 653), 
        100.0  // 实际距离100米
    );

    public FindPath() {
        setTitle("校园导航系统");
        setSize(1400, 1000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // 初始化边列表
        edges = new ArrayList<>();
        
        // 修改图片加载和显示
        try {
            mapImage = new ImageIcon("SUSEmap.jpg");
            // 调整图片大小为窗口的左上部分
            Image img = mapImage.getImage();
            Image scaledImg = img.getScaledInstance(1000, 700, Image.SCALE_SMOOTH);
            mapImage = new ImageIcon(scaledImg);
        } catch (Exception e) {
            System.err.println("无法加载地图图片：" + e.getMessage());
            e.printStackTrace();
        }
        
        // 初始化节点和边
        nodes = NodeFileManager.loadNodes();
        if (nodes.isEmpty()) {
            nodes = new ArrayList<>();
            initializeGraph();
        } else {
            edges = NodeFileManager.loadEdges(nodes);
        }
        
        // 创建主面板，使用 BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 创建左侧面板放置地图
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
        
        // 添加鼠标监听器到 mapPanel
        mapPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });
        
        // 创建右侧信息面板
        pathInfoArea = new JTextArea();
        pathInfoArea.setEditable(false);
        pathInfoArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(pathInfoArea);
        scrollPane.setPreferredSize(new Dimension(300, 800));
        
        // 修改控制面板布局
        JPanel controlPanel = createControlPanel();
        
        // 组装面板
        mainPanel.add(mapPanel, BorderLayout.CENTER);
        mainPanel.add(scrollPane, BorderLayout.EAST);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        try {
            locationIcon = new ImageIcon("icon.png");
            // 调整图标大小为30x30像素
            Image img = locationIcon.getImage();
            Image newImg = img.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
            locationIcon = new ImageIcon(newImg);
        } catch (Exception e) {
            System.err.println("无法加载位置图标: " + e.getMessage());
        }
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(2, 3, 10, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 初始化按钮
        addNodeBtn = new JButton("添加节点");
        deleteNodeBtn = new JButton("删除节点");
        editNodeBtn = new JButton("编辑节点");
        JButton toggleGraphBtn = new JButton("显示/隐藏图");
        JButton resetBtn = new JButton("重置");
        
        // 设置按钮事件
        addNodeBtn.addActionListener(e -> {
            isAddingNode = !isAddingNode;
            isDeletingNode = false;
            isEditingNode = false;
            setActiveButton(addNodeBtn, isAddingNode);
            setActiveButton(deleteNodeBtn, false);
            setActiveButton(editNodeBtn, false);
            resetPath(); // 重置路径状态
        });
        
        deleteNodeBtn.addActionListener(e -> {
            isDeletingNode = !isDeletingNode;
            isAddingNode = false;
            isEditingNode = false;
            setActiveButton(deleteNodeBtn, isDeletingNode);
            setActiveButton(addNodeBtn, false);
            setActiveButton(editNodeBtn, false);
            resetPath(); // 重置路径状态
        });
        
        editNodeBtn.addActionListener(e -> {
            isEditingNode = !isEditingNode;
            isAddingNode = false;
            isDeletingNode = false;
            setActiveButton(editNodeBtn, isEditingNode);
            setActiveButton(addNodeBtn, false);
            setActiveButton(deleteNodeBtn, false);
            resetPath(); // 重置路径状态
        });
        
        toggleGraphBtn.addActionListener(e -> {
            showGraph = !showGraph;
            repaint();
        });
        
        resetBtn.addActionListener(e -> {
            resetPath();
            repaint();
        });
        
        // 添加按钮到面板
        controlPanel.add(addNodeBtn);
        controlPanel.add(deleteNodeBtn);
        controlPanel.add(editNodeBtn);
        controlPanel.add(toggleGraphBtn);
        controlPanel.add(resetBtn);
        
        return controlPanel;
    }

    private void showConnectionDialog(Node node) {
        // 创建复选框列表
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
        
        // 添加滚动面板以处理大量节点
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(300, 400));
        
        // 显示对话框
        int result = JOptionPane.showConfirmDialog(this, scrollPane,
            "选择要连接的节点", JOptionPane.OK_CANCEL_OPTION);
            
        if (result == JOptionPane.OK_OPTION) {
            // 更新连接
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

    // 添加定义的方法

    private void initializeGraph() {
        // 初始化图的节点和边
        // 示例：addNode("图书馆", 100, 200);
    }

    private void drawGraph(Graphics2D g2d) {
        // 绘制图的节点和边
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
        // 绘制路径
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
        // 考虑地图面板的偏移量
        int x = e.getX() - 10;
        int y = e.getY() - 10;
        
        if (isAddingNode) {
            handleAddNode(e);
        } else if (isDeletingNode) {
            handleDeleteNode(e);
        } else if (isEditingNode) {
            handleEditNode(e);
        } else {
            // 处理普通的路径选择点击
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
            // 使用Dijkstra算法找到最短路径
            shortestPath = Dijkstra.findShortestPath(nodes, edges, startNode, endNode);
            
            // 更新路径信息显示
            StringBuilder info = new StringBuilder();
            info.append("最短路径：\n");
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
        

        
        // 如果有路径，添加路径信息
        if (shortestPath != null && !shortestPath.isEmpty()) {
            info.append("=== 路径信息 ===\n");
            info.append("最短路径：\n");
            for (int i = 0; i < shortestPath.size(); i++) {
                Node node = shortestPath.get(i);
                info.append(String.format("%s (%d, %d)", node.name, node.x, node.y));
                if (i < shortestPath.size() - 1) {
                    info.append(" ->\n");
                }
            }
            info.append("\n\n");
            
            // 添加路径长度信息（同时显示像素和实际距离）
            double pathLengthPixels = calculatePathLength(shortestPath) * PIXELS_PER_METER;
            double pathLengthMeters = calculatePathLength(shortestPath);
            info.append(String.format("路径长度: %.1f 米 (%.0f 像素)\n", 
                pathLengthMeters, pathLengthPixels));
        }
        
        pathInfoArea.setText(info.toString());
    }

    private double calculatePathLength(List<Node> path) {
        double pixelLength = 0;
        if (path.size() < 2) return 0;
        
        // 计算从点击位置到第一个节点的距离
        if (firstClickPoint != null) {
            pixelLength += Math.sqrt(
                Math.pow(firstClickPoint.x - path.get(0).x, 2) +
                Math.pow(firstClickPoint.y - path.get(0).y, 2)
            );
        }
        
        // 计算节点间的距离
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
        
        // 计算从最后一个节点到终点的距离
        if (secondClickPoint != null && !path.isEmpty()) {
            Node lastNode = path.get(path.size() - 1);
            pixelLength += Math.sqrt(
                Math.pow(secondClickPoint.x - lastNode.x, 2) +
                Math.pow(secondClickPoint.y - lastNode.y, 2)
            );
        }
        
        // 转换为实际距离（米）
        return pixelLength / PIXELS_PER_METER;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 设置线条粗细
        g2d.setStroke(new BasicStroke(5.0f));

        // 绘制从点击位置到最近节点的连线
        g2d.setColor(new Color(255, 0, 0));
        if (firstClickPoint != null && startNode != null) {
            // 考虑地图偏移量绘制线条
            g2d.drawLine(
                firstClickPoint.x + 12,  // 加回偏移量
                firstClickPoint.y + 37,
                startNode.x + 9, 
                startNode.y + 28
            );
        }
        if (secondClickPoint != null && endNode != null) {
            g2d.drawLine(
                secondClickPoint.x + 12,  // 加回偏移量
                secondClickPoint.y + 37,
                endNode.x + 9,
                endNode.y + 28
            );
        }

        // 绘制位置图标
        if (firstClickPoint != null) {
            drawLocationIcon(g2d, firstClickPoint.x + 15, firstClickPoint.y + 30);
        }
        if (secondClickPoint != null) {
            drawLocationIcon(g2d, secondClickPoint.x + 15, secondClickPoint.y + 30);
        }
    }

    private void drawLocationIcon(Graphics2D g2d, int x, int y) {
        // 绘制图标，使其中心位于点击位置
        g2d.drawImage(locationIcon.getImage(), 
                      x - locationIcon.getIconWidth()/2, 
                      y - locationIcon.getIconHeight()/2, 
                      locationIcon.getIconWidth(), 
                      locationIcon.getIconHeight(), 
                      null);
    }

    // 添加计算换算比例的方法
    private static double calculatePixelsPerMeter(Point p1, Point p2, double realDistanceInMeters) {
        double pixelDistance = Math.sqrt(
            Math.pow(p2.x - p1.x, 2) + 
            Math.pow(p2.y - p1.y, 2)
        );
        return pixelDistance / realDistanceInMeters;  // 像素/米
    }

    // 添加节点处理方法
    private void handleAddNode(MouseEvent e) {
        int x = e.getX() - 10;
        int y = e.getY() - 10;
        String name = JOptionPane.showInputDialog(this, "请输入节点名称：");
        if (name != null && !name.trim().isEmpty()) {
            Node newNode = new Node(name.trim(), x, y);
            nodes.add(newNode);
            NodeFileManager.saveNodes(nodes);
            
            // 询问是否添加连接
            int choice = JOptionPane.showConfirmDialog(this, 
                "是否添加与其他节点的连接？", 
                "添加连接", 
                JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) {
                showConnectionDialog(newNode);
            }
            
            isAddingNode = false;
            setActiveButton(addNodeBtn, false);
            repaint();
        }
    }

    // 删除节点处理方法
    private void handleDeleteNode(MouseEvent e) {
        int x = e.getX() - 10;
        int y = e.getY() - 10;
        Node nearestNode = findNearestNode(x, y);
        if (nearestNode != null) {
            int choice = JOptionPane.showConfirmDialog(this,
                "确定要删除节点 " + nearestNode.name + " 吗？",
                "删除节点",
                JOptionPane.YES_NO_OPTION);
                
            if (choice == JOptionPane.YES_OPTION) {
                nodes.remove(nearestNode);
                // 删除???该节点相关的所有边
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

    // 编辑节点处理方法
    private void handleEditNode(MouseEvent e) {
        int x = e.getX() - 10;
        int y = e.getY() - 10;
        Node nearestNode = findNearestNode(x, y);
        if (nearestNode != null) {
            String newName = JOptionPane.showInputDialog(this,
                "请输入新的节点名称：",
                nearestNode.name);
                
            if (newName != null && !newName.trim().isEmpty()) {
                nearestNode.name = newName.trim();
                NodeFileManager.saveNodes(nodes);
                
                // 询问是否修改连接
                int choice = JOptionPane.showConfirmDialog(this,
                    "是否修改节点连接？",
                    "修改连接",
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
