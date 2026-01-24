package Aqua.Game;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;

import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.*;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.sun.javafx.util.Utils.clamp;

public class Main extends GameApplication {

    private Entity player;
    private Entity enemy;

    private static final double SPEED = 250;
    private static final double PLAYER_SIZE = 36;
    private static final double BULLET_SIZE = 10;
    private static final double BULLET_SPEED = 300;
    private static final String version = "Alpha 1.2.1";
    private boolean up, down, left, right;
    private boolean gameOver = false;

    private final List<Entity> bullets = new ArrayList<>();
    private final Map<Entity, Point2D> bulletVelocity = new HashMap<>();

    private Text gameOverText;
    private Button respawnButton;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("From-top");
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setVersion(version);
    }

    @Override
    protected void initGame() {
        resetGame();
    }

    @Override
    protected void initInput() {

        getInput().addAction(new UserAction("Up") {
            @Override protected void onAction() { up = true; }
            @Override protected void onActionEnd() { up = false; }
        }, KeyCode.W);

        getInput().addAction(new UserAction("Down") {
            @Override protected void onAction() { down = true; }
            @Override protected void onActionEnd() { down = false; }
        }, KeyCode.S);

        getInput().addAction(new UserAction("Left") {
            @Override protected void onAction() { left = true; }
            @Override protected void onActionEnd() { left = false; }
        }, KeyCode.A);

        getInput().addAction(new UserAction("Right") {
            @Override protected void onAction() { right = true; }
            @Override protected void onActionEnd() { right = false; }
        }, KeyCode.D);
    }

    @Override
    protected void onUpdate(double tpf) {

        if (gameOver)
            return;

        // ===== movimiento jugador =====
        double dx = 0;
        double dy = 0;

        if (up) dy -= SPEED;
        if (down) dy += SPEED;
        if (left) dx -= SPEED;
        if (right) dx += SPEED;

        double newX = clamp(player.getX() + dx * tpf, 0, getAppWidth() - PLAYER_SIZE);
        double newY = clamp(player.getY() + dy * tpf, 0, getAppHeight() - PLAYER_SIZE);

        player.setPosition(newX, newY);

        // ===== mover balas + colisión =====
        Iterator<Entity> it = bullets.iterator();
        while (it.hasNext()) {
            Entity bullet = it.next();
            Point2D v = bulletVelocity.get(bullet);

            bullet.translate(v.multiply(tpf));

            // 💥 COLISIÓN REAL
            if (bullet.getBoundingBoxComponent()
                    .isCollidingWith(player.getBoundingBoxComponent())) {

                playerDies();
                return;
            }

            // eliminar fuera de pantalla
            if (bullet.getX() < -20 || bullet.getX() > getAppWidth() + 20
                    || bullet.getY() < -20 || bullet.getY() > getAppHeight() + 20) {

                bullet.removeFromWorld();
                bulletVelocity.remove(bullet);
                it.remove();
            }
        }
    }

    // =====================
    // GAME FLOW
    // =====================

    private void playerDies() {
        gameOver = true;

        player.removeFromWorld();

        gameOverText = new Text("GAME OVER");
        gameOverText.setFill(Color.RED);
        gameOverText.setScaleX(3);
        gameOverText.setScaleY(3);
        gameOverText.setTranslateX(280);
        gameOverText.setTranslateY(240);

        respawnButton = new Button("REAPARECER");
        respawnButton.setTranslateX(350);
        respawnButton.setTranslateY(300);
        respawnButton.setOnAction(e -> resetGame());

        addUINode(gameOverText);
        addUINode(respawnButton);
    }

    private void resetGame() {

        gameOver = false;

        getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);
        bullets.clear();
        bulletVelocity.clear();

        if (gameOverText != null) removeUINode(gameOverText);
        if (respawnButton != null) removeUINode(respawnButton);

        spawnPlayer();
        spawnEnemy();
        startEnemyShooting();
    }

    // =====================
    // SPAWNS
    // =====================

    private void spawnPlayer() {
        player = entityBuilder()
                .at(100, 300)
                .viewWithBBox(new Rectangle(PLAYER_SIZE, PLAYER_SIZE, Color.CORNFLOWERBLUE))
                .buildAndAttach();
    }

    private void spawnEnemy() {
        enemy = entityBuilder()
                .at(600, 300)
                .viewWithBBox(new Rectangle(40, 40, Color.DARKRED))
                .buildAndAttach();
    }

    // =====================
    // DISPAROS
    // =====================

    private void startEnemyShooting() {
        run(() -> {
            if (!gameOver) shootBullet();
        }, Duration.seconds(1));
    }

    private void shootBullet() {

        Entity bullet = entityBuilder()
                .at(enemy.getCenter().subtract(BULLET_SIZE / 2, BULLET_SIZE / 2))
                .viewWithBBox(new Rectangle(BULLET_SIZE, BULLET_SIZE, Color.GRAY))
                .buildAndAttach();

        Point2D direction = player.getCenter()
                .subtract(enemy.getCenter())
                .normalize()
                .multiply(BULLET_SPEED);

        bullets.add(bullet);
        bulletVelocity.put(bullet, direction);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
