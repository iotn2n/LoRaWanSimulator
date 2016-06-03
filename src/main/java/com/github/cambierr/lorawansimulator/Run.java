/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.cambierr.lorawansimulator;

import com.github.cambierr.lorawanpacket.DataPayload;
import com.github.cambierr.lorawanpacket.Direction;
import com.github.cambierr.lorawanpacket.FHDR;
import com.github.cambierr.lorawanpacket.MacPayload;
import com.github.cambierr.lorawanpacket.PhyPayload;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 *
 * @author cambierr
 */
public class Run {

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static String tz;

    @Option(name = "--devAddr", usage = "Sets the device address", required = true)
    private String hex_devAddr = "00010203";

    @Option(name = "--nwkSKey", usage = "Sets the network session key", required = true)
    private String hex_nwkSKey = "000102030405060708090A0B0C0D0E0F";

    @Option(name = "--appSKey", usage = "Sets the application session key", required = true)
    private String hex_appSKey = "000102030405060708090A0B0C0D0E0F";

    @Option(name = "--devEUI", usage = "Sets the device unique identifier", required = false)
    private String hex_devEUI = "0001020304050607";

    @Option(name = "--gatewayEUI", usage = "Sets the gateway unique identifier", required = false)
    private String hex_gatewayEUI = "0001020304050607";

    @Option(name = "--fCnt", usage = "Sets the first fCnt", required = false)
    private short fCnt = 0;

    @Option(name = "--routerHost", usage = "Sets the router host", required = false)
    private String routerAddress = "router.eu.thethings.network";

    @Option(name = "--routerPort", usage = "Sets the router port", required = false)
    private int routerPort = 1700;

    @Option(name = "-n", usage = "Number of messages to send", required = false)
    private int count = 1;

    @Option(name = "--plain", usage = "Set the plain payload", required = false, forbids = {"--hex"})
    private String txt = "";

    @Option(name = "--hex", usage = "Set the hex payload", required = false, forbids = {"--plain"})
    private String hex = "";

    private byte[] devAddr;
    private byte[] nwkSKey;
    private byte[] appSKey;
    private byte[] devEUI;
    private byte[] gatewayEUI;
    private byte[] payload;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        new Run().run(args);
    }

    public void run(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(100);

        try {
            parser.parseArgument(args);
            Pattern p = Pattern.compile("[0-9A-F]{8}");
            if (!p.matcher(hex_devAddr).matches()) {
                throw new CmdLineException("Invalid devAddr");
            }
            p = Pattern.compile("[0-9A-F]{32}");
            if (!p.matcher(hex_nwkSKey).matches()) {
                throw new CmdLineException("Invalid nwkSKey");
            }
            if (!p.matcher(hex_appSKey).matches()) {
                throw new CmdLineException("Invalid appSKey");
            }
            p = Pattern.compile("[0-9A-F]{16}");
            if (!p.matcher(hex_devEUI).matches()) {
                throw new CmdLineException("Invalid devEUI");
            }
            if (!p.matcher(hex_gatewayEUI).matches()) {
                throw new CmdLineException("Invalid gatewayEUI");
            }
            if (!hex.equals("")) {
                p = Pattern.compile("([0-9A-F][0-9A-F]){1,125}");
                if (!p.matcher(hex).matches()) {
                    throw new CmdLineException("Invalid hex payload");
                }
            }

        } catch (CmdLineException ex) {
            System.err.println(ex.getMessage());
            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        devAddr = hexStringToByteArray(hex_devAddr);
        reverseArray(devAddr);
        nwkSKey = hexStringToByteArray(hex_nwkSKey);
        appSKey = hexStringToByteArray(hex_appSKey);
        devEUI = hexStringToByteArray(hex_devEUI);
        gatewayEUI = hexStringToByteArray(hex_gatewayEUI);
        if (!hex.equals("")) {
            payload = hexStringToByteArray(hex);
        } else {
            payload = txt.getBytes();
        }

        for (int i = 0; i < count; i++) {
            doWork();
        }

    }

    private void send(byte[] _data) throws Exception {
        InetAddress address = InetAddress.getByName(routerAddress);

        DatagramPacket packet = new DatagramPacket(_data, _data.length, address, routerPort);

        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(packet);
        dsocket.close();
    }

    private void reverseArray(byte[] _array) {
        byte tmp;
        for (int i = 0; i < _array.length / 2; i++) {
            tmp = _array[i];
            _array[i] = _array[_array.length - i - 1];
            _array[_array.length - i - 1] = tmp;
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private void doWork() {

        PhyPayload pp = new PhyPayload(Direction.UP);
        pp.setMHDR((byte) (1 << 7));

        MacPayload mp = new MacPayload(pp);

        FHDR fhdr = new FHDR();

        fhdr.setDevAddr(devAddr);
        fhdr.setfCnt(fCnt++);
        fhdr.setfCtrl((byte) 0);
        fhdr.setfOpts(new byte[]{});

        mp.setFhdr(fhdr);
        mp.setfPort((byte) 1);

        DataPayload p = new DataPayload(mp);
        p.setAppSKey(appSKey);
        p.setNwkSKey(nwkSKey);

        try {
            p.setClearPayLoad(payload);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }

        pp.setMic(p.computeMic());

        ByteBuffer bb = ByteBuffer.allocate(384);
        pp.toRaw(bb);
        byte[] data = Arrays.copyOfRange(bb.array(), 0, bb.capacity() - bb.remaining());

        JSONArray rxpk = new JSONArray()
                .put(new JSONObject()
                        .put("time", df.format(new Date()) + "002Z")
                        .put("tmst", (int) (System.currentTimeMillis() / 1000))
                        .put("chan", 2)
                        .put("rfch", 0)
                        .put("freq", 866.349812)
                        .put("stat", 1)
                        .put("modu", "LORA")
                        .put("datr", "SF7BW125")
                        .put("codr", "4/6")
                        .put("rssi", -35)
                        .put("lsnr", 5.1)
                        .put("size", data.length)
                        .put("data", Base64.getEncoder().encodeToString(data))
                );

        String packet = new JSONObject().put("rxpk", rxpk).toString();

        ByteBuffer pckt = ByteBuffer.allocate(2048);
        pckt.order(ByteOrder.LITTLE_ENDIAN);
        pckt.put((byte) 1);
        pckt.putShort((short) 42);
        pckt.put((byte) 0);
        pckt.put(gatewayEUI);
        pckt.put(packet.getBytes());

        try {
            send(Arrays.copyOfRange(pckt.array(), 0, pckt.capacity() - pckt.remaining()));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        System.out.println("Packet: " + packet);
    }

}
