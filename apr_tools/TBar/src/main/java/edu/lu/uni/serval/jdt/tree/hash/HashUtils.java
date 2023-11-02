package edu.lu.uni.serval.jdt.tree.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import edu.lu.uni.serval.jdt.tree.ITree;

public class HashUtils {

    private HashUtils() {}

    public static final int BASE = 33;

    public static final HashGenerator DEFAULT_HASH_GENERATOR = new RollingHashGenerator.Md5RollingHashGenerator();

    public static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
    }

    public static int standardHash(ITree t) {
        return Integer.valueOf(t.getType()).hashCode() + HashUtils.BASE * t.getLabel().hashCode();
    }

    public static String inSeed(ITree t) {
        return ITree.OPEN_SYMBOL + t.getLabel() + ITree.SEPARATE_SYMBOL + t.getType();
    }

    public static String outSeed(ITree t) {
        return  t.getType() + ITree.SEPARATE_SYMBOL + t.getLabel() + ITree.CLOSE_SYMBOL;
    }

    public static int md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes());
            return byteArrayToInt(digest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return ITree.NO_VALUE;
    }

    public static int fpow(int a, int b) {
        if (b == 1)
            return a;
        int result = 1;
        while (b > 0) {
            if ((b & 1) != 0)
                result *= a;
            b >>= 1;
            a *= a;
        }
        return result;
    }

}
