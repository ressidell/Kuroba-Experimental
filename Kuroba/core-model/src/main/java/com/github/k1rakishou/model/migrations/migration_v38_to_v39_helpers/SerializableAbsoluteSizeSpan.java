package com.github.k1rakishou.model.migrations.migration_v38_to_v39_helpers;

import com.google.gson.annotations.SerializedName;

public class SerializableAbsoluteSizeSpan {
    @SerializedName("size")
    private int size;

    public SerializableAbsoluteSizeSpan(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
