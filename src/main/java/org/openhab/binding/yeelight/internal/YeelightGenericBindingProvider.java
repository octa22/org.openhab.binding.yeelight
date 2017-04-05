/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.yeelight.internal;

import org.openhab.binding.yeelight.YeelightBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.ColorItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author Ondrej Pecta
 * @since 1.9.0
 */
public class YeelightGenericBindingProvider extends AbstractGenericBindingProvider implements YeelightBindingProvider {

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "yeelight";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(item instanceof SwitchItem || item instanceof DimmerItem || item instanceof ColorItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of location '" + item.getClass().getSimpleName()
					+ "', only Switch-, Dimmer- and ColorItems are allowed - please check your *.items configuration");
		}
	}

	public YeelightBindingConfig getItemConfig(String itemName) {
		final YeelightBindingConfig config = (YeelightBindingConfig) this.bindingConfigs.get(itemName);
		return config;
	}

	/*
	public String getItemLocation(String itemName) {
		final YeelightBindingConfig config = (YeelightBindingConfig) this.bindingConfigs.get(itemName);
		return config != null ? (config.getLocation()) : null;
	}

    public String getItemAction(String itemName) {
        final YeelightBindingConfig config = (YeelightBindingConfig) this.bindingConfigs.get(itemName);
        return config != null ? (config.getAction()) : null;
    }*/

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		String type = bindingConfig;
		String command = "set_power";

		if(bindingConfig.contains("#"))
		{
		    int pos = bindingConfig.indexOf("#");
			type = bindingConfig.substring(0, pos);
			command = bindingConfig.substring(pos + 1);
		}


		YeelightBindingConfig config = new YeelightBindingConfig(type, command);
		
		//parse bindingconfig here ...

		addBindingConfig(item, config);
	}

}
