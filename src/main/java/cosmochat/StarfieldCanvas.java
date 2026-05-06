package cosmochat;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StarfieldCanvas extends Canvas {

    private static final int STAR_COUNT = 300;
    private final List<Star> stars = new ArrayList<>();
    private final Random random = new Random();
    
    private AnimationTimer animator;
    private double mouseX = 0, mouseY = 0;
    private double targetMouseX = 0, targetMouseY = 0;
    private long startTime = 0;

    private static class Star {
        double x, y;
        double size;
        double opacity;
        double twinkleSpeed;
        double twinklePhase;
        double layer;

        Star(double x, double y, double layer, Random rand) {
            this.x = x;
            this.y = y;
            this.layer = layer;
            this.size = Math.max(0.3, layer * 1.5 + rand.nextDouble() * 0.5);
            this.opacity = 0.2 + layer * 0.6;
            this.twinkleSpeed = 0.5 + rand.nextDouble() * 2.0;
            this.twinklePhase = rand.nextDouble() * 360;
        }
    }

    public StarfieldCanvas() {
        for (int i = 0; i < STAR_COUNT; i++) {
            double layer = random.nextDouble();
            stars.add(new Star(
                random.nextDouble(), 
                random.nextDouble(), 
                layer, 
                random
            ));
        }

        this.setOnMouseMoved(event -> {
            targetMouseX = (event.getX() / getWidth() - 0.5) * 2;
            targetMouseY = (event.getY() / getHeight() - 0.5) * 2;
        });
        
        this.setMouseTransparent(true); 
    }

    public void startAnimation() {
        startTime = System.currentTimeMillis();
        animator = new AnimationTimer() {
            @Override
            public void handle(long now) {
                draw();
            }
        };
        animator.start();
    }

    public void stopAnimation() {
        if (animator != null) animator.stop();
    }

    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        
        if (w <= 0 || h <= 0) return;

        mouseX += (targetMouseX - mouseX) * 0.05;
        mouseY += (targetMouseY - mouseY) * 0.05;

        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.web("#070709"));
        gc.fillRect(0, 0, w, h);

        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        double globalFade = Math.min(1.0, elapsedSeconds / 1.5);

        for (Star star : stars) {
            double offsetX = mouseX * star.layer * 20;
            double offsetY = mouseY * star.layer * 20;
            
            double sx = star.x * w + offsetX;
            double sy = star.y * h + offsetY;

            double twinkle = Math.sin(elapsedSeconds * star.twinkleSpeed + star.twinklePhase);
            double currentOpacity = star.opacity * (0.7 + 0.3 * twinkle) * globalFade;
            double currentSize = star.size * (0.9 + 0.1 * twinkle);

            gc.beginPath();
            gc.arc(sx, sy, currentSize, currentSize, 0, 360);
            gc.setFill(Color.rgb(255, 255, 255, currentOpacity));
            gc.fill();
            
            if (star.layer > 0.8 && currentOpacity > 0.5) {
                gc.beginPath();
                gc.arc(sx, sy, currentSize * 3, currentSize * 3, 0, 360);
                gc.setFill(Color.rgb(255, 255, 255, currentOpacity * 0.08));
                gc.fill();
            }
        }

        drawNebula(gc, w * 0.85, h * 0.2, 200, Color.web("rgba(255,255,255,0.015)"), globalFade);
        drawNebula(gc, w * 0.2, h * 0.8, 150, Color.web("rgba(180,180,200,0.012)"), globalFade);
    }

    private void drawNebula(GraphicsContext gc, double x, double y, double r, Color color, double fade) {
        gc.save();
        gc.setGlobalAlpha(fade * 0.5);
        gc.setFill(color);
        gc.fillOval(x - r, y - r, r * 2, r * 2);
        gc.restore();
    }
}