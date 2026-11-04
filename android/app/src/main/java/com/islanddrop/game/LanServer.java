package com.islanddrop.game;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The tiny game server every copy of the app runs on port 8765.
 *
 * It serves the game page over HTTP (so friends can also join from a plain browser),
 * relays WebSocket messages between one host and its guests, and while a host is
 * connected it announces itself on the local network with a UDP broadcast on port 8766
 * so other phones can find it.
 *
 * Protocol (text frames):
 *   client -> server first frame:  "H<name>" (host, only from this device) or "G<name>" (guest)
 *   server -> client:              "I<id>" accepted, "E<reason>" refused, "N" no host yet
 *   host   -> server:              "<guestId>|<payload>" or "*|<payload>" (all guests)
 *   guest  -> server:              "<payload>"  (forwarded to host as "<guestId>|<payload>")
 *   server -> host:                "+<id>|<name>" guest joined, "-<id>" guest left
 *   server -> guests:              "X" when the host leaves
 */
public class LanServer {

    /** Opens a bundled game file by relative path, e.g. "index.html". */
    public interface Assets {
        InputStream open(String path) throws IOException;
    }

    public static final int PORT = 8765;
    public static final int BEACON_PORT = 8766;
    private static final String MAGIC = "ISLANDDROP";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Charset LATIN1 = Charset.forName("ISO-8859-1");

    private final Assets assets;
    private final String deviceName;
    private final Map<Integer, Client> guests = new ConcurrentHashMap<Integer, Client>();
    private ServerSocket serverSocket;
    private volatile boolean running;
    private volatile Client host;
    private int nextId = 1;
    private Thread beaconThread;

    public LanServer(Assets assets, String deviceName) {
        this.assets = assets;
        String n = deviceName == null || deviceName.trim().length() == 0 ? "Island Drop" : deviceName.trim();
        this.deviceName = n.replace('|', ' ');
    }

    public boolean isRunning() {
        return running;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(PORT));
        running = true;
        Thread t = new Thread(new Runnable() {
            public void run() {
                acceptLoop();
            }
        }, "lan-accept");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        Client h = host;
        if (h != null) h.close();
        for (Client c : guests.values()) c.close();
    }

    private void acceptLoop() {
        while (running) {
            try {
                final Socket s = serverSocket.accept();
                s.setTcpNoDelay(true);
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        handle(s);
                    }
                }, "lan-conn");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (!running) return;
            }
        }
    }

    private void handle(Socket s) {
        try {
            s.setSoTimeout(15000);
            InputStream in = new BufferedInputStream(s.getInputStream());
            OutputStream out = new BufferedOutputStream(s.getOutputStream());
            String request = readLine(in);
            if (request == null) {
                s.close();
                return;
            }
            Map<String, String> headers = new HashMap<String, String>();
            String line;
            while ((line = readLine(in)) != null && line.length() > 0) {
                int i = line.indexOf(':');
                if (i > 0) headers.put(line.substring(0, i).trim().toLowerCase(Locale.US), line.substring(i + 1).trim());
            }
            String[] parts = request.split(" ");
            String path = parts.length > 1 ? parts[1] : "/";
            String upgrade = headers.get("upgrade");
            if (upgrade != null && upgrade.equalsIgnoreCase("websocket")) {
                String key = headers.get("sec-websocket-key");
                if (key == null) {
                    s.close();
                    return;
                }
                String accept = base64(sha1((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(LATIN1)));
                out.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "
                        + accept + "\r\n\r\n").getBytes(LATIN1));
                out.flush();
                s.setSoTimeout(0);
                new Client(s, in, out).run();
                return;
            }
            serveFile(path, out);
            out.flush();
            s.close();
        } catch (Exception e) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void serveFile(String path, OutputStream out) throws IOException {
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        q = path.indexOf('#');
        if (q >= 0) path = path.substring(0, q);
        if (path.length() == 0 || path.equals("/")) path = "/index.html";
        String name = URLDecoder.decode(path.substring(1), "UTF-8");
        if (name.contains("..") || name.startsWith("/") || name.contains("\\")) {
            status(out, "403 Forbidden");
            return;
        }
        byte[] body;
        try {
            InputStream f = assets.open(name);
            body = readAll(f);
            f.close();
        } catch (IOException e) {
            status(out, "404 Not Found");
            return;
        }
        String type = name.endsWith(".html") ? "text/html; charset=utf-8"
                : name.endsWith(".js") ? "application/javascript"
                : name.endsWith(".css") ? "text/css"
                : name.endsWith(".png") ? "image/png"
                : name.endsWith(".json") ? "application/json"
                : "application/octet-stream";
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: " + type + "\r\nContent-Length: " + body.length
                + "\r\nCache-Control: no-cache\r\nConnection: close\r\n\r\n").getBytes(LATIN1));
        out.write(body);
    }

    private static void status(OutputStream out, String st) throws IOException {
        byte[] b = st.getBytes(LATIN1);
        out.write(("HTTP/1.1 " + st + "\r\nContent-Type: text/plain\r\nContent-Length: " + b.length
                + "\r\nConnection: close\r\n\r\n").getBytes(LATIN1));
        out.write(b);
    }

    /* ---------------- WebSocket client ---------------- */

    private final class Client implements Runnable {
        final Socket socket;
        final InputStream in;
        final OutputStream out;
        final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
        final AtomicBoolean open = new AtomicBoolean(true);
        boolean isHost;
        int id = -1;
        String name = "";

        Client(Socket socket, InputStream in, OutputStream out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            Thread w = new Thread(new Runnable() {
                public void run() {
                    writeLoop();
                }
            }, "lan-write");
            w.setDaemon(true);
            w.start();
        }

        void send(String msg) {
            if (!open.get()) return;
            if (queue.size() > 600) {
                close();
                return;
            }
            queue.offer(msg);
        }

        void writeLoop() {
            try {
                while (open.get()) {
                    String m = queue.poll(5, TimeUnit.SECONDS);
                    if (m == null) writeFrame(0x9, new byte[0]);
                    else writeFrame(0x1, m.getBytes(UTF8));
                }
            } catch (Exception e) {
                close();
            }
        }

        synchronized void writeFrame(int op, byte[] d) throws IOException {
            out.write(0x80 | op);
            int n = d.length;
            if (n < 126) {
                out.write(n);
            } else if (n < 65536) {
                out.write(126);
                out.write((n >>> 8) & 255);
                out.write(n & 255);
            } else {
                out.write(127);
                for (int i = 7; i >= 0; i--) out.write((int) ((((long) n) >>> (8 * i)) & 255));
            }
            out.write(d);
            out.flush();
        }

        public void run() {
            try {
                String first = readMessage();
                if (first == null) return;
                onFirst(first);
                String m;
                while (open.get() && (m = readMessage()) != null) onMessage(m);
            } catch (Exception ignored) {
            } finally {
                close();
            }
        }

        private int readByte() throws IOException {
            int b = in.read();
            if (b < 0) throw new IOException("closed");
            return b;
        }

        private void readFully(byte[] b) throws IOException {
            int off = 0;
            while (off < b.length) {
                int r = in.read(b, off, b.length - off);
                if (r < 0) throw new IOException("closed");
                off += r;
            }
        }

        String readMessage() throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            while (true) {
                int b0 = in.read();
                if (b0 < 0) return null;
                int b1 = readByte();
                boolean fin = (b0 & 0x80) != 0;
                int op = b0 & 0x0f;
                boolean masked = (b1 & 0x80) != 0;
                long len = b1 & 0x7f;
                if (len == 126) {
                    len = (readByte() << 8) | readByte();
                } else if (len == 127) {
                    len = 0;
                    for (int i = 0; i < 8; i++) len = (len << 8) | readByte();
                }
                if (len > (1 << 20)) throw new IOException("frame too big");
                byte[] mask = new byte[4];
                if (masked) readFully(mask);
                byte[] data = new byte[(int) len];
                readFully(data);
                if (masked) for (int i = 0; i < data.length; i++) data[i] ^= mask[i & 3];
                if (op == 0x8) return null;
                if (op == 0x9) {
                    writeFrame(0xA, data);
                    continue;
                }
                if (op == 0xA) continue;
                buf.write(data);
                if (buf.size() > (1 << 20)) throw new IOException("message too big");
                if (fin) return new String(buf.toByteArray(), UTF8);
            }
        }

        void onFirst(String m) {
            char c = m.length() > 0 ? m.charAt(0) : '?';
            name = clean(m.length() > 1 ? m.substring(1) : "");
            if (c == 'H') {
                if (!socket.getInetAddress().isLoopbackAddress()) {
                    send("EOnly the phone running the app can host its own game.");
                    closeSoon();
                    return;
                }
                synchronized (LanServer.this) {
                    Client h = host;
                    if (h != null && h.open.get()) {
                        send("EThis phone is already hosting a game in another window.");
                        closeSoon();
                        return;
                    }
                    isHost = true;
                    id = 0;
                    host = this;
                }
                send("I0");
                for (Client g : guests.values()) send("+" + g.id + "|" + g.name);
                startBeacon();
            } else if (c == 'G') {
                synchronized (LanServer.this) {
                    id = nextId++;
                    guests.put(id, this);
                }
                send("I" + id);
                Client h = host;
                if (h != null) h.send("+" + id + "|" + name);
                else send("N");
            } else {
                close();
            }
        }

        void onMessage(String m) {
            if (isHost) {
                int p = m.indexOf('|');
                if (p < 0) return;
                String to = m.substring(0, p);
                String payload = m.substring(p + 1);
                if (to.equals("*")) {
                    for (Client g : guests.values()) g.send(payload);
                } else {
                    try {
                        Client g = guests.get(Integer.parseInt(to));
                        if (g != null) g.send(payload);
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else {
                Client h = host;
                if (h != null) h.send(id + "|" + m);
            }
        }

        /** Lets a refusal message reach the client before the socket closes. */
        void closeSoon() {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException ignored) {
                    }
                    close();
                }
            }, "lan-close");
            t.setDaemon(true);
            t.start();
        }

        void close() {
            if (!open.compareAndSet(true, false)) return;
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            if (isHost) {
                synchronized (LanServer.this) {
                    if (host == this) host = null;
                }
                for (Client g : guests.values()) g.send("X");
            } else if (id > 0) {
                guests.remove(id);
                Client h = host;
                if (h != null) h.send("-" + id);
            }
        }
    }

    /* ---------------- LAN discovery ---------------- */

    private synchronized void startBeacon() {
        if (beaconThread != null && beaconThread.isAlive()) return;
        beaconThread = new Thread(new Runnable() {
            public void run() {
                beaconLoop();
            }
        }, "lan-beacon");
        beaconThread.setDaemon(true);
        beaconThread.start();
    }

    private void beaconLoop() {
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            ds.setBroadcast(true);
            byte[] msg = (MAGIC + "|" + PORT + "|" + deviceName).getBytes(UTF8);
            while (running && host != null) {
                for (InetAddress a : broadcastAddresses()) {
                    try {
                        ds.send(new DatagramPacket(msg, msg.length, a, BEACON_PORT));
                    } catch (IOException ignored) {
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (ds != null) ds.close();
        }
    }

    private static List<InetAddress> broadcastAddresses() {
        List<InetAddress> list = new ArrayList<InetAddress>();
        try {
            list.add(InetAddress.getByName("255.255.255.255"));
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e != null && e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress b = ia.getBroadcast();
                    if (b != null && !list.contains(b)) list.add(b);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /** Listens for host announcements for the given time and returns them as a JSON array of {ip,name}. */
    public static String scan(int millis) {
        Map<String, String> found = new LinkedHashMap<String, String>();
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket(null);
            ds.setReuseAddress(true);
            ds.setBroadcast(true);
            ds.bind(new InetSocketAddress(BEACON_PORT));
            ds.setSoTimeout(250);
            byte[] buf = new byte[512];
            long end = System.currentTimeMillis() + millis;
            while (System.currentTimeMillis() < end) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    ds.receive(p);
                    String s = new String(p.getData(), 0, p.getLength(), UTF8);
                    if (!s.startsWith(MAGIC + "|")) continue;
                    String[] f = s.split("\\|", 3);
                    found.put(p.getAddress().getHostAddress(), f.length > 2 ? clean(f[2]) : "Island Drop");
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (ds != null) ds.close();
        }
        StringBuilder sb = new StringBuilder("[");
        for (Map.Entry<String, String> en : found.entrySet()) {
            if (sb.length() > 1) sb.append(',');
            sb.append("{\"ip\":\"").append(json(en.getKey())).append("\",\"name\":\"").append(json(en.getValue())).append("\"}");
        }
        return sb.append(']').toString();
    }

    /** This phone's address on Wi-Fi or its own hotspot, or "" when it has none. */
    public static String localIp() {
        String best = "";
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e != null && e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                String n = ni.getName().toLowerCase(Locale.US);
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress a = ia.getAddress();
                    if (!(a instanceof Inet4Address) || !a.isSiteLocalAddress()) continue;
                    String ip = a.getHostAddress();
                    if (n.startsWith("wlan") || n.startsWith("ap") || n.startsWith("swlan") || n.startsWith("eth") || n.startsWith("en")) return ip;
                    if (best.length() == 0 && !n.startsWith("rmnet") && !n.startsWith("ccmni")) best = ip;
                }
            }
        } catch (Exception ignored) {
        }
        return best;
    }

    /* ---------------- helpers ---------------- */

    private static String clean(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length() && b.length() < 24; i++) {
            char c = s.charAt(i);
            if (c >= 32 && c != '|' && c != '<' && c != '>') b.append(c);
        }
        return b.toString().trim();
    }

    private static String json(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 32) b.append(' ');
            else b.append(c);
        }
        return b.toString();
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) >= 0) {
            if (c == '\n') break;
            if (c != '\r') b.write(c);
            if (b.size() > 8192) throw new IOException("header too long");
        }
        if (c < 0 && b.size() == 0) return null;
        return new String(b.toByteArray(), LATIN1);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int r;
        while ((r = in.read(buf)) > 0) b.write(buf, 0, r);
        return b.toByteArray();
    }

    private static byte[] sha1(byte[] d) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(d);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final char[] B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private static String base64(byte[] d) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length; i += 3) {
            int b = (d[i] & 255) << 16 | (i + 1 < d.length ? (d[i + 1] & 255) << 8 : 0) | (i + 2 < d.length ? d[i + 2] & 255 : 0);
            sb.append(B64[b >> 18 & 63]).append(B64[b >> 12 & 63]);
            sb.append(i + 1 < d.length ? B64[b >> 6 & 63] : '=');
            sb.append(i + 2 < d.length ? B64[b & 63] : '=');
        }
        return sb.toString();
    }
}
