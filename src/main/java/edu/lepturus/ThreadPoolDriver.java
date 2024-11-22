package edu.lepturus;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于ExecutorService线程池的驱动程序
 *
 * @author T.lepturus
 * @version 1.0
 */
public class ThreadPoolDriver {
    private final ExecutorService executorService;
    private static final int POOL_SIZE = 4;

    public ThreadPoolDriver() {
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * POOL_SIZE);
    }

    /**
     * 不断接收TCP连接并启动线程进行处理
     *
     * @param port 监听的端口
     */
    public void run(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    executorService.execute(new SimpleProxyServer(socket));
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * args[0]: 代理监听的端口
     * @param args args
     */
    public static void main(String[] args) {
        try {
            new ThreadPoolDriver().run(Integer.parseInt(args[0]));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
