package com.example.flashsale.web;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final Object A = new Object();
    private final Object B = new Object();

    @PostMapping("/deadlock")
    public String deadlock() {
        Thread t1 = new Thread(() -> {
            synchronized (A) {
                sleep(50);
                synchronized (B) {
                    // never reached if deadlocked
                }
            }
        }, "deadlock-t1");

        Thread t2 = new Thread(() -> {
            synchronized (B) {
                sleep(50);
                synchronized (A) {
                    // never reached if deadlocked
                }
            }
        }, "deadlock-t2");

        t1.start();
        t2.start();
        return "Triggered deadlock threads. Use jstack to confirm.";
    }

    @PostMapping("/deadlock/fix")
    public String fix() {
        Thread t1 = new Thread(() -> lockInOrder(A, B), "fix-t1");
        Thread t2 = new Thread(() -> lockInOrder(A, B), "fix-t2");
        t1.start();
        t2.start();
        return "Deadlock fixed by consistent lock ordering (A then B).";
    }

    private void lockInOrder(Object first, Object second) {
        synchronized (first) {
            sleep(50);
            synchronized (second) { }
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
