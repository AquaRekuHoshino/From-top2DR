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

    private static final double SPEED = 270;
    private static final double PLAYER_SIZE = 40;
    private static final double BULLET_SIZE = 10;
    private static final double BULLET_SPEED = 300;

    private boolean up, down, left, right;
    private boolean gameOver = false;
    private boolean canWin = false;

    private int deathCount = 0;

    private final List<Entity> bullets = new ArrayList<>();
    private final Map<Entity, Point2D> bulletVelocity = new HashMap<>();

    private Text infoText;
    private Button respawnButton;


    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("From-top2DR");
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setAppIcon("icon.png");
        settings.setVersion("Alpha 1.3.0");
    }

    @Override
    protected void initGame() {
        resetGame();
        initUI();
    }

    public void initUI() {
        infoText = new Text();
        infoText.setFill(Color.WHITE);
        infoText.setTranslateX(10);
        infoText.setTranslateY(20);
        addUINode(infoText);
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

        updateUI();

        if (gameOver)
            return;

        movePlayer(tpf);
        updateBullets(tpf);

        // colisión jugador - enemigo
        if (player.getBoundingBoxComponent().isCollidingWith(enemy.getBoundingBoxComponent())) {
            if (canWin) {
                winGame();
            }
        }
    }

    private void updateUI() {
        if (deathCount < 10) {
            infoText.setText("Muertes: " + deathCount + " / 10");
        } else {
            infoText.setText("Muertes: " + deathCount + " | TOCA EL CUBO ROJO PARA GANAR");
            canWin = true;
        }
    }


    private void movePlayer(double tpf) {
        double dx = 0;
        double dy = 0;

        if (up) dy -= SPEED;
        if (down) dy += SPEED;
        if (left) dx -= SPEED;
        if (right) dx += SPEED;

        double newX = clamp(player.getX() + dx * tpf, 0, getAppWidth() - PLAYER_SIZE);
        double newY = clamp(player.getY() + dy * tpf, 0, getAppHeight() - PLAYER_SIZE);

        player.setPosition(newX, newY);
    }

    private void updateBullets(double tpf) {
        Iterator<Entity> it = bullets.iterator();

        while (it.hasNext()) {
            Entity bullet = it.next();
            Point2D v = bulletVelocity.get(bullet);

            bullet.translate(v.multiply(tpf));

            if (bullet.getBoundingBoxComponent().isCollidingWith(player.getBoundingBoxComponent())) {
                playerDies();
                return;
            }

            if (bullet.getX() < -20 || bullet.getX() > getAppWidth() + 20
                    || bullet.getY() < -20 || bullet.getY() > getAppHeight() + 20) {

                bullet.removeFromWorld();
                bulletVelocity.remove(bullet);
                it.remove();
            }
        }
    }

    private void playerDies() {
        gameOver = true;
        deathCount++;

        player.removeFromWorld();

        Text deathText = new Text("HAS MUERTO");
        deathText.setFill(Color.RED);
        deathText.setScaleX(3);
        deathText.setScaleY(3);
        deathText.setTranslateX(getAppWidth() / 2 - 120);
        deathText.setTranslateY(getAppHeight() / 2 - 60);

        respawnButton = new Button("REAPARECER");
        respawnButton.setTranslateX(getAppWidth() / 2 - 60);
        respawnButton.setTranslateY(getAppHeight() / 2);
        respawnButton.setOnAction(e -> {
            removeUINode(deathText);
            removeUINode(respawnButton);
            resetGame();
        });

        addUINode(deathText);
        addUINode(respawnButton);
    }

    private void winGame() {
        gameOver = true;

        Text winText = new Text("¡GANASTE!");
        winText.setFill(Color.LIME);
        winText.setScaleX(3);
        winText.setScaleY(3);
        winText.setTranslateX(getAppWidth() / 2 - 140);
        winText.setTranslateY(getAppHeight() / 2);



        addUINode(winText);
    }

    private void resetGame() {
        gameOver = false;

        bullets.forEach(Entity::removeFromWorld);
        bullets.clear();
        bulletVelocity.clear();

        getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);

        spawnPlayer();
        spawnEnemy();
        startEnemyShooting();
    }

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

    private void startEnemyShooting() {
        run(() -> {
            if (!gameOver)
                shootBullet();
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
