package com.mycompany.snakegame.util;

import java.util.function.IntConsumer;

public class ClockThread extends Thread {

    private final IntConsumer onTick;
    private final Object lock = new Object();
    private int seconds;
    private volatile boolean counting;

    public ClockThread(IntConsumer onTick) {
        super("snake-clock");
        setDaemon(true);
        this.onTick = onTick;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                Thread.sleep(1000);
                if (counting) {
                    int now;
                    synchronized (lock) {
                        now = ++seconds;
                    }
                    onTick.accept(now);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   
        } finally {
            System.out.println("[ClockThread] stopped");
        }
    }

    public void resetSeconds() {
        synchronized (lock) {
            seconds = 0;
        }
    }

    public void setCounting(boolean counting) {
        this.counting = counting;
    }
}
