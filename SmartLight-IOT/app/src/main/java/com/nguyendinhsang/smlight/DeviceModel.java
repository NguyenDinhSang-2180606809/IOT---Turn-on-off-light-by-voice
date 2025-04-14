package com.nguyendinhsang.smlight;

public class DeviceModel {
    private String name;
    private int iconResId;
    private String room;

    public DeviceModel(String name, int iconResId, String room) {
        this.name = name;
        this.iconResId = iconResId;
        this.room = room;

    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }
    public String getRoom() {
        return room;
    }

}
