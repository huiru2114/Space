package com.example.space;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized manager for authentication state across the app
 */
public class AuthStateManager {
    private static AuthStateManager instance;
    private final List<AuthStateListener> listeners = new ArrayList<>();
    private boolean isAuthenticated = false;

    // Private constructor to enforce singleton pattern
    private AuthStateManager() {
    }

    public static synchronized AuthStateManager getInstance() {
        if (instance == null) {
            instance = new AuthStateManager();
        }
        return instance;
    }

    public interface AuthStateListener {
        void onAuthStateChanged(boolean isAuthenticated);
    }

    public void addListener(AuthStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            // Immediately notify new listeners of current state
            listener.onAuthStateChanged(isAuthenticated);
        }
    }

    public void removeListener(AuthStateListener listener) {
        listeners.remove(listener);
    }

    public void setAuthenticated(boolean authenticated) {
        if (this.isAuthenticated != authenticated) {
            this.isAuthenticated = authenticated;
            notifyAuthChanged();
        }
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    private void notifyAuthChanged() {
        // Create a copy of the list to avoid ConcurrentModificationException
        for (AuthStateListener listener : new ArrayList<>(listeners)) {
            listener.onAuthStateChanged(isAuthenticated);
        }
    }
}