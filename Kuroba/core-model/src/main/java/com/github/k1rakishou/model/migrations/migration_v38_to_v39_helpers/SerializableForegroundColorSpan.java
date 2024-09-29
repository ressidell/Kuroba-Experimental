package com.github.k1rakishou.model.migrations.migration_v38_to_v39_helpers;

import com.google.gson.annotations.SerializedName;

public class SerializableForegroundColorSpan {
    @SerializedName("foreground_color")
    private int foregroundColor;

    public SerializableForegroundColorSpan(int foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public int getForegroundColor() {
        return foregroundColor;
    }
}
