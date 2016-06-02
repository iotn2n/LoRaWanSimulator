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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author cambierr
 */
public class Run {

    private static Random random = new Random();

    private static byte[] devAddr;
    private static byte[] appEUI;
    private static byte[] nwkSKey;
    private static byte[] appSKey;
    private static byte[] devEUI;
    private static byte[] gatewayEUI;

    private static String routerAddress;
    private static int routerPort;

    private static short fCnt = 0;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        devAddr = hexStringToByteArray("BA3C02E9");

        appEUI = hexStringToByteArray("70B3D57ED00002C0");

        nwkSKey = hexStringToByteArray("7077724ACAE9B1A72FBB0197E7892241");

        appSKey = hexStringToByteArray("3A4AE8EDEEAEEF4815E200BAB5BDAC28");

        devEUI = new byte[8];
        random.nextBytes(devEUI);

        gatewayEUI = new byte[8];
        random.nextBytes(gatewayEUI);

        routerAddress = "router.eu.thethings.network";

        routerPort = 1700;
        
        
        reverseArray(devAddr);
        for (int i = 0; i < 1; i++) {
            run();
        }

    }

    private static void send(byte[] _data) throws Exception {
        InetAddress address = InetAddress.getByName(routerAddress);

        DatagramPacket packet = new DatagramPacket(_data, _data.length, address, routerPort);

        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(packet);
        dsocket.close();
    }

    private static void reverseArray(byte[] _array) {
        byte tmp;
        for (int i = 0; i < _array.length / 2; i++) {
            tmp = _array[i];
            _array[i] = _array[_array.length - i - 1];
            _array[_array.length - i - 1] = tmp;
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static void run() throws Exception {


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
        p.setClearPayLoad("salut".getBytes());

        pp.setMic(p.computeMic());

        ByteBuffer bb = ByteBuffer.allocate(1024);
        pp.toRaw(bb);
        byte[] data = Arrays.copyOfRange(bb.array(), 0, bb.capacity() - bb.remaining());

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

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

        send(Arrays.copyOfRange(pckt.array(), 0, pckt.capacity() - pckt.remaining()));

        System.out.println("sent !");
    }

}
