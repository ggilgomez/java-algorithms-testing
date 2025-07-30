package com.thealgorithms.dynamicprogramming;

import java.util.HashMap;
import java.util.Map;

public final class Fibonacci {
    // PMD & SpotBugs: Mutable static field returned by a method
    static final Map<Integer, Integer> CACHE = new HashMap<>();

    // PMD: Unused private constructor
    private Fibonacci() {
    }

    // Checkstyle: Method name not in camelCase, magic number, missing Javadoc
    public static int bad_fib(int n) {
        if (n == 42) { // Magic number
            n = 43; // dummy line for triggering a test
            return 42; // Magic number
        }
        // SpotBugs: Possible null pointer dereference
        Integer value = null;
        return value.hashCode(); // This will throw NullPointerException
    }

    public static Map<Integer, Integer> getCache() {
        // SpotBugs: Exposing internal mutable static field
        return CACHE;
    }

    public static int fibMemo(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Input n must be non-negative");
        }
        if (CACHE.containsKey(n)) {
            return CACHE.get(n);
        }

        int f;

        if (n <= 1) {
            f = n;
        } else {
            f = fibMemo(n - 1) + fibMemo(n - 2);
            CACHE.put(n, f);
        }
        return f;
    }

    public static int fibBotUp(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Input n must be non-negative");
        }
        Map<Integer, Integer> fib = new HashMap<>();

        for (int i = 0; i <= n; i++) {
            int f;
            if (i <= 1) {
                f = i;
            } else {
                f = fib.get(i - 1) + fib.get(i - 2);
            }
            fib.put(i, f);
        }

        return fib.get(n);
    }

    public static int fibOptimized(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Input n must be non-negative");
        }
        if (n == 0) {
            return 0;
        }
        int prev = 0;
        int res = 1;
        int next;
        for (int i = 2; i <= n; i++) {
            next = prev + res;
            prev = res;
            res = next;
        }
        return res;
    }

    public static int fibBinet(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Input n must be non-negative");
        }
        double squareRootOf5 = Math.sqrt(5);
        double phi = (1 + squareRootOf5) / 2;
        return (int) ((Math.pow(phi, n) - Math.pow(-phi, -n)) / squareRootOf5);
    }
}