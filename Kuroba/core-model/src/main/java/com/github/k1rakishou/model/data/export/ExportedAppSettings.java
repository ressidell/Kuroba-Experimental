/*
 * KurobaEx - *chan browser https://github.com/K1rakishou/Kuroba-Experimental/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.k1rakishou.model.data.export;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ExportedAppSettings {
    // Don't forget to change this when changing any of the Export models.
    // Also, don't forget to handle the change in the onUpgrade or onDowngrade methods
    public static final int CURRENT_EXPORT_SETTINGS_VERSION = 1;

    @SerializedName("exported_settings_version")
    private int version;
    @SerializedName("exported_sites")
    private List<ExportedSite> exportedSites;
    //there aren't any exported pins here because the pins are stored inside each exported site
    //there also aren't any exported loadables because they are inside the exported pins
    @SerializedName("exported_boards")
    private List<ExportedBoard> exportedBoards;
    @SerializedName("exported_filters")
    private List<ExportedFilter> exportedFilters;
    @SerializedName("exported_post_hides")
    private List<ExportedPostHide> exportedPostHides;
    @SerializedName("exported_settings")
    @Nullable
    private String settings;

    public ExportedAppSettings(
            int version,
            List<ExportedSite> exportedSites,
            List<ExportedBoard> exportedBoards,
            List<ExportedFilter> exportedFilters,
            List<ExportedPostHide> exportedPostHides,
            @NonNull String settings
    ) {
        this.version = version;
        this.exportedSites = exportedSites;
        this.exportedBoards = exportedBoards;
        this.exportedFilters = exportedFilters;
        this.exportedPostHides = exportedPostHides;
        this.settings = settings;
    }

    public static ExportedAppSettings empty() {
        return new ExportedAppSettings(
                CURRENT_EXPORT_SETTINGS_VERSION,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                ""
        );
    }

    /**
     * Sites and boards are important we can't export almost nothing else without them
     * (probably only settings)
     */
    public boolean isEmpty() {
        return exportedSites.isEmpty() && exportedBoards.isEmpty() && (settings == null || settings.isEmpty());
    }

    public List<ExportedSite> getExportedSites() {
        return exportedSites;
    }

    public List<ExportedBoard> getExportedBoards() {
        return exportedBoards;
    }

    public List<ExportedFilter> getExportedFilters() {
        return exportedFilters;
    }

    public List<ExportedPostHide> getExportedPostHides() {
        return exportedPostHides;
    }

    public int getVersion() {
        return version;
    }

    @Nullable
    public String getSettings() {
        return settings;
    }

    public void setExportedSites(List<ExportedSite> exportedSites) {
        this.exportedSites = exportedSites;
    }

    public void setExportedBoards(List<ExportedBoard> exportedBoards) {
        this.exportedBoards = exportedBoards;
    }

    public void setExportedFilters(List<ExportedFilter> exportedFilters) {
        this.exportedFilters = exportedFilters;
    }

    public void setExportedPostHides(List<ExportedPostHide> exportedPostHides) {
        this.exportedPostHides = exportedPostHides;
    }

    public void setSettings(String settings) {
        throw new UnsupportedOperationException("Settings are only allowed to be set with the "
                + "constructor, and must be from ChanSettings.serializeToString().");
    }
}
