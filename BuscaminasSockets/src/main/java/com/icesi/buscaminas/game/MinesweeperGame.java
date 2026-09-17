package com.icesi.buscaminas.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class MinesweeperGame {
    private final int rows;
    private final int cols;
    private final int mineCount;
    private final boolean[][] mines;
    private final boolean[][] opened;
    private final boolean[][] flagged;
    private GameStatus status;
    private int openedSafeCells;

    public MinesweeperGame(int rows, int cols, int mineCount) {
        if (rows < 2 || cols < 2) {
            throw new IllegalArgumentException("El tablero debe ser de al menos 2x2.");
        }
        if (rows > 30 || cols > 30) {
            throw new IllegalArgumentException("El tablero maximo permitido es 30x30.");
        }
        if (mineCount < 1 || mineCount >= rows * cols) {
            throw new IllegalArgumentException("La cantidad de minas debe estar entre 1 y filas*columnas-1.");
        }

        this.rows = rows;
        this.cols = cols;
        this.mineCount = mineCount;
        this.mines = new boolean[rows][cols];
        this.opened = new boolean[rows][cols];
        this.flagged = new boolean[rows][cols];
        this.status = GameStatus.PLAYING;
        placeMines();
    }

    private void placeMines() {
        List<Integer> positions = new ArrayList<>(rows * cols);
        for (int i = 0; i < rows * cols; i++) {
            positions.add(i);
        }
        Collections.shuffle(positions);

        for (int i = 0; i < mineCount; i++) {
            int position = positions.get(i);
            mines[position / cols][position % cols] = true;
        }
    }

    public synchronized ActionResult openCell(int row, int col) {
        ActionResult validation = validatePlayableCell(row, col);
        if (validation != null) {
            return validation;
        }
        if (flagged[row][col]) {
            return ActionResult.error("La casilla tiene bandera. Quitala antes de abrirla.");
        }
        if (opened[row][col]) {
            return ActionResult.error("La casilla ya esta abierta.");
        }

        if (mines[row][col]) {
            opened[row][col] = true;
            status = GameStatus.LOST;
            return ActionResult.ok("BOOM. Abriste una mina.");
        }

        floodOpen(row, col);
        updateWinCondition();
        return status == GameStatus.WON
                ? ActionResult.ok("Abriste la casilla y despejaste todas las casillas seguras. Ganaste.")
                : ActionResult.ok("Casilla abierta.");
    }

    public synchronized ActionResult toggleFlag(int row, int col) {
        ActionResult validation = validatePlayableCell(row, col);
        if (validation != null) {
            return validation;
        }
        if (opened[row][col]) {
            return ActionResult.error("No puedes poner bandera en una casilla abierta.");
        }

        flagged[row][col] = !flagged[row][col];
        return ActionResult.ok(flagged[row][col] ? "Bandera colocada." : "Bandera retirada.");
    }

    private ActionResult validatePlayableCell(int row, int col) {
        if (status != GameStatus.PLAYING) {
            return ActionResult.error("La partida ya termino. Crea una nueva con NUEVO.");
        }
        if (!isInside(row, col)) {
            return ActionResult.error("Coordenadas fuera del tablero.");
        }
        return null;
    }

    private void floodOpen(int startRow, int startCol) {
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startRow, startCol});

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int row = current[0];
            int col = current[1];

            if (!isInside(row, col) || opened[row][col] || flagged[row][col] || mines[row][col]) {
                continue;
            }

            opened[row][col] = true;
            openedSafeCells++;

            if (adjacentMines(row, col) == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) {
                            continue;
                        }
                        int nr = row + dr;
                        int nc = col + dc;
                        if (isInside(nr, nc) && !opened[nr][nc] && !flagged[nr][nc] && !mines[nr][nc]) {
                            queue.add(new int[]{nr, nc});
                        }
                    }
                }
            }
        }
    }

    private int adjacentMines(int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int nr = row + dr;
                int nc = col + dc;
                if (isInside(nr, nc) && mines[nr][nc]) {
                    count++;
                }
            }
        }
        return count;
    }

    private void updateWinCondition() {
        if (openedSafeCells == rows * cols - mineCount) {
            status = GameStatus.WON;
        }
    }

    private boolean isInside(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public synchronized String renderBoard() {
        boolean revealMines = status != GameStatus.PLAYING;
        StringBuilder sb = new StringBuilder();

        sb.append("    ");
        for (int col = 0; col < cols; col++) {
            sb.append(String.format("%2d ", col));
        }
        sb.append(System.lineSeparator());

        for (int row = 0; row < rows; row++) {
            sb.append(String.format("%2d |", row));
            for (int col = 0; col < cols; col++) {
                char symbol;
                if (opened[row][col]) {
                    if (mines[row][col]) {
                        symbol = '*';
                    } else {
                        int nearby = adjacentMines(row, col);
                        symbol = nearby == 0 ? ' ' : Character.forDigit(nearby, 10);
                    }
                } else if (flagged[row][col]) {
                    symbol = 'F';
                } else if (revealMines && mines[row][col]) {
                    symbol = '*';
                } else {
                    symbol = '#';
                }
                sb.append(String.format(" %c ", symbol));
            }
            sb.append('|').append(System.lineSeparator());
        }
        return sb.toString();
    }

    public synchronized GameStatus getStatus() {
        return status;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getMineCount() {
        return mineCount;
    }
}
