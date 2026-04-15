package com.klasio.membership.domain.model;

public enum ProofStatus {
    PENDING,      // Uploaded, awaiting admin review
    APPROVED,     // Admin approved; membership activation was triggered
    REJECTED,     // Admin rejected; rejection reason available to student
    SUPERSEDED    // A newer proof was uploaded for the same membership; this one is archived
}
