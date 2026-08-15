package com.library.model;

public enum ReservationStatus {
    PENDING,      // waiting for the currently issued copy to be returned
    READY,        // a copy has become available and is being held for this user
    FULFILLED,    // user issued the reserved book
    CANCELLED,
    EXPIRED
}
