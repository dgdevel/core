package com.github.dgdevel.core.server;

import com.github.dgdevel.core.db.DatabaseManager;
import com.github.dgdevel.core.registry.FilesRegistry;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.Map;

public class FileResource extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final DatabaseManager databaseManager;
    private final FilesRegistry filesRegistry;

    public FileResource(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.filesRegistry = new FilesRegistry(databaseManager.getConnection());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        if (request.method() != HttpMethod.GET) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        String uri = request.uri();
        if (!uri.startsWith("/files/")) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        try {
            String fileIdStr = uri.substring(7);
            long fileId = Long.parseLong(fileIdStr);
            
            com.github.dgdevel.core.model.File file = filesRegistry.findById(fileId);
            
            if (file == null) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }

            String payload = file.getPayload();
            if (payload == null || payload.isEmpty()) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }

            byte[] data = payload.getBytes();
            FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(data)
            );
            
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
            
            ctx.writeAndFlush(response);
            
        } catch (NumberFormatException e) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(status.toString(), CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
