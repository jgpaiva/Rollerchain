/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package objectecho;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Handles both client-side and server-side handler depending on which
 * constructor was called.
 */
public class ObjectEchoServerHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = Logger.getLogger(
			ObjectEchoServerHandler.class.getName());

	private final AtomicLong transferredMessages = new AtomicLong();

	public long getTransferredMessages() {
		return this.transferredMessages.get();
	}

	@Override
	public void handleUpstream(
			ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent &&
				((ChannelStateEvent) e).getState() != ChannelState.INTEREST_OPS) {
			ObjectEchoServerHandler.logger.info(e.toString());
		}
		super.handleUpstream(ctx, e);
	}

	@Override
	public void messageReceived(
			ChannelHandlerContext ctx, MessageEvent e) {
		// Echo back the received object to the client.
		this.transferredMessages.incrementAndGet();
		Object tempObj = e.getMessage();
		if (tempObj instanceof MyEvent) {
			ObjectEchoServerHandler.logger.log(Level.INFO, "received event. id: "
					+ ((MyEvent) tempObj).getId());
		}
		e.getChannel().write(tempObj);
	}

	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, ExceptionEvent e) {
		ObjectEchoServerHandler.logger.log(
				Level.WARNING,
				"Unexpected exception from downstream.",
				e.getCause());
		e.getChannel().close();
	}
}
