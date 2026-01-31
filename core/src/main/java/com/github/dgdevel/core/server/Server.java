package com.github.dgdevel.core.server;

import com.github.dgdevel.core.db.DatabaseManager;
import com.github.dgdevel.core.jsonrpc.JsonRpcHandler;
import com.github.dgdevel.core.registry.AuthenticationRegistry;
import com.github.dgdevel.core.registry.AuthorizationRegistry;
import com.github.dgdevel.core.registry.GenericRegistry;
import com.github.dgdevel.core.registry.UserRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class Server {
    private final int port;
    private final String dbUrl;
    private final DatabaseManager databaseManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public Server(int port, String dbUrl) {
        this.port = port;
        this.dbUrl = dbUrl;
        this.databaseManager = new DatabaseManager(dbUrl);
    }

    public void start() throws Exception {
        databaseManager.connect();

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024)
             .group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                   @Override
                   protected void initChannel(SocketChannel ch) {
                       ch.pipeline().addLast(new HttpRequestDecoder());
                       ch.pipeline().addLast(new HttpObjectAggregator(65536));
                       ch.pipeline().addLast(new HttpResponseEncoder());
                        ch.pipeline().addLast(
                            new JsonRpcHandler(
                                databaseManager,
                                new UserRegistry(databaseManager.getConnection()),
                                new AuthenticationRegistry(databaseManager.getConnection()),
                                new AuthorizationRegistry(databaseManager.getConnection()),
                                new GenericRegistry(databaseManager.getConnection())));
                   }
             });

            channel = b.bind(port).sync().channel();
            System.out.println("JSON-RPC Server started on port " + port + "\nDatabase url: " + dbUrl);

        } catch (Exception e) {
            shutdown();
            throw e;
        }
    }

    public void shutdown() {
        if (channel != null) {
            channel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        try {
            databaseManager.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String dbUrl = args.length > 1 ? args[1] : "jdbc:h2:mem:test";

        Server server = new Server(port, dbUrl);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            server.shutdown();
        }));

        server.getChannel().closeFuture().sync();
    }

    public io.netty.channel.Channel getChannel() {
        return channel;
    }
}
