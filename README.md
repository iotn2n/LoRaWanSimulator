# LoRaWanSimulator

How to use:
```bash
java -jar LoRaWanSimulator-0.1.jar --devAddr BA3C02E9 --nwkSKey 7077724ACAE9B1A72FBB0197E7892240 --appSKey 3A4AE8EDEEAEEF4815E200BAB5BDAC27 --plain Hello --fCnt 1
```

All options:
```
--appSKey VAL    : Sets the application session key
--devAddr VAL    : Sets the device address
--devEUI VAL     : Sets the device unique identifier (default: 0001020304050607)
--fCnt N         : Sets the first fCnt (default: 1)
--gatewayEUI VAL : Sets the gateway unique identifier (default: 0001020304050607)
--hex VAL        : Set the hex payload (default: empty)
--nwkSKey VAL    : Sets the network session key
--plain VAL      : Set the plain payload (default: empty)
--routerHost VAL : Sets the router host (default: router.eu.thethings.network)
--routerPort N   : Sets the router port (default: 1700)
-n N             : Number of messages to send (default: 1)

```
