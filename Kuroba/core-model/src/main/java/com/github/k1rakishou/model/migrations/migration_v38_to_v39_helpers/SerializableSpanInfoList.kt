package com.github.k1rakishou.model.migrations.migration_v38_to_v39_helpers

import com.google.gson.annotations.SerializedName

data class SerializableSpanInfoList(
  @SerializedName("serializable_span_info_list")
  val spanInfoList: List<SerializableSpanInfo>
)