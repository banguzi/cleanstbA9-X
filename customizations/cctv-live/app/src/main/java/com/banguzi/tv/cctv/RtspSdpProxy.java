package com.banguzi.tv.cctv;

import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Lightweight localhost-only RTSP-over-TCP relay that normalizes non-RFC camera SDP responses
 * (such as gvminirtsp/ireader cameras emitting bare LF instead of CRLF or omitting Content-Base)
 * while forwarding interleaved binary RTP/RTCP ('$') packets untouched.
 */
final class RtspSdpProxy {
    private final String remoteHost;
    private final int remotePort;
    private final String remoteHostHeader;
    private final ServerSocket serverSocket;
    private final String localRtspUrl;
    private final String localContentBase;

    private volatile boolean closed;
    private volatile Socket clientSocket;
    private volatile Socket upstreamSocket;

    private RtspSdpProxy(String originalUrl) throws IOException {
        Uri uri = Uri.parse(originalUrl);
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new IOException("Invalid RTSP host");
        }
        this.remoteHost = host;
        int parsedPort = uri.getPort();
        this.remotePort = parsedPort > 0 ? parsedPort : 554;
        this.remoteHostHeader = parsedPort > 0 ? (host + ":" + parsedPort) : host;

        this.serverSocket = new ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"));
        int localPort = serverSocket.getLocalPort();

        String userInfo = uri.getEncodedUserInfo();
        String authPrefix = (userInfo != null && !userInfo.isEmpty()) ? (userInfo + "@") : "";
        String path = uri.getEncodedPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        String query = uri.getEncodedQuery();
        String baseNoQuery = "rtsp://" + authPrefix + "127.0.0.1:" + localPort + path;
        this.localRtspUrl = (query != null && !query.isEmpty()) ? (baseNoQuery + "?" + query) : baseNoQuery;
        this.localContentBase = baseNoQuery.endsWith("/") ? baseNoQuery : (baseNoQuery + "/");

        Thread acceptThread = new Thread(this::acceptLoop, "RtspSdpProxy-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    static RtspSdpProxy start(String originalUrl) throws IOException {
        return new RtspSdpProxy(originalUrl);
    }

    String localUrl() {
        return localRtspUrl;
    }

    void close() {
        closed = true;
        closeQuietly(serverSocket);
        closeQuietly(clientSocket);
        closeQuietly(upstreamSocket);
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket client = serverSocket.accept();
                if (closed) {
                    closeQuietly(client);
                    return;
                }
                closeQuietly(clientSocket);
                closeQuietly(upstreamSocket);

                client.setTcpNoDelay(true);
                Socket upstream = new Socket();
                upstream.setTcpNoDelay(true);
                upstream.connect(new InetSocketAddress(remoteHost, remotePort), 6000);

                this.clientSocket = client;
                this.upstreamSocket = upstream;

                String localAuthority = "127.0.0.1:" + serverSocket.getLocalPort();

                Thread upThread = new Thread(
                        () -> pumpClientToCamera(client, upstream, localAuthority),
                        "RtspSdpProxy-Up");
                upThread.setDaemon(true);
                upThread.start();

                Thread downThread = new Thread(
                        () -> pumpCameraToClient(upstream, client),
                        "RtspSdpProxy-Down");
                downThread.setDaemon(true);
                downThread.start();
            } catch (IOException e) {
                if (!closed) {
                    Log.w(CctvConfig.TAG, "proxy connection error: " + e.getClass().getSimpleName());
                }
                return;
            }
        }
    }

    private void pumpClientToCamera(Socket client, Socket upstream, String localAuthority) {
        try {
            InputStream in = new BufferedInputStream(client.getInputStream(), 4096);
            OutputStream out = new BufferedOutputStream(upstream.getOutputStream(), 4096);
            byte[] rtpBuf = new byte[65536];

            while (!closed) {
                int first = in.read();
                if (first == -1) break;

                if (first == '$') {
                    int ch = readByte(in);
                    int hi = readByte(in);
                    int lo = readByte(in);
                    int len = (hi << 8) | lo;
                    readFully(in, rtpBuf, len);
                    out.write('$');
                    out.write(ch);
                    out.write(hi);
                    out.write(lo);
                    out.write(rtpBuf, 0, len);
                    out.flush();
                } else {
                    byte[] rawHeaders = readRtspHeaders(first, in);
                    String headerStr = new String(rawHeaders, StandardCharsets.UTF_8);
                    int bodyLen = parseContentLength(headerStr);
                    byte[] body = new byte[bodyLen];
                    if (bodyLen > 0) {
                        readFully(in, body, bodyLen);
                    }
                    String rewritten = headerStr.replace(localAuthority, remoteHostHeader);
                    out.write(rewritten.getBytes(StandardCharsets.UTF_8));
                    if (bodyLen > 0) {
                        out.write(body);
                    }
                    out.flush();
                }
            }
        } catch (IOException ignored) {
        } finally {
            closeQuietly(client);
            closeQuietly(upstream);
        }
    }

    private void pumpCameraToClient(Socket upstream, Socket client) {
        try {
            InputStream in = new BufferedInputStream(upstream.getInputStream(), 16384);
            OutputStream out = new BufferedOutputStream(client.getOutputStream(), 16384);
            byte[] rtpBuf = new byte[65536];

            while (!closed) {
                int first = in.read();
                if (first == -1) break;

                if (first == '$') {
                    int ch = readByte(in);
                    int hi = readByte(in);
                    int lo = readByte(in);
                    int len = (hi << 8) | lo;
                    readFully(in, rtpBuf, len);
                    out.write('$');
                    out.write(ch);
                    out.write(hi);
                    out.write(lo);
                    out.write(rtpBuf, 0, len);
                    if (in.available() == 0) {
                        out.flush();
                    }
                } else {
                    byte[] rawHeaders = readRtspHeaders(first, in);
                    String headerStr = new String(rawHeaders, StandardCharsets.UTF_8);
                    int bodyLen = parseContentLength(headerStr);
                    byte[] body = new byte[bodyLen];
                    if (bodyLen > 0) {
                        readFully(in, body, bodyLen);
                    }

                    String lowerHeaders = headerStr.toLowerCase(Locale.ROOT);
                    if (bodyLen > 0 && lowerHeaders.contains("application/sdp")) {
                        body = normalizeSdp(body);
                        headerStr = rewriteSdpHeaders(headerStr, body.length);
                    } else if (lowerHeaders.contains(remoteHostHeader.toLowerCase(Locale.ROOT))) {
                        String localAuth = "127.0.0.1:" + serverSocket.getLocalPort();
                        headerStr = headerStr.replace(remoteHostHeader, localAuth);
                    }

                    out.write(headerStr.getBytes(StandardCharsets.UTF_8));
                    if (body.length > 0) {
                        out.write(body);
                    }
                    out.flush();
                }
            }
        } catch (IOException ignored) {
        } finally {
            closeQuietly(upstream);
            closeQuietly(client);
        }
    }

    private String rewriteSdpHeaders(String rawHeaders, int newLength) {
        String[] lines = rawHeaders.split("\r?\n");
        StringBuilder sb = new StringBuilder(rawHeaders.length() + 96);
        boolean hasContentBase = false;
        boolean hasContentLength = false;

        for (String line : lines) {
            if (line.isEmpty()) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("content-length:")) {
                sb.append("Content-Length: ").append(newLength).append("\r\n");
                hasContentLength = true;
            } else {
                if (lower.startsWith("content-base:") || lower.startsWith("content-location:")) {
                    hasContentBase = true;
                }
                sb.append(line).append("\r\n");
            }
        }
        if (!hasContentLength) {
            sb.append("Content-Length: ").append(newLength).append("\r\n");
        }
        if (!hasContentBase) {
            sb.append("Content-Base: ").append(localContentBase).append("\r\n");
        }
        sb.append("\r\n");
        return sb.toString();
    }

    static byte[] normalizeSdp(byte[] rawBody) {
        String sdp = new String(rawBody, StandardCharsets.UTF_8);
        String[] rawLines = sdp.split("\r?\n");
        StringBuilder out = new StringBuilder(sdp.length() + 16);
        for (String line : rawLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                out.append(trimmed).append("\r\n");
            }
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] readRtspHeaders(int firstByte, InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(512);
        buf.write(firstByte);
        int b0 = -1, b1 = -1, b2 = -1, b3 = firstByte;
        while (true) {
            int next = in.read();
            if (next == -1) {
                throw new EOFException("EOF inside RTSP headers");
            }
            buf.write(next);
            b0 = b1;
            b1 = b2;
            b2 = b3;
            b3 = next;
            if (b0 == '\r' && b1 == '\n' && b2 == '\r' && b3 == '\n') {
                break;
            }
            if (b2 == '\n' && b3 == '\n') {
                break;
            }
            if (buf.size() > 16384) {
                throw new IOException("RTSP header too large");
            }
        }
        return buf.toByteArray();
    }

    private static int parseContentLength(String headers) {
        for (String line : headers.split("\r?\n")) {
            if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                try {
                    return Integer.parseInt(line.substring(15).trim());
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static int readByte(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) throw new EOFException();
        return b;
    }

    private static void readFully(InputStream in, byte[] dst, int len) throws IOException {
        int off = 0;
        while (off < len) {
            int n = in.read(dst, off, len - off);
            if (n < 0) throw new EOFException();
            off += n;
        }
    }

    private static void closeQuietly(ServerSocket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeQuietly(Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }
}
