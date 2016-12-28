/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.yeelight;

import org.openhab.binding.yeelight.internal.YeelightGenericBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.binding.BindingProvider;

/**
 * @author Ondrej Pecta
 * @since 1.9.0
 */
public interface YeelightBindingProvider extends BindingProvider {

    BindingConfig getItemConfig(String itemName);

}
