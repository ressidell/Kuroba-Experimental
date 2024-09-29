package com.github.k1rakishou.model.migrations.migration_v38_to_v39_helpers;

import com.google.gson.annotations.SerializedName;

public class SerializableTypefaceSpan {
    @SerializedName("family")
    private String family;

    public SerializableTypefaceSpan(String family) {
        this.family = family;
    }

    public String getFamily() {
        return family;
    }
}
