package org.openhab.binding.yeelight.internal;

import org.openhab.core.binding.BindingConfig;

/**
 * This is a helper class holding binding specific configuration details
 *
 * @author Ondrej Pecta
 * @since 1.9.0
 */
class YeelightBindingConfig implements BindingConfig {
    // put member fields here which holds the parsed values
    private String location;
    private String action;

    public String getAction() {
        return action;
    }

    public String getLocation() {
        return location;
    }

    public YeelightBindingConfig(String location, String action) {
        this.location = location;
        this.action = action;
    }
}
