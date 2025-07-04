package com.plit.FO.party;

public enum PositionEnum {
    TOP("탑"),
    JUNGLE("정글"),
    MID("미드"),
    ADC("원딜"),
    SUPPORT("서포터"),
    ALL("전체");

    private final String displayName;

    PositionEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
