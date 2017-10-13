package org.openhab.binding.yeelight.model;

import java.util.List;

public class YeelightGetPropsResponse {
    private int id;
    List<String> result;

    public int getId() {
        return id;
    }

    public List<String> getResult() {
        return result;
    }
}
