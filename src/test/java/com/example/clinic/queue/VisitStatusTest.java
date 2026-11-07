package com.example.clinic.queue;

import static com.example.clinic.queue.VisitStatus.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VisitStatusTest {

    @Test
    void legalTransitionsFollowTheStatusModel() {
        assertTrue(BOOKED.canMoveTo(WAITING));
        assertTrue(BOOKED.canMoveTo(CANCELLED));
        assertTrue(WAITING.canMoveTo(CALLED));
        assertTrue(CALLED.canMoveTo(IN_SERVICE));
        assertTrue(CALLED.canMoveTo(WAITING));
        assertTrue(IN_SERVICE.canMoveTo(COMPLETED));
    }

    @Test
    void illegalTransitionsAreRejected() {
        assertFalse(BOOKED.canMoveTo(COMPLETED));
        assertFalse(BOOKED.canMoveTo(IN_SERVICE));
        assertFalse(WAITING.canMoveTo(IN_SERVICE));
        assertFalse(IN_SERVICE.canMoveTo(CANCELLED));
        assertFalse(COMPLETED.canMoveTo(WAITING));
    }

    @Test
    void terminalStatusesHaveNoActions() {
        for (VisitStatus s : new VisitStatus[]{COMPLETED, CANCELLED, NO_SHOW}) {
            assertTrue(s.isTerminal());
            assertTrue(s.actions().isEmpty());
        }
    }
}
