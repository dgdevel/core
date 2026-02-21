package com.github.dgdevel.core.server;

import com.github.dgdevel.core.config.Config;
import com.github.dgdevel.core.db.DatabaseManager;
import com.github.dgdevel.core.jsonrpc.JsonRpcHandler;
import com.github.dgdevel.core.msgpack.MsgPackHandler;
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
    private final String bindAddress;
    private final int jsonRpcPort;
    private final int msgPackPort;
    private final String dbUrl;
    private final DatabaseManager databaseManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel jsonRpcChannel;
    private Channel msgPackChannel;

    public Server(String bindAddress, int jsonRpcPort, int msgPackPort, String dbUrl, String dbUsername, String dbPassword) {
        this.bindAddress = bindAddress;
        this.jsonRpcPort = jsonRpcPort;
        this.msgPackPort = msgPackPort;
        this.dbUrl = dbUrl;
        this.databaseManager = new DatabaseManager(dbUrl, dbUsername, dbPassword);
    }

    public Server(int jsonRpcPort, int msgPackPort, String dbUrl, String dbUsername, String dbPassword) {
        this("0.0.0.0", jsonRpcPort, msgPackPort, dbUrl, dbUsername, dbPassword);
    }

    public Server(int port, String dbUrl, String dbUsername, String dbPassword) {
        this("0.0.0.0", port, port + 1, dbUrl, dbUsername, dbPassword);
    }

    public Server(int port, String dbUrl) {
        this("0.0.0.0", port, port + 1, dbUrl, null, null);
    }

    public void start() throws Exception {
        databaseManager.connect();

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            final UserRegistry userRegistry = new UserRegistry(databaseManager.getConnection());
            final AuthenticationRegistry authenticationRegistry = new AuthenticationRegistry(databaseManager.getConnection());
            final AuthorizationRegistry authorizationRegistry = new AuthorizationRegistry(databaseManager.getConnection());
            final GenericRegistry genericRegistry = new GenericRegistry(databaseManager.getConnection());

            ServerBootstrap jsonRpcBootstrap = new ServerBootstrap();
            jsonRpcBootstrap.option(ChannelOption.SO_BACKLOG, 1024)
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
                                userRegistry,
                                authenticationRegistry,
                                authorizationRegistry,
                                genericRegistry));
                   }
              });

            ServerBootstrap msgPackBootstrap = new ServerBootstrap();
            msgPackBootstrap.option(ChannelOption.SO_BACKLOG, 1024)
             .group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                   @Override
                   protected void initChannel(SocketChannel ch) {
                       ch.pipeline().addLast(new MsgPackHandler(
                           databaseManager,
                           userRegistry,
                           authenticationRegistry,
                           authorizationRegistry,
                           genericRegistry));
                   }
              });

            jsonRpcChannel = jsonRpcBootstrap.bind(bindAddress, jsonRpcPort).sync().channel();
            System.out.println("JSON-RPC Server started on port " + jsonRpcPort);

            msgPackChannel = msgPackBootstrap.bind(bindAddress, msgPackPort).sync().channel();
            System.out.println("MessagePack Server started on port " + msgPackPort);
            System.out.println("Database url: " + dbUrl);

        } catch (Exception e) {
            shutdown();
            throw e;
        }
    }

    public void shutdown() {
        if (jsonRpcChannel != null) {
            jsonRpcChannel.close();
        }
        if (msgPackChannel != null) {
            msgPackChannel.close();
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
        Config config = Config.load(args);

        Server server = new Server(config.getBindAddress(), config.getJsonRpcPort(), config.getMsgPackPort(), config.getDbUrl(), config.getDbUsername(), config.getDbPassword());
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            server.shutdown();
        }));

        server.getJsonRpcChannel().closeFuture().sync();
    }

    public io.netty.channel.Channel getJsonRpcChannel() {
        return jsonRpcChannel;
    }

    public io.netty.channel.Channel getMsgPackChannel() {
        return msgPackChannel;
    }
}
