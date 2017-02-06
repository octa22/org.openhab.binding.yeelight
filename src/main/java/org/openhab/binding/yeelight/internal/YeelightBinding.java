/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.yeelight.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.openhab.binding.yeelight.YeelightBindingProvider;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Hashtable;
import java.util.Map;


/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 *
 * @author Ondrej Pecta
 * @since 1.9.0
 */
public class YeelightBinding extends AbstractActiveBinding<YeelightBindingProvider> {

    private static final Logger logger =
            LoggerFactory.getLogger(YeelightBinding.class);
    private static final String MCAST_ADDR = "239.255.255.250";
    private static final int MCAST_PORT = 1982;
    private final int BUFFER_LENGTH = 1024;
    private long msgid = 0;

    //Constants
    private final String RESULT = "result";
    private final String TOGGLE = "toggle";
    private final String SMOOTH = "smooth";
    private final String GET_PROP = "get_prop";
    private final String SET_BRIGHT = "set_bright";
    private final String SET_POWER = "set_power";
    private final String SET_CT = "set_ct";
    private final String SET_HSB = "set_hsb";
    private final String SET_RGB = "set_rgb";

    //thread
    private Thread thread;

    //Socket
    private MulticastSocket socket = null;

    //Gson parser
    private JsonParser parser = new JsonParser();

    //devices
    Hashtable<String, YeelightDevice> devices;

    byte[] buffer = new byte[BUFFER_LENGTH];
    DatagramPacket dgram = new DatagramPacket(buffer, buffer.length);


    /**
     * The BundleContext. This is only valid when the bundle is ACTIVE. It is set in the activate()
     * method and must not be accessed anymore once the deactivate() method was called or before activate()
     * was called.
     */
    private BundleContext bundleContext;

    private ItemRegistry itemRegistry;


    /**
     * the refresh interval which is used to poll values from the Yeelight
     * server (optional, defaults to 60000ms)
     */
    private long refreshInterval = 60000;


    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }


    public YeelightBinding() {
    }


    /**
     * Called by the SCR to activate the component with its configuration read from CAS
     *
     * @param bundleContext BundleContext of the Bundle that defines this component
     * @param configuration Configuration properties for this component obtained from the ConfigAdmin service
     */
    public void activate(final BundleContext bundleContext, final Map<String, Object> configuration) {
        this.bundleContext = bundleContext;

        // the configuration is guaranteed not to be null, because the component definition has the
        // configuration-policy set to require. If set to 'optional' then the configuration may be null


        // to override the default refresh interval one has to add a
        // parameter to openhab.cfg like <bindingName>:refresh=<intervalInMs>
        String refreshIntervalString = (String) configuration.get("refresh");
        if (StringUtils.isNotBlank(refreshIntervalString)) {
            refreshInterval = Long.parseLong(refreshIntervalString);
        }

        devices = new Hashtable<>();
        setupSocket();
        setProperlyConfigured(socket != null);
    }

    private void setupSocket() {
        try {
            socket = new MulticastSocket(); // must bind receive side
            socket.joinGroup(InetAddress.getByName(MCAST_ADDR));
        } catch (IOException e) {
            logger.error(e.toString());
        }

        thread = new Thread(new Runnable() {
            public void run() {
                receiveData(socket, dgram);
            }
        });
        thread.start();
    }

    private void receiveData(MulticastSocket socket, DatagramPacket dgram) {

        try {
            while (true) {
                socket.receive(dgram);
                String sentence = new String(dgram.getData(), 0,
                        dgram.getLength());

                logger.debug("Yeelight received packet: " + sentence);

                if (isOKPacket(sentence) || isNotifyPacket(sentence)) {
                    String[] lines = sentence.split("\n");
                    String id = "";
                    String location = "";
                    String model = "";
                    String support = "";
                    for (String line : lines) {
                        line = line.replace("\r", "");
                        line = line.replace("\n", "");

                        if (line.startsWith("id: "))
                            id = line.substring(4);
                        else if (line.startsWith("Location: "))
                            location = line.substring(10);
                        else if (line.startsWith("model: "))
                            model = line.substring(7);
                        else if (line.startsWith("support: "))
                            support = line.substring(9);
                    }
                    if (!id.equals("") && !devices.containsKey(id)) {
                        YeelightDevice device = new YeelightDevice(id, location, model, support);
                        devices.put(id, device);
                        logger.info("Found Yeelight device :\n" + device.toString());
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e.toString());
        }
    }

    private boolean isNotifyPacket(String sentence) {
        return sentence.startsWith("NOTIFY * HTTP/1.1");
    }

    private boolean isOKPacket(String sentence) {
        return sentence.startsWith("HTTP/1.1 200 OK");
    }

    private void discoverYeelightDevices() {
        String url = null;

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("M-SEARCH * HTTP/1.1\r\n");
            sb.append("MAN: \"ssdp:discover\"\r\n");
            sb.append("ST: wifi_bulb\r\n");

            byte[] sendData = sb.toString().getBytes("UTF-8");
            InetAddress addr = InetAddress.getByName(MCAST_ADDR);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, addr, MCAST_PORT);

            socket.send(sendPacket);
        } catch (MalformedURLException e) {
            logger.error("The URL '" + url + "' is malformed: " + e.toString());
        } catch (Exception e) {
            logger.error("Cannot get SomfyTahoma login cookie: " + e.toString());
        }
    }

    /**
     * Called by the SCR when the configuration of a binding has been changed through the ConfigAdmin service.
     *
     * @param configuration Updated configuration properties
     */
    public void modified(final Map<String, Object> configuration) {
        // update the internal configuration accordingly
    }

    /**
     * Called by the SCR to deactivate the component when either the configuration is removed or
     * mandatory references are no longer satisfied or the component has simply been stopped.
     *
     * @param reason Reason code for the deactivation:<br>
     *               <ul>
     *               <li> 0 – Unspecified
     *               <li> 1 – The component was disabled
     *               <li> 2 – A reference became unsatisfied
     *               <li> 3 – A configuration was changed
     *               <li> 4 – A configuration was deleted
     *               <li> 5 – The component was disposed
     *               <li> 6 – The bundle was stopped
     *               </ul>
     */
    public void deactivate(final int reason) {
        this.bundleContext = null;
        if (thread != null && thread.isAlive())
            thread.interrupt();
        if (this.socket != null)
            socket.close();
        devices.clear();
    }


    /**
     * @{inheritDoc}
     */
    @Override
    protected long getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected String getName() {
        return "Yeelight Refresh Service";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void execute() {
        // the frequently executed code (polling) goes here ...
        logger.debug("execute() method is called!");

        if (!bindingsExist()) {
            return;
        }

        Hashtable<String, String> propList = new Hashtable<>();

        //devices.clear();
        discoverYeelightDevices();

        for (final YeelightBindingProvider provider : providers) {
            for (String itemName : provider.getItemNames()) {

                YeelightBindingConfig config = (YeelightBindingConfig) provider.getItemConfig(itemName);
                if (config == null)
                    continue;

                String action = config.getAction();
                if (action.equals(TOGGLE))
                    continue;

                String location = config.getLocation();
                String result;

                if (!propList.containsKey(location)) {
                    result = sendYeelightGetPropCommand(location);
                    if (result == null)
                        continue;
                    propList.put(location, result);
                    logger.info("Result: " + result);
                } else {
                    result = propList.get(location);
                }

                processYeelightResult(result, action, itemName);
            }
        }
    }

    private void processYeelightResult(String result, String action, String itemName) {
        JsonObject jo = parser.parse(result).getAsJsonObject();
        State newState = null;
        State oldState = null;
        try {
            switch (action) {
                case SET_POWER:
                    String power = getJSONArrayResult(jo, 0).getAsString();
                    newState = power.equals("on") ? OnOffType.ON : OnOffType.OFF;
                    break;
                case SET_BRIGHT:
                    int bright = getJSONArrayResult(jo, 1).getAsInt();
                    newState = new PercentType(bright == 1 ? 0 : bright);
                    break;
                case SET_CT:
                    int ct = getJSONArrayResult(jo, 2).getAsInt();
                    newState = new PercentType((ct - 1700) / 48);
                    break;
                case SET_HSB:
                    int hue = getJSONArrayResult(jo, 3).getAsInt();
                    int sat = getJSONArrayResult(jo, 4).getAsInt();
                    int br = getJSONArrayResult(jo, 1).getAsInt();
                    newState = new HSBType(new DecimalType(hue), new PercentType(sat), new PercentType(br == 1 ? 0 : br));
                    break;
                case SET_RGB:
                    int rgb = getJSONArrayResult(jo, 5).getAsInt();
                    Color col = getRGBColor(rgb);
                    newState = new HSBType(col);
                    break;
                default:
                    logger.error("Unknown Yeelight action: " + action);

            }

            oldState = itemRegistry.getItem(itemName).getState();
        } catch (ItemNotFoundException e) {
            logger.error(e.toString());
        }
        if (!oldState.equals(newState)) {
            eventPublisher.postUpdate(itemName, newState);
        }
    }

    private JsonElement getJSONArrayResult(JsonObject jo, int pos) {
        return jo.get(RESULT).getAsJsonArray().get(pos);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void internalReceiveCommand(String itemName, Command command) {
        // the code being executed when a command was sent on the openHAB
        // event bus goes here. This method is only called if one of the
        // BindingProviders provide a binding for the given 'itemName'.
        logger.debug("internalReceiveCommand({},{}) is called!", itemName, command);

        YeelightBindingConfig config = getItemConfig(itemName);
        if (config == null)
            return;

        String location = config.getLocation();
        String action = config.getAction();

        switch (action) {
            case SET_POWER:
                if (command instanceof OnOffType) {
                    sendYeelightPowerCommand(location, command.toString().toLowerCase());
                }
                break;
            case TOGGLE:
                if (command instanceof OnOffType && command.equals(OnOffType.ON)) {
                    sendYeelightToggleCommand(location);
                }
                break;
            case SET_BRIGHT:
                sendYeelightBrightCommand(location, Integer.parseInt(command.toString()));
                break;
            case SET_CT:
                sendYeelightCTCommand(location, Integer.parseInt(command.toString()));
                break;
            case SET_HSB:
                if (command instanceof HSBType) {
                    HSBType hsb = (HSBType) command;
                    sendYeelightHSCommand(location, hsb.getHue().intValue(), hsb.getSaturation().intValue());
                    sendYeelightBrightCommand(location, hsb.getBrightness().intValue());
                } else if (command instanceof OnOffType) {
                    sendYeelightPowerCommand(location, command.toString().toLowerCase());
                }
                break;
            case SET_RGB:
                if (command instanceof HSBType) {
                    HSBType hsb = (HSBType) command;
                    sendYeelightRGBCommand(location, hsb.getRed().intValue(), hsb.getGreen().intValue(), hsb.getBlue().intValue());
                    sendYeelightBrightCommand(location, hsb.getBrightness().intValue());
                } else if (command instanceof OnOffType) {
                    sendYeelightPowerCommand(location, command.toString().toLowerCase());
                }
                break;
            default:
                logger.error("Unknown Yeelight command: " + action);
        }

    }

    private String sendYeelightGetPropCommand(String location) {
        return sendYeelightCommand(location, GET_PROP, new Object[]{"power", "bright", "ct", "hue", "sat", "rgb"});
    }


    private String sendYeelightToggleCommand(String location) {
        return sendYeelightCommand(location, TOGGLE, new Object[]{});
    }

    private String sendYeelightBrightCommand(String location, int param) {
        return sendYeelightCommand(location, SET_BRIGHT, new Object[]{param == 0 ? 1 : param, SMOOTH, 500});
    }


    private String sendYeelightRGBCommand(String location, int red, int green, int blue) {
        return sendYeelightCommand(location, SET_RGB, new Object[]{getRGBValue(red, green, blue), SMOOTH, 500});
    }

    private String sendYeelightHSCommand(String location, int hue, int saturation) {
        return sendYeelightCommand(location, "set_hsv", new Object[]{hue, saturation, SMOOTH, 500});
    }

    private String sendYeelightCTCommand(String location, int param) {
        return sendYeelightCommand(location, "set_ct_abx", new Object[]{1700 + 48 * param, SMOOTH, 500});
    }

    private String sendYeelightPowerCommand(String location, String param) {
        return sendYeelightCommand(location, SET_POWER, new Object[]{param, "", 0});
    }

    private String sendYeelightCommand(String location, String action, Object[] params) {
        int index = location.indexOf(":");
        Socket clientSocket = null;
        try {
            String ip = location.substring(0, index);
            int port = Integer.parseInt(location.substring(index + 1));
            clientSocket = new Socket(ip, port);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String sentence = "{\"id\":" + msgid++ + ",\"method\":\"" + action + "\",\"params\":[" + getProperties(params) + "]}\r\n";
            logger.debug("Sending sentence: " + sentence);
            outToServer.writeBytes(sentence);
            return inFromServer.readLine();
            //clientSocket.close();

        } catch (IOException e) {
            logger.error(e.toString());
        } finally {

            if (clientSocket != null)
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    //silence
                }
        }
        return null;
    }

    private int getRGBValue(int red, int green, int blue) {
        return red * 65536 + green * 256 + blue;
    }

    private Color getRGBColor(int rgb) {
        int red = rgb / 65536;
        int green = (rgb - red * 65536) / 256;
        int blue = rgb - red * 65536 - green * 256;
        return new Color(red, green, blue);

    }

    private YeelightBindingConfig getItemConfig(String itemName) {
        for (final YeelightBindingProvider provider : providers) {
            if (provider.getItemNames().contains(itemName))
                return (YeelightBindingConfig) provider.getItemConfig(itemName);
        }
        return null;
    }

    private String getProperties(Object[] properties) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Object o : properties) {
            if (!first)
                builder.append(",");
            else
                first = false;

            if (o instanceof String) {
                builder.append("\"");
                builder.append(o);
                builder.append("\"");
            } else
                builder.append(o);
        }
        return builder.toString();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void internalReceiveUpdate(String itemName, State newState) {
        // the code being executed when a state was sent on the openHAB
        // event bus goes here. This method is only called if one of the
        // BindingProviders provide a binding for the given 'itemName'.
        logger.debug("internalReceiveUpdate({},{}) is called!", itemName, newState);
    }

}
