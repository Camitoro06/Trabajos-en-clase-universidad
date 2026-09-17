package com.icesi.buscaminas.game;

public class GameSelfTest {
    public static void main(String[] args) {
        MinesweeperGame game = new MinesweeperGame(9, 9, 10);

        assert game.getRows() == 9;
        assert game.getCols() == 9;
        assert game.getMineCount() == 10;
        assert game.getStatus() == GameStatus.PLAYING;

        ActionResult outside = game.openCell(-1, 0);
        assert !outside.success();

        ActionResult flag = game.toggleFlag(0, 0);
        assert flag.success();

        ActionResult blocked = game.openCell(0, 0);
        assert !blocked.success();

        System.out.println("GameSelfTest OK");
    }
}
