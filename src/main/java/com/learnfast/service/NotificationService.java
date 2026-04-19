package com.learnfast.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Async notification service demonstrating multithreading with synchronization.
 *
 * Producer-consumer: callers produce notifications into a BlockingQueue;
 * the store is a ConcurrentHashMap with synchronized list access per user.
 */
@Service
public class NotificationService {

    private final ConcurrentHashMap<Long, List<String>> notificationStore = new ConcurrentHashMap<>();

    // Producer-consumer: shared queue consumed by async dispatch
    private final BlockingQueue<NotificationEvent> pendingEvents = new LinkedBlockingQueue<>();

    /**
     * Asynchronously dispatches a notification to the given user.
     * Runs on the LearnFast-Async thread pool configured in AsyncConfig.
     *
     * SRP: this method owns only notification delivery logic.
     * Thread-safe: computeIfAbsent + synchronized block prevent concurrent writes.
     */
    @Async
    public CompletableFuture<Void> sendNotification(Long userId, String message) {
        NotificationEvent event = new NotificationEvent(userId, message);
        pendingEvents.offer(event);

        List<String> userNotifications = notificationStore
            .computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (userNotifications) {
            userNotifications.add(message);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns a snapshot of all pending notifications for the given user.
     * Uses synchronized copy to avoid ConcurrentModificationException.
     */
    public List<String> getNotifications(Long userId) {
        List<String> stored = notificationStore.get(userId);
        if (stored == null) {
            return Collections.emptyList();
        }
        synchronized (stored) {
            return new ArrayList<>(stored);
        }
    }

    /**
     * Clears all notifications for a user.
     * Synchronized on the method to prevent concurrent clear + add races.
     */
    public synchronized void clearNotifications(Long userId) {
        notificationStore.remove(userId);
    }

    /**
     * Returns the number of unprocessed events still in the queue.
     * Useful for observability / health checks.
     */
    public int getPendingEventCount() {
        return pendingEvents.size();
    }

    // Immutable value object for the producer-consumer queue
    private static final class NotificationEvent {
        final Long userId;
        final String message;

        NotificationEvent(Long userId, String message) {
            this.userId = userId;
            this.message = message;
        }

        @Override
        public String toString() {
            return "NotificationEvent{userId=" + userId + ", message='" + message + "'}";
        }
    }
}
