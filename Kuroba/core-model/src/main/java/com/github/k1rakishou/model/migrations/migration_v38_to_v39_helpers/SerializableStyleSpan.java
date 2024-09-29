package com.github.k1rakishou.model.migrations.migration_v38_to_v39_helpers;

import com.google.gson.annotations.SerializedName;

public class SerializableStyleSpan {
    @SerializedName("style")
    private int style;

    public SerializableStyleSpan(int style) {
        this.style = style;
    }

    public int getStyle() {
        return style;
    }
}
