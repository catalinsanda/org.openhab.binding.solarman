package org.openhab.binding.solarman.internal.state;

public class LoggerState {
    public final static Integer NO_FAILED_REQUESTS = 3;
    private State state = State.ONLINE; // Let's assume we're online initially
    private int offlineTryCount = 0;

    public void setOnline() {
        state = State.ONLINE;
        offlineTryCount = 0;
    }

    public void setPossiblyOffline() {
        state = ++offlineTryCount < NO_FAILED_REQUESTS ? State.LIMBO : State.OFFLINE;
    }

    public boolean isOffline() {
        return state == State.OFFLINE;
    }

    public boolean isJustBecameOffline() {
        return state == State.OFFLINE && offlineTryCount == NO_FAILED_REQUESTS;
    }

    public enum State {
        ONLINE,
        LIMBO,
        OFFLINE,
    }
}
