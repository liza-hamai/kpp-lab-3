import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Гра "Життя" (Game of Life) — клітинний автомат Джона Конвея.
 * Кросплатформна реалізація на Java з графічним інтерфейсом (Swing).
 *
 * Правила:
 *  1. Жива клітина з 2 або 3 живими сусідами — виживає.
 *  2. Жива клітина з 0 або 1 живим сусідом — вмирає (самотність).
 *  3. Жива клітина з 4+ живими сусідами — вмирає (перенаселення).
 *  4. Мертва клітина рівно з 3 живими сусідами — оживає.
 */
public class GameOfLife extends JFrame {

    // ── Константи ──────────────────────────────────────────────────────────
    private static final int ROWS        = 60;
    private static final int COLS        = 80;
    private static final int CELL_SIZE   = 12;
    private static final int PANEL_W     = COLS * CELL_SIZE;
    private static final int PANEL_H     = ROWS * CELL_SIZE;
    private static final int TIMER_DELAY = 100; // мс між поколіннями

    // ── Стан ───────────────────────────────────────────────────────────────
    private boolean[][] grid    = new boolean[ROWS][COLS];
    private boolean[][] nextGrid = new boolean[ROWS][COLS];
    private int generation = 0;
    private boolean running = false;

    // ── UI-компоненти ──────────────────────────────────────────────────────
    private final GridPanel gridPanel;
    private final JLabel    statusLabel;
    private final Timer     timer;

    // ══════════════════════════════════════════════════════════════════════
    public GameOfLife() {
        super("Гра Життя — Conway's Game of Life");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // ── Панель сітки ──────────────────────────────────────────────────
        gridPanel = new GridPanel();
        gridPanel.setPreferredSize(new Dimension(PANEL_W, PANEL_H));

        // ── Кнопки керування ─────────────────────────────────────────────
        JButton btnStart    = new JButton("▶ Старт");
        JButton btnStop     = new JButton("⏸ Пауза");
        JButton btnStep     = new JButton("⏭ Крок");
        JButton btnRandom   = new JButton("🔀 Випадково");
        JButton btnClear    = new JButton("Очистити");
        JButton btnGlider   = new JButton("Планер");

        statusLabel = new JLabel("Покоління: 0  |  ЛКМ — додати клітину, ПКМ — видалити");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // ── Панель кнопок ─────────────────────────────────────────────────
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        controls.add(btnStart);
        controls.add(btnStop);
        controls.add(btnStep);
        controls.add(btnRandom);
        controls.add(btnClear);
        controls.add(btnGlider);

        // ── Компонування вікна ────────────────────────────────────────────
        setLayout(new BorderLayout());
        add(gridPanel,  BorderLayout.CENTER);
        add(controls,   BorderLayout.NORTH);
        add(statusLabel, BorderLayout.SOUTH);

        // ── Таймер ────────────────────────────────────────────────────────
        timer = new Timer(TIMER_DELAY, e -> {
            nextGeneration();
            gridPanel.repaint();
            updateStatus();
        });

        // ── Обробники кнопок ──────────────────────────────────────────────
        btnStart.addActionListener(e -> { running = true;  timer.start(); });
        btnStop .addActionListener(e -> { running = false; timer.stop();  });
        btnStep .addActionListener(e -> {
            timer.stop(); running = false;
            nextGeneration();
            gridPanel.repaint();
            updateStatus();
        });
        btnRandom.addActionListener(e -> {
            timer.stop(); running = false;
            randomFill(0.25);
            generation = 0;
            gridPanel.repaint();
            updateStatus();
        });
        btnClear.addActionListener(e -> {
            timer.stop(); running = false;
            clearGrid();
            generation = 0;
            gridPanel.repaint();
            updateStatus();
        });
        btnGlider.addActionListener(e -> {
            timer.stop(); running = false;
            clearGrid();
            addGlider(5, 5);
            generation = 0;
            gridPanel.repaint();
            updateStatus();
        });

        // ── Малювання мишею ───────────────────────────────────────────────
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { handleMouse(e); }
            @Override public void mouseDragged(MouseEvent e)  { handleMouse(e); }
        };
        gridPanel.addMouseListener(mouseAdapter);
        gridPanel.addMouseMotionListener(mouseAdapter);

        // ── Початковий стан ───────────────────────────────────────────────
        randomFill(0.25);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Логіка гри
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Обчислює наступне покоління згідно з правилами Конвея.
     */
    private void nextGeneration() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int neighbours = countNeighbours(r, c);
                boolean alive = grid[r][c];

                if (alive) {
                    // Правило 1: 2 або 3 сусіди — виживає
                    // Правило 2: < 2 сусідів — вмирає від самотності
                    // Правило 3: > 3 сусідів — вмирає від перенаселення
                    nextGrid[r][c] = (neighbours == 2 || neighbours == 3);
                } else {
                    // Правило 4: рівно 3 сусіди — оживає
                    nextGrid[r][c] = (neighbours == 3);
                }
            }
        }
        // Копіюємо nextGrid → grid
        boolean[][] tmp = grid;
        grid = nextGrid;
        nextGrid = tmp;
        generation++;
    }

    /**
     * Підраховує кількість живих сусідів клітинки (r, c).
     * Поле є тороїдальним — краї замкнені.
     */
    private int countNeighbours(int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue; // сама клітинка не рахується
                int nr = (r + dr + ROWS) % ROWS;  // тороїдальне обгортання
                int nc = (c + dc + COLS) % COLS;
                if (grid[nr][nc]) count++;
            }
        }
        return count;
    }

    /** Заповнює поле випадково з заданою щільністю [0..1]. */
    private void randomFill(double density) {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c] = Math.random() < density;
    }

    /** Очищає поле (всі клітинки мертві). */
    private void clearGrid() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c] = false;
    }

    /**
     * Додає класичний планер (glider) у позицію (row, col).
     * Планер — найменший відомий рухомий об'єкт у Грі Життя.
     *
     *  . O .
     *  . . O
     *  O O O
     */
    private void addGlider(int row, int col) {
        int[][] pattern = {
                {0, 1}, {1, 2}, {2, 0}, {2, 1}, {2, 2}
        };
        for (int[] cell : pattern)
            grid[row + cell[0]][col + cell[1]] = true;
    }

    /** Обробляє натискання/перетягування миші на сітці. */
    private void handleMouse(MouseEvent e) {
        int c = e.getX() / CELL_SIZE;
        int r = e.getY() / CELL_SIZE;
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return;
        grid[r][c] = SwingUtilities.isLeftMouseButton(e);
        gridPanel.repaint();
        updateStatus();
    }

    private void updateStatus() {
        long alive = 0;
        for (boolean[] row : grid)
            for (boolean cell : row)
                if (cell) alive++;
        statusLabel.setText(
                String.format("Покоління: %d  |  Живих клітин: %d  |  ЛКМ — додати, ПКМ — видалити",
                        generation, alive));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Внутрішній клас: відображення сітки
    // ══════════════════════════════════════════════════════════════════════
    private class GridPanel extends JPanel {

        private static final Color COLOR_ALIVE = new Color(50, 180, 80);
        private static final Color COLOR_DEAD  = new Color(20, 20, 30);
        private static final Color COLOR_GRID  = new Color(40, 40, 50);

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // Фон
            g2.setColor(COLOR_DEAD);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Клітинки
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    int x = c * CELL_SIZE;
                    int y = r * CELL_SIZE;
                    if (grid[r][c]) {
                        g2.setColor(COLOR_ALIVE);
                        g2.fillRect(x + 1, y + 1, CELL_SIZE - 1, CELL_SIZE - 1);
                    }
                }
            }

            // Сітка
            g2.setColor(COLOR_GRID);
            for (int r = 0; r <= ROWS; r++)
                g2.drawLine(0, r * CELL_SIZE, PANEL_W, r * CELL_SIZE);
            for (int c = 0; c <= COLS; c++)
                g2.drawLine(c * CELL_SIZE, 0, c * CELL_SIZE, PANEL_H);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        // Запуск GUI в потоці подій Swing (EDT)
        SwingUtilities.invokeLater(GameOfLife::new);
    }
}