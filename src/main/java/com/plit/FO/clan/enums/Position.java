package com.plit.FO.clan.enums;

public enum Position {
    TOP("탑"),
    JUNGLE("정글"),
    MID("미드"),
    ADC("원딜"),
    SUPPORT("서포터"),
    ALL("올라운더");

    private final String label;

    Position(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Position fromString(String value) {
        for (Position pos : Position.values()) {
            if (pos.name().equalsIgnoreCase(value)) {
                return pos;
            }
        }
        throw new IllegalArgumentException("Unknown position: " + value);
    }
}