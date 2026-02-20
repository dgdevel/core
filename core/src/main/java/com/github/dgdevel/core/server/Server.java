package com.github.dgdevel.core.server;

import com.github.dgdevel.core.db.DatabaseManager;
import com.github.dgdevel.core.jsonrpc.JsonRpcHandler;
import com.github.dgdevel.core.msgpack.MsgPackHandler;
import com.github.dgdevel.core.registry.AuthenticationRegistry;
import com.github.dgdevel.core.registry.AuthorizationRegistry;
import com.github.dgdevel.core.registry.GenericRegistry;
import com.github.dgdevel.core.registry.UserRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public class Server {
    private final int jsonRpcPort;
    private final int msgPackPort;
    private final String dbUrl;
    private final DatabaseManager databaseManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel jsonRpcChannel;
    private Channel msgPackChannel;

    public Server(int jsonRpcPort, int msgPackPort, String dbUrl) {
        this.jsonRpcPort = jsonRpcPort;
        this.msgPackPort = msgPackPort;
        this.dbUrl = dbUrl;
        this.databaseManager = new DatabaseManager(dbUrl);
    }

    public Server(int port, String dbUrl) {
        this(port, port + 1, dbUrl);
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

            jsonRpcChannel = jsonRpcBootstrap.bind(jsonRpcPort).sync().channel();
            System.out.println("JSON-RPC Server started on port " + jsonRpcPort);

            msgPackChannel = msgPackBootstrap.bind(msgPackPort).sync().channel();
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
        int jsonRpcPort = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int msgPackPort = args.length > 1 ? Integer.parseInt(args[1]) : jsonRpcPort + 1;
        String dbUrl = args.length > 2 ? args[2] : "jdbc:h2:mem:test";

        Server server = new Server(jsonRpcPort, msgPackPort, dbUrl);
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
