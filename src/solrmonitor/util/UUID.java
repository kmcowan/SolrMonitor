package  solrmonitor.util;

import java.rmi.dgc.VMID;
import java.security.MessageDigest;


public class UUID {

    private static String VMID = new VMID().toString();
    private static volatile long counter = 0;

    /** Creates new UUID */
    private UUID() {
    }

    public static String genUUID() {
        return genUUID160();
    }

    public static String genUUID128() {
        return toHexString(doUUID("MD5"));
    }

    public static String genUUID128DCEStyle() {
        String preString = genUUID128();
        return preString.substring(0,8) + "-" + preString.substring(8,12) + "-" + preString.substring(12,16) + "-" + preString.substring(16,20) + "-" + preString.substring(20,32);
    }

    public static String genUUID160() {
        return toHexString(doUUID("SHA-1"));
    }

    public static String genUUIDOfLength(int length){
        String result = UUID.genUUID();
         if(length > result.length()){
             length = result.length();
         }
        return result.substring(0, length);
    }

    private static String genUUIDasLongs() {
        String hexUUID = genUUID128();
        long longMost = Long.parseLong(hexUUID.substring(0,15),16);
        long longLeast = Long.parseLong(hexUUID.substring(16,31),16);
        return "mostSig=" + longMost + " leastSig=" + longLeast;
    }
    private static byte[] doUUID(String digestType) {
        StringBuffer constituents = new StringBuffer(UUID.VMID);
        constituents.append(UUID.counter);
        constituents.append(Math.random());
        constituents.append(System.currentTimeMillis());


        MessageDigest digest = null;

       try {
            digest = MessageDigest.getInstance(digestType);
            return digest.digest(constituents.toString().getBytes("UTF-8"));
       } catch(Exception ex) {
           throw new IllegalArgumentException();
       }

    }

    public static void main(String[] args) {
        System.out.println(genUUID128DCEStyle());
    }
    public static String toHexString(byte[] b) {

        StringBuffer buf = new StringBuffer(b.length * 2);
        buf.setLength(b.length * 2);

        for (int ix=0; ix<b.length; ix++) {
            int lo = b[ix] & 0x0F;
            int hi = (b[ix] & 0xF0) >> 4;
            buf.setCharAt(ix*2,     Character.forDigit(hi, 16));
            buf.setCharAt(ix*2 + 1, Character.forDigit(lo, 16));
        }

        return buf.toString();
    }

}
