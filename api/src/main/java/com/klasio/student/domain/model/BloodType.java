package com.klasio.student.domain.model;

public enum BloodType {
    O_POSITIVE("O+"),
    O_NEGATIVE("O-"),
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-");

    private final String label;

    BloodType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static BloodType fromLabel(String label) {
        for (BloodType bt : values()) {
            if (bt.label.equals(label)) return bt;
        }
        throw new IllegalArgumentException("Unknown blood type: " + label);
    }
}
