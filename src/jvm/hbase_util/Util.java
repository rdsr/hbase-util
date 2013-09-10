package hbase_util;

public class Util {
    /**
     * Write a printable representation of a byte array.
     * 
     * @param b byte array
     * @return string
     * @see #toStringBinary(byte[], int, int)
     */
    public static String toStringBinary(final byte[] b) {
        if (b == null) {
            return "null";
        }
        return toStringBinary(b, 0, b.length);
    }

    /**
     * Write a printable representation of a byte array. Non-printable characters are hex escaped in
     * the format \\x%02X, eg: \x00 \x05 etc
     * 
     * @param b array to write out
     * @param off offset to start at
     * @param len length to write
     * @return string output
     */
    public static String toStringBinary(final byte[] b, int off, int len) {
        final StringBuilder result = new StringBuilder();
        // Just in case we are passed a 'len' that is > buffer length...
        if (off >= b.length) {
            return result.toString();
        }
        if (off + len > b.length) {
            len = b.length - off;
        }
        for (int i = off; i < off + len; ++i) {
            final int ch = b[i] & 0xFF;
            if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')
                    || " `~!@#$%^&*()-_=+[]{}|;:'\",.<>/?".indexOf(ch) >= 0) {
                result.append((char) ch);
            } else {
                result.append(String.format("\\x%02X", ch));
            }
        }
        return result.toString();
    }

    public static byte[] toBytesBinary(String in) {
        // this may be bigger than we need, but let's be safe.
        final byte[] b = new byte[in.length()];
        int size = 0;
        for (int i = 0; i < in.length(); ++i) {
            final char ch = in.charAt(i);
            if (ch == '\\' && in.length() > i + 1 && in.charAt(i + 1) == 'x') {
                // ok, take next 2 hex digits.
                final char hd1 = in.charAt(i + 2);
                final char hd2 = in.charAt(i + 3);

                // they need to be [A-F|a-f]0-9:
                if (!isHexDigit(hd1) || !isHexDigit(hd2)) {
                    // bogus escape code, ignore:
                    continue;
                }
                // turn hex ASCII digit -> number
                final byte d = (byte) ((toBinaryFromHex((byte) hd1) << 4) + toBinaryFromHex((byte) hd2));

                b[size++] = d;
                i += 3; // skip 3
            } else {
                b[size++] = (byte) ch;
            }
        }
        // resize:
        final byte[] b2 = new byte[size];
        System.arraycopy(b, 0, b2, 0, size);
        return b2;
    }

    /**
     * Takes a ASCII digit in the range A-F0-9 and returns the corresponding integer/ordinal value.
     * 
     * @param ch The hex digit.
     * @return The converted hex value as a byte.
     */
    public static byte toBinaryFromHex(byte ch) {
        if (ch >= 'A' && ch <= 'F') {
            return (byte) ((byte) 10 + (byte) (ch - 'A'));
        }
        if (ch >= 'a' && ch <= 'f') {
            return (byte) ((byte) 10 + (byte) (ch - 'a'));
        }

        return (byte) (ch - '0');
    }

    private static boolean isHexDigit(char c) {
        return (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f') || (c >= '0' && c <= '9');
    }
}
