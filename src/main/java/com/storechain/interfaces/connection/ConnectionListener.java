package com.storechain.interfaces.connection;

import com.storechain.connection.InboundPacket;
import com.storechain.connection.netty.NettyNioSocketChannel;
import com.storechain.connection.netty.NettyServer;

import io.netty.channel.Channel;

public interface ConnectionListener {
	
	public void onEvent(Channel channel, InboundPacket packet);

	public NettyServer getServer();
}
