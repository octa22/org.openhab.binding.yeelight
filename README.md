# org.openhab.binding.yeelight
Tested Yeelight devices: LED strip, ceiling light

# binding configuration
No configuration needed

# item file example
```
Switch YeelightStrip "Yeelight LED power" { yeelight="192.168.2.43:55443" }
Switch YeelightStripToggle "Toggle Yeelight LED strip" { yeelight="192.168.2.43:55443#toggle" }
Dimmer YeelightStripBright "LED brightness [%.1f]" { yeelight="192.168.2.43:55443#set_bright" }
Dimmer YeelightStripCT "LED color temp [%.1f]" { yeelight="192.168.2.43:55443#set_ct" }
Color YeelightRGB "RGB" { yeelight="192.168.2.43:55443#set_hsb" } 
Switch YeelightCeilingMoon "Yeelight ceiling moon" { yeelight="192.168.2.117:55443#nightlight" }
```
