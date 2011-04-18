package com.hz.yk.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.Cookie;


public class RequestUtil {

	/**
	 * Append request parameters from the specified String to the specified Map.
	 * It is presumed that the specified Map is not accessed from any other
	 * thread, so no synchronization is performed.
	 * <p>
	 * <strong>IMPLEMENTATION NOTE</strong>: URL decoding is performed
	 * individually on the parsed name and value elements, rather than on the
	 * entire query string ahead of time, to properly deal with the case where
	 * the name or value includes an encoded "=" or "&" character that would
	 * otherwise be interpreted as a delimiter.
	 * 
	 * @param map
	 *            Map that accumulates the resulting parameters
	 * @param data
	 *            Input string containing request parameters
	 * 
	 * @exception IllegalArgumentException
	 *                if the data is malformed
	 */
	public static void parseParameters(Map map, String data, String encoding)
			throws UnsupportedEncodingException {

		if ((data != null) && (data.length() > 0)) {

			// use the specified encoding to extract bytes out of the
			// given string so that the encoding is not lost. If an
			// encoding is not specified, let it use platform default
			byte[] bytes = null;
			try {
				if (encoding == null) {
					bytes = data.getBytes();
				} else {
					bytes = data.getBytes(encoding);
				}
			} catch (UnsupportedEncodingException uee) {
			}

			parseParameters(map, bytes, encoding);
		}

	}

	/**
	 * Parse a cookie header into an array of cookies according to RFC 2109.
	 * 
	 * @param header
	 *            Value of an HTTP "Cookie" header
	 */
	public static Cookie[] parseCookieHeader(String header) {

		if ((header == null) || (header.length() < 1))
			return (new Cookie[0]);

		ArrayList cookies = new ArrayList();
		while (header.length() > 0) {
			int semicolon = header.indexOf(';');
			if (semicolon < 0)
				semicolon = header.length();
			if (semicolon == 0)
				break;
			String token = header.substring(0, semicolon);
			if (semicolon < header.length())
				header = header.substring(semicolon + 1);
			else
				header = "";
			try {
				int equals = token.indexOf('=');
				if (equals > 0) {
					String name = token.substring(0, equals).trim();
					String value = token.substring(equals + 1).trim();
					cookies.add(new Cookie(name, value));
				}
			} catch (Throwable e) {
				;
			}
		}

		return ((Cookie[]) cookies.toArray(new Cookie[cookies.size()]));

	}

	/**
	 * Append request parameters from the specified String to the specified Map.
	 * It is presumed that the specified Map is not accessed from any other
	 * thread, so no synchronization is performed.
	 * <p>
	 * <strong>IMPLEMENTATION NOTE</strong>: URL decoding is performed
	 * individually on the parsed name and value elements, rather than on the
	 * entire query string ahead of time, to properly deal with the case where
	 * the name or value includes an encoded "=" or "&" character that would
	 * otherwise be interpreted as a delimiter.
	 * 
	 * NOTE: byte array data is modified by this method. Caller beware.
	 * 
	 * @param map
	 *            Map that accumulates the resulting parameters
	 * @param data
	 *            Input string containing request parameters
	 * @param encoding
	 *            Encoding to use for converting hex
	 * 
	 * @exception UnsupportedEncodingException
	 *                if the data is malformed
	 */
	public static void parseParameters(Map map, byte[] data, String encoding)
			throws UnsupportedEncodingException {

		if (data != null && data.length > 0) {
			int ix = 0;
			int ox = 0;
			String key = null;
			String value = null;
			while (ix < data.length) {
				byte c = data[ix++];
				switch ((char) c) {
				case '&':
					value = new String(data, 0, ox, encoding);
					if (key != null) {
						putMapEntry(map, key, value);
						key = null;
					}
					ox = 0;
					break;
				case '=':
					if (key == null) {
						key = new String(data, 0, ox, encoding);
						ox = 0;
					} else {
						data[ox++] = c;
					}
					break;
				case '+':
					data[ox++] = (byte) ' ';
					break;
				case '%':
					data[ox++] = (byte) ((convertHexDigit(data[ix++]) << 4) + convertHexDigit(data[ix++]));
					break;
				default:
					data[ox++] = c;
				}
			}
			// The last value does not end in '&'. So save it now.
			if (key != null) {
				value = new String(data, 0, ox, encoding);
				putMapEntry(map, key, value);
			}
		}

	}

	/**
	 * Put name and value pair in map. When name already exist, add value to
	 * array of values.
	 * 
	 * @param map
	 *            The map to populate
	 * @param name
	 *            The parameter name
	 * @param value
	 *            The parameter value
	 */
	private static void putMapEntry(Map map, String name, String value) {
		String[] newValues = null;
		String[] oldValues = (String[]) map.get(name);
		if (oldValues == null) {
			newValues = new String[1];
			newValues[0] = value;
		} else {
			newValues = new String[oldValues.length + 1];
			System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
			newValues[oldValues.length] = value;
		}
		map.put(name, newValues);
	}

	/**
	 * 转换成16进制
	 * Convert a byte character value to hexidecimal digit value.
	 * 
	 * @param b
	 *            the character value byte
	 */
	private static byte convertHexDigit(byte b) {
		if ((b >= '0') && (b <= '9'))
			return (byte) (b - '0');
		if ((b >= 'a') && (b <= 'f'))
			return (byte) (b - 'a' + 10);
		if ((b >= 'A') && (b <= 'F'))
			return (byte) (b - 'A' + 10);
		return 0;
	}
}
