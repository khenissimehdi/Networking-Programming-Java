package tp4.exo1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.*;

public class ClientIdUpperCaseUDPOneByOne {

	private static Logger logger = Logger.getLogger(ClientIdUpperCaseUDPOneByOne.class.getName());
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final int BUFFER_SIZE = 1024;

	private void sendMessage(DatagramChannel client, String msg, SocketAddress serverAddress, Charset charset) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		var s = charset.encode(msg);
		buffer.putLong(requestId);
		buffer.put(s);
		buffer.flip();


		client.send(buffer, serverAddress);
	}

	private ByteBuffer recieveMessage(DatagramChannel client) throws IOException {
		var buffer = ByteBuffer.allocate(BUFFER_SIZE);
		InetSocketAddress exp=(InetSocketAddress) client.receive(buffer);
		return buffer;
	}

	private record Response(long id, String message) {
	};

	private final String inFilename;
	private final String outFilename;
	private final long timeout;
	private final InetSocketAddress server;
	private final DatagramChannel dc;
	private long requestId;
	private final SynchronousQueue<Response> queue = new SynchronousQueue<>();


	public static void usage() {
		System.out.println("Usage : ClientIdUpperCaseUDPOneByOne in-filename out-filename timeout host port ");
	}

	public ClientIdUpperCaseUDPOneByOne(String inFilename, String outFilename, long timeout, InetSocketAddress server)
			throws IOException {
		this.inFilename = Objects.requireNonNull(inFilename);
		this.outFilename = Objects.requireNonNull(outFilename);
		this.timeout = timeout;
		this.server = server;
		this.dc = DatagramChannel.open();
		dc.bind(null);
	}

	private String getBufferValue(ByteBuffer buffer, Charset charset) {
		buffer.position(Long.BYTES);
		var cb = charset.decode(buffer);

		return cb.toString();
	}

	private void listenerThreadRun() {
		try {

			while (!Thread.currentThread().isInterrupted()) {
				var buffer = recieveMessage(dc);
				buffer.flip();
				queue.put(new Response(buffer.getLong(), getBufferValue(buffer, UTF8)));
				// ANY MANIPULATION OF THE BUFFER AFTER THE PUT IS BAD !
			}

		} catch (InterruptedException e ) {
				logger.log(Level.INFO, e.getMessage());
		}  catch (SecurityException e ) {
				logger.log(Level.INFO, "Program terminated");
		} catch ( AlreadyConnectedException | UnresolvedAddressException | UnsupportedAddressTypeException e) {
				logger.log(Level.WARNING, e.getMessage());
		} catch (IOException e) {
				logger.log(Level.SEVERE, e.getMessage());
		}
	}

	public void launch() throws IOException {
		try {

			var listenerThread = new Thread(this::listenerThreadRun);
			listenerThread.start();
			var lines = Files.readAllLines(Path.of(inFilename), UTF8);

			var upperCaseLines = new ArrayList<String>();

			while (requestId < lines.size()) {
				try {
					sendMessage(dc,lines.get((int) requestId),server, UTF8);

					var sendTime = System.currentTimeMillis();
					var nowTime = System.currentTimeMillis();


					while(nowTime - sendTime  <=  timeout ) {

						var upperCaseLine = queue.poll(timeout - ( nowTime -  sendTime), TimeUnit.MILLISECONDS);
						nowTime = System.currentTimeMillis();

						if(upperCaseLine != null && upperCaseLine.id == requestId) {
							upperCaseLines.add(upperCaseLine.message);
							requestId++;
							break;
						}

					}


				} catch (InterruptedException e  ) {
					logger.log(Level.INFO, e.getMessage());
				} catch (IOException e) {
					logger.log(Level.SEVERE, e.getMessage());
				}
			}

		    listenerThread.interrupt();
			Files.write(Paths.get(outFilename), upperCaseLines, UTF8, CREATE, WRITE, TRUNCATE_EXISTING);
		} finally {
			dc.close();
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 5) {
			usage();
			return;
		}

		var inFilename = args[0];
		var outFilename = args[1];
		var timeout = Long.parseLong(args[2]);
		var server = new InetSocketAddress(args[3], Integer.parseInt(args[4]));

		// Create client with the parameters and launch it
		new ClientIdUpperCaseUDPOneByOne(inFilename, outFilename, timeout, server).launch();
	}
}
