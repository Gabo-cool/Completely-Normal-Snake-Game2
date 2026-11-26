package com.mycompany.snakegame.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public abstract class Food extends GameObject {

    private static int spawned = 0;

    protected Food(Cell position) {
        super(position);  
        spawned++;
    }

    public static int getSpawned() {
        return spawned;
    }

    public abstract int getPoints();

    public abstract Color getColor();

    public String getLabel() {
        return "Apple";
    }

    @Override
    public void draw(Graphics2D g, int cell) {
        int x = getPosition().col() * cell;
        int y = getPosition().row() * cell;
        int pad = 3;
        int size = cell - pad * 2;

        g.setColor(new Color(0, 0, 0, 45));                       // shadow
        g.fillOval(x + pad + 2, y + pad + 4, size, size);
        g.setColor(getColor());                                   // body 
        g.fillOval(x + pad, y + pad + 2, size, size);
        g.setColor(new Color(255, 255, 255, 90));                 // highlight
        g.fillOval(x + pad + 3, y + pad + 4, size / 2, size / 2);

        g.setColor(new Color(90, 55, 25));                        // stem
        g.setStroke(new BasicStroke(3));
        g.drawLine(x + cell / 2, y + pad + 3, x + cell / 2 + 2, y + 1);
        g.setColor(new Color(35, 115, 45));                       // leaf
        g.fillOval(x + cell / 2 + 1, y, 10, 6);
    }

    @Override
    public String toString() {
        return getLabel() + "@" + getPosition();
    }
}
