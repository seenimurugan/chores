package com.nila.chores.notification;

/**
 * Immutable result of a single channel send attempt.
 */
public record SendResult(boolean ok, String error) {
    public static SendResult success() { return new SendResult(true, null); }
    public static SendResult fail(String error) { return new SendResult(false, error); }
}
