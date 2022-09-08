package org.embl.mobie.io.ome.zarr.util;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class ZarrAxis {
    private transient final int index;
    private final String name;
    private final String type;
    private String unit;

    public ZarrAxis(int index, String name, String type, String unit) {
        this.index = index;
        this.name = name;
        this.type = type;
        this.unit = unit;
    }

    public ZarrAxis(int index, String name, String type) {
        this.index = index;
        this.name = name;
        this.type = type;
    }

    public static JsonElement convertToJson(List<ZarrAxis> zarrAxes) {
        StringBuilder axes = new StringBuilder();
        axes.append("[");
        for (ZarrAxis axis : zarrAxes) {
            axes.append("\"").append(axis.getName()).append("\"");
            if (axis.getIndex() < zarrAxes.size() - 1) {
                axes.append(",");
            }
        }
        axes.append("]");
        Gson gson = new Gson();
        return gson.fromJson(axes.toString(), JsonElement.class);
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
