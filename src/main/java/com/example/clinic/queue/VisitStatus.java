package com.example.clinic.queue;

import java.util.List;

/**
 * Single source of truth for the status machine. The server only ever applies transitions
 * that appear in {@link #actions()}.
 */
public enum VisitStatus {
    BOOKED, WAITING, CALLED, IN_SERVICE, COMPLETED, CANCELLED, NO_SHOW;

    public record Action(VisitStatus target, String label) {}

    public List<Action> actions() {
        return switch (this) {
            case BOOKED -> List.of(new Action(WAITING, "Check in"), new Action(NO_SHOW, "No-show"), new Action(CANCELLED, "Cancel"));
            case WAITING -> List.of(new Action(CALLED, "Call"), new Action(NO_SHOW, "No-show"), new Action(CANCELLED, "Cancel"));
            case CALLED -> List.of(new Action(IN_SERVICE, "Start visit"), new Action(WAITING, "Skip back to queue"), new Action(NO_SHOW, "No-show"));
            case IN_SERVICE -> List.of(new Action(COMPLETED, "Complete"));
            case COMPLETED, CANCELLED, NO_SHOW -> List.of();
        };
    }

    public boolean canMoveTo(VisitStatus target) {
        return actions().stream().anyMatch(a -> a.target() == target);
    }

    public boolean isTerminal() {
        return actions().isEmpty();
    }
}
