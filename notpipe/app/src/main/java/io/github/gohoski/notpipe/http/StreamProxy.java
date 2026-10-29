package io.github.gohoski.notpipe.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import io.github.gohoski.notpipe.NotPipe;

/**
 * Created by Claude on 29.10.2026.
 * A tiny local HTTP server between the player and a video file on a yt2009 instance.
 * HD comes from those instances as one static mp4 whose single connection is often slower than the video
 * plays (yet several connections together are much faster), so this downloads the part ahead of the
 * playhead in parallel chunks into a temporary file and feeds the player from it.
 */

public class StreamProxy {
    private static final int CHUNK_SIZE = 512 * 1024;
    private static final int WORKERS = 4;
    /** How far ahead of the playhead to download, in chunks (about 24 MB) */
    private static final int WINDOW = 48;

    private static StreamProxy instance;

    private ServerSocket server;
    private Download current;

    private StreamProxy() {}

    public static synchronized StreamProxy getInstance() {
        if (instance == null) instance = new StreamProxy();
        return instance;
    }

    /**
     * @return a local URL the player can open instead of the given one, or the original URL if the proxy can't start
     */
    public synchronized String proxy(String url) {
        try {
            ensureServer();
        } catch (IOException e) {
            e.printStackTrace();
            return url;
        }
        if (current == null || !current.url.equals(url)) {
            if (current != null) current.close();
            current = new Download(url);
        }
        return "http://127.0.0.1:" + server.getLocalPort() + "/video.mp4?" + current.token;
    }

    /** Stops downloading the given URL (if it's still the current one) and deletes the temporary file */
    public synchronized void release(String url) {
        if (current != null && current.url.equals(url)) {
            current.close();
            current = null;
        }
    }

    private synchronized Download find(String token) {
        return current != null && current.token.equals(token) ? current : null;
    }

    private void ensureServer() throws IOException {
        if (server != null && !server.isClosed()) return;
        // Leftovers from a previous run that ended without releasing
        File[] old = NotPipe.getAppContext().getCacheDir().listFiles();
        for (int i = 0; old != null && i < old.length; i++) {
            if (old[i].getName().startsWith("stream_")) old[i].delete();
        }
        server = new ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"));
        Thread accept = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    final Socket socket;
                    try {
                        socket = server.accept();
                    } catch (IOException e) {
                        return;
                    }
                    new Thread(new Runnable() {
                        public void run() {
                            serve(socket);
                        }
                    }).start();
                }
            }
        });
        accept.setDaemon(true);
        accept.start();
    }

    private void serve(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            String requestLine = readLine(in);
            long start = 0;
            boolean ranged = false;
            String line;
            while ((line = readLine(in)) != null && line.length() > 0) {
                String lower = line.toLowerCase();
                if (lower.startsWith("range:") && lower.indexOf("bytes=") >= 0) {
                    String spec = lower.substring(lower.indexOf("bytes=") + 6).trim();
                    int dash = spec.indexOf('-');
                    if (dash > 0) {
                        start = Long.parseLong(spec.substring(0, dash).trim());
                        ranged = true;
                    }
                }
            }
            String token = requestLine == null || requestLine.indexOf('?') < 0 ? ""
                    : requestLine.substring(requestLine.indexOf('?') + 1, requestLine.lastIndexOf(' '));
            Download download = find(token);
            OutputStream out = socket.getOutputStream();
            if (download == null || !download.prepare()) {
                out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes());
                out.flush();
                return;
            }
            long length = download.length;
            if (start >= length) {
                out.write(("HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */" + length
                        + "\r\nConnection: close\r\n\r\n").getBytes());
                out.flush();
                return;
            }
            StringBuilder head = new StringBuilder();
            head.append(ranged ? "HTTP/1.1 206 Partial Content\r\n" : "HTTP/1.1 200 OK\r\n");
            head.append("Content-Type: video/mp4\r\nAccept-Ranges: bytes\r\nConnection: close\r\n");
            head.append("Content-Length: ").append(length - start).append("\r\n");
            if (ranged) head.append("Content-Range: bytes ").append(start).append('-').append(length - 1)
                    .append('/').append(length).append("\r\n");
            head.append("\r\n");
            out.write(head.toString().getBytes());
            download.streamTo(out, start);
        } catch (Exception ignored) {
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') sb.append((char) c);
        }
        return c == -1 && sb.length() == 0 ? null : sb.toString();
    }

    /** One remote file being fetched in chunks into a temporary file */
    private static class Download {
        final String url;
        final String token = Long.toString(System.nanoTime(), 36);
        long length = -1;
        private int chunks;
        /** 0 = not started, 1 = downloading, 2 = done */
        private byte[] state;
        private RandomAccessFile file;
        private File path;
        private int readHead = 0;
        private volatile boolean closed = false;
        private boolean prepared = false, failed = false;

        Download(String url) {
            this.url = url;
        }

        /** Finds the file size and starts the workers, once */
        synchronized boolean prepare() {
            if (prepared) return !failed;
            prepared = true;
            try {
                HttpURLConnection c = open(0, 0);
                String range = c.getHeaderField("Content-Range");
                c.disconnect();
                if (range == null || range.indexOf('/') < 0) throw new IOException("No range support");
                length = Long.parseLong(range.substring(range.indexOf('/') + 1).trim());
                chunks = (int) ((length + CHUNK_SIZE - 1) / CHUNK_SIZE);
                state = new byte[chunks];
                path = new File(NotPipe.getAppContext().getCacheDir(), "stream_" + token + ".mp4");
                file = new RandomAccessFile(path, "rw");
                file.setLength(length);
            } catch (Exception e) {
                e.printStackTrace();
                failed = true;
                return false;
            }
            for (int i = 0; i < WORKERS; i++) {
                Thread worker = new Thread(new Runnable() {
                    public void run() {
                        work();
                    }
                });
                worker.setDaemon(true);
                worker.start();
            }
            return true;
        }

        private HttpURLConnection open(long from, long to) throws IOException {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(HttpClient.CONNECT_TIMEOUT);
            c.setReadTimeout(20000);
            c.setRequestProperty("User-Agent", HttpClient.USER_AGENT);
            c.setRequestProperty("Range", "bytes=" + from + "-" + to);
            if (c.getResponseCode() != 206) {
                c.disconnect();
                throw new IOException("HTTP " + c.getResponseCode());
            }
            return c;
        }

        private synchronized int nextJob() {
            while (!closed) {
                int end = Math.min(chunks, readHead + WINDOW);
                for (int i = readHead; i < end; i++) {
                    if (state[i] == 0) {
                        state[i] = 1;
                        return i;
                    }
                }
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    return -1;
                }
            }
            return -1;
        }

        private void work() {
            byte[] buffer = new byte[CHUNK_SIZE];
            int job;
            while ((job = nextJob()) >= 0) {
                long from = (long) job * CHUNK_SIZE;
                long to = Math.min(length, from + CHUNK_SIZE) - 1;
                int size = (int) (to - from + 1);
                try {
                    HttpURLConnection c = open(from, to);
                    InputStream in = c.getInputStream();
                    int read = 0, n;
                    while (read < size && !closed && (n = in.read(buffer, read, size - read)) != -1) read += n;
                    in.close();
                    c.disconnect();
                    if (read < size) throw new IOException("Short read");
                    synchronized (this) {
                        if (closed) return;
                        file.seek(from);
                        file.write(buffer, 0, size);
                        state[job] = 2;
                        notifyAll();
                    }
                } catch (Exception e) {
                    synchronized (this) {
                        if (state != null && state[job] == 1) state[job] = 0;
                        notifyAll();
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                        return;
                    }
                }
            }
        }

        /** Writes the file from the given offset as the chunks arrive */
        void streamTo(OutputStream out, long start) throws IOException, InterruptedException {
            byte[] buffer = new byte[64 * 1024];
            long pos = start;
            synchronized (this) {
                readHead = (int) (pos / CHUNK_SIZE);
                notifyAll();
            }
            while (pos < length && !closed) {
                int chunk = (int) (pos / CHUNK_SIZE);
                int n;
                synchronized (this) {
                    if (chunk > readHead) {
                        readHead = chunk;
                        notifyAll();
                    }
                    while (state[chunk] != 2 && !closed) wait(500);
                    if (closed) return;
                    long chunkEnd = Math.min(length, (long) (chunk + 1) * CHUNK_SIZE);
                    n = (int) Math.min(buffer.length, chunkEnd - pos);
                    file.seek(pos);
                    file.readFully(buffer, 0, n);
                }
                out.write(buffer, 0, n);
                pos += n;
            }
            out.flush();
        }

        synchronized void close() {
            closed = true;
            notifyAll();
            try {
                if (file != null) file.close();
            } catch (IOException ignored) {}
            if (path != null) path.delete();
        }
    }
}
