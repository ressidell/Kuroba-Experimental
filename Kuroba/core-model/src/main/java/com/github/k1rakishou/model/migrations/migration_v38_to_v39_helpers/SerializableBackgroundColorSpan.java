package com.github.k1rakishou.model.migrations.migration_v38_to_v39_helpers;

import com.google.gson.annotations.SerializedName;

public class SerializableBackgroundColorSpan {
    @SerializedName("background_color")
    private int backgroundColor;

    public SerializableBackgroundColorSpan(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }
}
