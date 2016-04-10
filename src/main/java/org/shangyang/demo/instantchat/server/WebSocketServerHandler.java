/*
 * Copyright 2013-2018 Lilinfeng.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.shangyang.demo.instantchat.server;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.shangyang.demo.instantchat.OnlineCheckHelper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

/**
 * @author shangyang
 * @version 1.0
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
	
    private static final Logger logger = Logger.getLogger(WebSocketServerHandler.class.getName());

    private WebSocketServerHandshaker handshaker;

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
    	
		// 传统的HTTP接入，建立 socket 连接
		if (msg instanceof FullHttpRequest) {
		    handleHttpRequest(ctx, (FullHttpRequest) msg);
		}
		
		// WebSocket接入
		else if (msg instanceof WebSocketFrame) {
		    handleWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
		
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    	ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

		// 如果HTTP解码失败，返回HHTP异常
		if ( !req.getDecoderResult().isSuccess() || ( !"websocket".equals( req.headers().get("Upgrade") ) ) ) {
		    
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST) );
		    
		    return;
		}
	
		// 构造握手响应返回，本机测试
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://localhost:8081/websocket", null, false);
		
		handshaker = wsFactory.newHandshaker(req);
		
		if (handshaker == null) {
		    
			WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse( ctx.channel() );
		    
		} else {
		    
			handshaker.handshake(ctx.channel(), req);
		
		}
		
    }

    private void handleWebSocketFrame( ChannelHandlerContext ctx, WebSocketFrame frame ) {

		// 判断是否是关闭链路的指令
		if (frame instanceof CloseWebSocketFrame) {
			
		    handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
		    
		    return;
		    
		}
		
		// 判断是否是Ping消息
		if (frame instanceof PingWebSocketFrame) {
		    
			ctx.channel().write( new PongWebSocketFrame(frame.content().retain()) );
		    
		    return;
		    
		}
		
		// 本例程仅支持文本消息，不支持二进制消息
		if ( !(frame instanceof TextWebSocketFrame) ) {
		    throw new UnsupportedOperationException(String.format( "%s frame types not supported", frame.getClass().getName()) );
		}
	
		// 获取输入
		String input_txt = ( (TextWebSocketFrame) frame).text();
		
		handleInput( input_txt, ctx.channel() );

		if (logger.isLoggable(Level.FINE)) {
		    logger.fine(String.format("%s received %s", ctx.channel(), input_txt));
		}
		
		// 返回应答消息
		// ctx.channel().write( new TextWebSocketFrame(request + " , 欢迎使用Netty WebSocket服务，现在时刻：" + new java.util.Date().toString()));

    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
    	
		// 返回应答给客户端
		if ( res.getStatus().code() != 200 ) {
		
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),  CharsetUtil.UTF_8);
		    
		    res.content().writeBytes(buf);
		    
		    buf.release();
		    
		    setContentLength(res, res.content().readableBytes());
		    
		}
	
		// 如果是非 Keep-Alive，关闭连接
		
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			
		    f.addListener(ChannelFutureListener.CLOSE);
		    
		}
		
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	    throws Exception {
    	
		cause.printStackTrace();
		
		ctx.close();
		
    }
    
    // FIXME: to use JSON for message format instead
    static void handleInput(String txt, Channel channel){
    	
    	// register the connected user
    	if( txt.startsWith("user:") ){
    		
    		// indicates the user gets connected in
    		
    		String username = txt.replaceFirst("user:", "");
    		
    		OnlineCheckHelper.addOnlineUser( username, channel );
    		
    	}
    	
    	// send the message to the user
    	if( txt.startsWith("message:")){
    		
    		String message = txt.replaceFirst("message:", "").replaceFirst("send to:.*", "").trim();
    		
    		String toUser = txt.replaceFirst(".*send to:", "").trim();
    		
    		Channel toChannel = OnlineCheckHelper.getUserChannel(toUser);
    		
    		if( toChannel == null ){
    			
    			// scenario 2, 发送消息给离线用户
    			
    			System.out.println( " channel is empty " );
    			
    		}else{
    			
    			toChannel.write( new TextWebSocketFrame("message:"+ message +" from:" + OnlineCheckHelper.getOnlineUser(channel)  ) );
    			
    			//toChannel.write( new TextWebSocketFrame( message ) );
    			
    			toChannel.flush(); // must uses flush or else the message is cached not sent
    			
    			System.out.println(" message has been sent to "+ toUser);
    			
    		}
    		
    		
    	}
    	
    	
    }
    
}
