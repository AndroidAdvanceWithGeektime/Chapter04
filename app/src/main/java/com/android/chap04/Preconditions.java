package com.android.chap04;

/**
 * @author zhoujun
 * @date 19-1-23
 * @email jun_zhou1@hnair.com
 */
public class Preconditions {
    /**
     * Returns instance unless it's null.
     *
     * @throws NullPointerException if instance is null
     */
    static <T> T checkNotNull(T instance, String name) {
        if (instance == null) {
            throw new NullPointerException(name + " must not be null");
        }
        return instance;
    }

    private Preconditions() {
        throw new AssertionError();
    }
}
