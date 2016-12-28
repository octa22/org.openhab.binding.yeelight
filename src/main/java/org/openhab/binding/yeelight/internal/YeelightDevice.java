package org.openhab.binding.yeelight.internal;

/**
 * Created by Ondřej Pečta on 25. 12. 2016.
 * intended for future use...
 */
public class YeelightDevice {
    private String id;
    private String location;
    private String model;
    private String support;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(String support) {
        this.support = support;
    }

    public YeelightDevice(String id, String location, String model, String support) {
        this.id = id;
        this.location = location;
        this.model = model;
        this.support = support;
    }

    @Override
    public String toString() {
        return "{" +
                "\nid='" + id + '\'' +
                "\nlocation='" + location + '\'' +
                ",\nmodel='" + model + '\'' +
                ",\nsupport='" + support + '\'' +
                "\n}";
    }
}
