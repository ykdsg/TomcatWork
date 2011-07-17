package com.hz.yk.ex03.connector.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hz.yk.naming.StringManager;

public class SocketInputStream extends InputStream {
	Log log = LogFactory.getLog(this.getClass());
	// -------------------------------------------------------------- Constants

	/**
	 * CR.
	 */
	private static final byte CR = (byte) '\r';

	/**
	 * LF.
	 */
	private static final byte LF = (byte) '\n';

	/**
	 * SP.
	 */
	private static final byte SP = (byte) ' ';

	/**
	 * HT. �������� (Ctrl-I)
	 */
	private static final byte HT = (byte) '\t';

	/**
	 * COLON.
	 */
	private static final byte COLON = (byte) ':';

	/**
	 * Lower case offset.
	 */
	private static final int LC_OFFSET = 'A' - 'a';

	/**
	 * Internal buffer.
	 */
	protected byte buf[];

	/**
	 * request ������Ϣ������ Last valid byte.
	 */
	protected int count;

	/**
	 * �������е�λ�� Position in the buffer.
	 */
	protected int pos;

	/**
	 * Underlying input stream.
	 */
	protected InputStream is;

	protected int readCount = 0;

	protected boolean eol = false;

	// ----------------------------------------------------------- Constructors

	/**
	 * Construct a servlet input stream associated with the specified socket
	 * input.
	 * 
	 * @param is
	 *            socket input stream
	 * @param bufferSize
	 *            size of the internal buffer
	 */
	public SocketInputStream(InputStream is, int bufferSize) {

		this.is = is;
		buf = new byte[bufferSize];

	}

	// -------------------------------------------------------------- Variables

	/**
	 * The string manager for this package.
	 */
	protected static StringManager sm = StringManager
			.getManager(Constants.Package);

	/**
	 * Read the request line, and copies it to the given buffer. This function
	 * is meant to be used during the HTTP request header parsing. Do NOT
	 * attempt to read the request body using it.
	 * 
	 * @param requestLine
	 *            Request line object
	 * @throws IOException
	 *             If an exception occurs during the underlying socket read
	 *             operations, or if the given buffer is not big enough to
	 *             accomodate the whole line.
	 */
	public void readRequestLine(HttpRequestLine requestLine) throws IOException {
		// Recycling check
		if (requestLine.methodEnd != 0) {
			requestLine.recycle();
		}
		// read request info to buf
		readRequestInfo();

		// Reading the method name
		readMethodName(requestLine);

		// Reading URI
		readURI(requestLine);

		// Reading protocol
		readProtocol(requestLine);

	}

	/**
	 * Read a header, and copies it to the given buffer. This function is meant
	 * to be used during the HTTP request header parsing. Do NOT attempt to read
	 * the request body using it.
	 * 
	 * @param requestLine
	 *            Request line object
	 * @throws IOException
	 *             If an exception occurs during the underlying socket read
	 *             operations, or if the given buffer is not big enough to
	 *             accomodate the whole line.
	 */
	public void readHeader(HttpHeader header) throws IOException {

		// Recycling check
		if (header.nameEnd != 0)
			header.recycle();

		// Checking for a blank line
		int chr = read();
		if ((chr == CR) || (chr == LF)) { // Skipping CR
			if (chr == CR)
				read(); // Skipping LF
			header.nameEnd = 0;
			header.valueEnd = 0;
			return;
		} else {
			pos--;
		}

		// Reading the header name

		int maxRead = header.name.length;
		int readStart = pos;
		int readCount = 0;

		boolean colon = false;

		while (!colon) {
			// if the buffer is full, extend it
			if (readCount >= maxRead) {
				if ((2 * maxRead) <= HttpHeader.MAX_NAME_SIZE) {
					char[] newBuffer = new char[2 * maxRead];
					System.arraycopy(header.name, 0, newBuffer, 0, maxRead);
					header.name = newBuffer;
					maxRead = header.name.length;
				} else {
					throw new IOException(
							sm.getString("requestStream.readline.toolong"));
				}
			}
			// We're at the end of the internal buffer
			if (pos >= count) {
				int val = read();
				if (val == -1) {
					throw new IOException(
							sm.getString("requestStream.readline.error"));
				}
				pos = 0;
				readStart = 0;
			}
			if (buf[pos] == COLON) {
				colon = true;
			}
			char val = (char) buf[pos];
			// ת��Сд
			if ((val >= 'A') && (val <= 'Z')) {
				val = (char) (val - LC_OFFSET);
			}
			header.name[readCount] = val;
			readCount++;
			pos++;
		}

		header.nameEnd = readCount - 1;

		// Reading the header value (which can be spanned over multiple lines)

		maxRead = header.value.length;
		readStart = pos;
		readCount = 0;

		int crPos = -2;

		boolean eol = false;
		boolean validLine = true;

		while (validLine) {

			boolean space = true;

			// Skipping spaces
			// Note : Only leading white spaces are removed. Trailing white
			// spaces are not.
			while (space) {
				// We're at the end of the internal buffer
				if (pos >= count) {
					// Copying part (or all) of the internal buffer to the line
					// buffer
					int val = read();
					if (val == -1)
						throw new IOException(
								sm.getString("requestStream.readline.error"));
					pos = 0;
					readStart = 0;
				}
				if ((buf[pos] == SP) || (buf[pos] == HT)) {
					pos++;
				} else {
					space = false;
				}
			}

			while (!eol) {
				// if the buffer is full, extend it
				if (readCount >= maxRead) {
					if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
						char[] newBuffer = new char[2 * maxRead];
						System.arraycopy(header.value, 0, newBuffer, 0, maxRead);
						header.value = newBuffer;
						maxRead = header.value.length;
					} else {
						throw new IOException(
								sm.getString("requestStream.readline.toolong"));
					}
				}
				// We're at the end of the internal buffer
				if (pos >= count) {
					// Copying part (or all) of the internal buffer to the line
					// buffer
					int val = read();
					if (val == -1)
						throw new IOException(
								sm.getString("requestStream.readline.error"));
					pos = 0;
					readStart = 0;
				}
				if (buf[pos] == CR) {
				} else if (buf[pos] == LF) {
					eol = true;
				} else {
					// FIXME : Check if binary conversion is working fine
					int ch = buf[pos] & 0xff;
					header.value[readCount] = (char) ch;
					readCount++;
				}
				pos++;
			}

			int nextChr = read();

			if ((nextChr != SP) && (nextChr != HT)) {
				pos--;
				validLine = false;
			} else {
				eol = false;
				// if the buffer is full, extend it
				if (readCount >= maxRead) {
					if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
						char[] newBuffer = new char[2 * maxRead];
						System.arraycopy(header.value, 0, newBuffer, 0, maxRead);
						header.value = newBuffer;
						maxRead = header.value.length;
					} else {
						throw new IOException(
								sm.getString("requestStream.readline.toolong"));
					}
				}
				header.value[readCount] = ' ';
				readCount++;
			}

		}

		header.valueEnd = readCount;

	}

	/**
	 * ����Ϣ��ȡ��buf,������pos����intֵ�� Read byte.
	 */
	@Override
	public int read() throws IOException {
		if (pos >= count) {
			fill();
			if (pos >= count)
				return -1;
		}
		return buf[pos++] & 0xff;
	}

	/**
     *
     */
	/*
	 * public int read(byte b[], int off, int len) throws IOException {
	 * 
	 * }
	 */

	/**
     *
     */
	/*
	 * public long skip(long n) throws IOException {
	 * 
	 * }
	 */

	/**
	 * Returns the number of bytes that can be read from this input stream
	 * without blocking.
	 */
	public int available() throws IOException {
		return (count - pos) + is.available();
	}

	/**
	 * Close the input stream.
	 */
	public void close() throws IOException {
		if (is == null)
			return;
		is.close();
		is = null;
		buf = null;
	}

	// ------------------------------------------------------ Protected Methods

	/**
	 * ��inputstream�е���Ϣ�����ֽ�����buf Fill the internal buffer using data from the
	 * undelying input stream.
	 */
	protected void fill() throws IOException {
		pos = 0;
		count = 0;
		int nRead = is.read(buf, 0, buf.length);
		log.info("the request : \n" + new String(buf));
		if (nRead > 0) {
			count = nRead;
		}
	}

	/**
	 * ������ͷ����Ϣ����buf, pos=����ͷ��ʼ��λ��,count=ʵ�ʶ�ȡ�ֽڵĳ���
	 * 
	 * @author "yangk"
	 * @date 2011-4-19 ����05:13:40
	 * @throws EOFException
	 */
	private void readRequestInfo() throws EOFException {
		// Checking for a blank line
		int chr = 0;
		do { // �������� Skipping CR or LF
			try {
				chr = read();
			} catch (IOException e) {
				chr = -1;
			}
		} while ((chr == CR) || (chr == LF));
		// ��������������⵽���ļ�������ĩβʱ���׳����쳣��
		if (chr == -1)
			throw new EOFException(sm.getString("requestStream.readline.error"));
		pos--;
	}

	/**
	 * ���ݵ�һ������ͷȡ��method����
	 * 
	 * @author "yangk"
	 * @date 2011-4-19 ����08:53:05
	 * @param requestLine
	 * @throws IOException
	 */
	private void readMethodName(HttpRequestLine requestLine) throws IOException {
		readCount = 0;
		boolean space = false;

		while (!space) {
			// if the buffer is full, extend it
			if (readCount >= requestLine.method.length) {
				requestLine.reSizeMethod(readCount);
			}
			// We're at the end of the internal buffer
			if (pos >= count) {
				int val = read();
				if (val == -1) {
					throw new IOException(
							sm.getString("requestStream.readline.error"));
				}
				pos = 0;
			}
			if (buf[pos] == SP) {
				space = true;
			}
			requestLine.method[readCount] = (char) buf[pos];
			readCount++;
			pos++;
		}
		requestLine.methodEnd = readCount - 1;
	}

	/**
	 * ���ݵ�һ������ͷȡ��uri
	 * 
	 * @author "yangk"
	 * @date 2011-4-19 ����08:51:47
	 * @param requestLine
	 * @throws IOException
	 */
	private void readURI(HttpRequestLine requestLine) throws IOException {
		int readCount = 0;
		boolean space = false;

		while (!space) {
			// if the buffer is full, extend it
			if (readCount >= requestLine.uri.length) {
				requestLine.reSizeUri(readCount);
			}
			// We're at the end of the internal buffer
			if (pos >= count) {
				int val = read();
				if (val == -1)
					throw new IOException(
							sm.getString("requestStream.readline.error"));
				pos = 0;
			}
			if (buf[pos] == SP) {
				space = true;
			} else if ((buf[pos] == CR) || (buf[pos] == LF)) {
				// HTTP/0.9 style request
				eol = true;
				space = true;
			}
			requestLine.uri[readCount] = (char) buf[pos];
			readCount++;
			pos++;
		}
		requestLine.uriEnd = readCount - 1;
	}

	/**
	 * ���ݵ�һ������ͷȡ��Protocol
	 * 
	 * @author "yangk"
	 * @date 2011-4-19 ����08:54:32
	 * @param requestLine
	 * @throws IOException
	 */
	private void readProtocol(HttpRequestLine requestLine) throws IOException {
		readCount = 0;

		while (!eol) {
			// if the buffer is full, extend it
			if (readCount >= requestLine.protocol.length) {
				requestLine.reSizeProtocol(readCount);
			}
			// We're at the end of the internal buffer
			if (pos >= count) {
				// Copying part (or all) of the internal buffer to the line
				// buffer
				int val = read();
				if (val == -1)
					throw new IOException(
							sm.getString("requestStream.readline.error"));
				pos = 0;
			}
			if (buf[pos] == CR) {
				// Skip CR.
			} else if (buf[pos] == LF) {
				eol = true;
			} else {
				requestLine.protocol[readCount] = (char) buf[pos];
				readCount++;
			}
			pos++;
		}

		requestLine.protocolEnd = readCount;
	}
}
