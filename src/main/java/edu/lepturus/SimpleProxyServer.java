package edu.lepturus;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 服务器线程执行
 *
 * @author T.lepturus
 * @version 1.0
 */
public class SimpleProxyServer implements Runnable {
    private static final int BUFFER_SIZE = 8192;
    private static final String CRLF = "\r\n";
    private final Socket clientSocket;

    public SimpleProxyServer(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * HTTP Status 501
     */
    private static class NotImplementedException extends Exception {
        public NotImplementedException(String message) {
            super(message);
        }
    }

    /**
     * InetAddress with Port
     */
    private static class InetPortPair {
        private InetAddress inetAddress;
        private int port;

        public InetPortPair() {
        }

        public InetPortPair(InetAddress inetAddress, int port) {
            this.inetAddress = inetAddress;
            this.port = port;
        }

        public InetAddress getInetAddress() {
            return this.inetAddress;
        }

        public InetPortPair setInetAddress(InetAddress inetAddress) {
            this.inetAddress = inetAddress;
            return this;
        }

        public int getPort() {
            return this.port;
        }

        public InetPortPair setPort(int port) {
            this.port = port;
            return this;
        }
    }

    @Override
    public void run() {
        try (InputStream clientInputStream = this.clientSocket.getInputStream();
             OutputStream clientOutputStream = this.clientSocket.getOutputStream()) {
            try {
                InetPortPair inetPortPair = new InetPortPair();
                byte[] requestBytes = requestConversion(clientInputStream, inetPortPair);
                try (Socket socket = new Socket(inetPortPair.getInetAddress(), inetPortPair.getPort());
                     OutputStream serverOutputStream = socket.getOutputStream();
                     InputStream serverInputStream = socket.getInputStream()) {
                    System.out.println("Sending request to server" + inetPortPair.getInetAddress()
                            + ":" + inetPortPair.getPort() + "...");
                    System.out.println(new String(requestBytes, StandardCharsets.UTF_8));
                    serverOutputStream.write(requestBytes);
                    int readBytes;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    System.out.println("Waiting for response from server " + inetPortPair.getInetAddress()
                            + ":" + inetPortPair.getPort());
                    while ((readBytes = serverInputStream.read(buffer)) != -1) {
                        clientOutputStream.write(buffer, 0, readBytes);
                    }
                    System.out.println("End of this connection to server " + inetPortPair.getInetAddress()
                            + ":" + inetPortPair.getPort());
                }
            } catch (NotImplementedException e) {
                response501(clientOutputStream);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * 将客户端请求头和请求行规范化，使用HTTP1.0，并转换为byte[]，同时解析目标host&port
     *
     * @param inputStream 输入流
     * @param inetPortPair 修改为解析出的inetPortPair
     * @return 要发给服务器的请求
     * @throws IOException IOException
     * @throws NotImplementedException 501
     */
    private byte[] requestConversion(InputStream inputStream, InetPortPair inetPortPair)
            throws IOException, NotImplementedException {
        String header = getHeader(inputStream);
        String[] lines = header.split(CRLF);
        if (lines.length > 1 && header.startsWith("GET")) {
            InetPortPair assignmentHelper = parseInetPortPair(lines);
            inetPortPair
                    .setInetAddress(assignmentHelper.getInetAddress())
                    .setPort(assignmentHelper.getPort());
            return header.replaceFirst("HTTP/1\\.1", "HTTP/1.0").getBytes(StandardCharsets.UTF_8);
        } else if (lines.length > 1) {
            throw new NotImplementedException("501 Not Implemented");
        } else {
            throw new IOException("Error in request conversion: invalid header");
        }
    }

    /**
     * 从客户端请求解析出请求头和请求行
     *
     * @param inputStream 输入流
     * @return 解析结果
     * @throws IOException IOException
     */
    private String getHeader(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int last = 0, c;
        boolean inHeader = true;
        while (inHeader && ((c = inputStream.read()) != -1)) {
            switch (c) {
                case '\r':
                    break;
                case '\n':
                    if (c == last) {
                        inHeader = false;
                        break;
                    }
                    last = c;
                    stringBuilder.append(CRLF);
                    break;
                default:
                    last = c;
                    stringBuilder.append((char) c);
            }
        }
        stringBuilder.append(CRLF);
        System.out.println("Client said:");
        System.out.println(stringBuilder);
        return stringBuilder.toString();
    }

    /**
     * 给客户端响应501
     *
     * @param outputStream 输出流
     * @throws IOException IOException
     */
    private void response501(OutputStream outputStream) throws IOException {
        String RESPONSE = "HTTP/1.0 501 Not Implemented" + CRLF + CRLF;
        outputStream.write(RESPONSE.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 从请求行中解析目标host&port
     *
     * @param lines 请求头和请求行
     * @return InetPortPair
     * @throws IOException IOException
     */
    InetPortPair parseInetPortPair(String[] lines) throws IOException {
        for (String line : lines) {
            if (line.startsWith("Host: ")) {
                String[] ipp = line.substring(6).trim().split(":");
                return new InetPortPair(
                        InetAddress.getByName(ipp[0]),
                        ipp.length > 1 ? Integer.parseInt(ipp[1]) : 80);
            }
        }
        throw new IOException("Invalid request");
    }
}
