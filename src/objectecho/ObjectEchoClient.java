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

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

/**
 * Modification of {@link EchoClient} which utilizes Java object serialization.
 */
public class ObjectEchoClient implements Runnable {
	private static final Logger logger = Logger.getLogger(
			ObjectEchoClient.class.getName());

	private final String host;
	private final int port;
	private final int id;

	public ObjectEchoClient(String host, int port, int id) {
		this.host = host;
		this.port = port;
		this.id = id;
	}

	public void run() {
		// Configure the client.
		ClientBootstrap bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
						new ObjectEncoder(),
						new ObjectDecoder(
								ClassResolvers.cacheDisabled(this.getClass().getClassLoader())),
						new ObjectEchoClientHandler(ObjectEchoClient.this.id));
			}
		});

		// Start the connection attempt.
		bootstrap.connect(new InetSocketAddress(this.host, this.port));
	}

	public static void main(String[] args) throws Exception {
		// Parse options.
		final String host = "localhost";
		final int port = 8080;

		ObjectEchoClient a = new ObjectEchoClient(host, port, 0);
		Thread ta = new Thread(a);
		ta.run();

		ObjectEchoClient b = new ObjectEchoClient(host, port, 1);
		Thread tb = new Thread(b);
		tb.run();

		ta.join();
		ObjectEchoClient.logger.log(Level.INFO, "TA done");
		tb.join();
		ObjectEchoClient.logger.log(Level.INFO, "TB done");
	}
}
