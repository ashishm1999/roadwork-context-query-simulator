package org.coaas.roadwork.utils;

/**
 * Callback interface subscribers implement to receive delivered messages.
 */
@FunctionalInterface
public interface OnMessage {
    void handle(Message message);
}
