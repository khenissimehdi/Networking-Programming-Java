package tp3.exo1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientUpperCaseUDPTimeout {
    public static final int BUFFER_SIZE = 1024;

    private static void usage() {
        System.out.println("Usage : NetcatUDP host port charset");
    }

    public static DatagramChannel openChannel() throws IOException {
        DatagramChannel datagramChannel = DatagramChannel.open();
        return datagramChannel;
    }

    public static DatagramChannel bindChannel(SocketAddress local) throws IOException {
        return openChannel().bind(local);
    }

    public static void sendMessage(DatagramChannel client, String msg, SocketAddress serverAddress, Charset charset) throws IOException {
        ByteBuffer buffer =  charset.encode(msg);
        client.send(buffer, serverAddress);
    }

    public  static InetSocketAddress recieveMessage(DatagramChannel client, ByteBuffer buffer) throws IOException {
        InetSocketAddress exp=(InetSocketAddress) client.receive(buffer);
        buffer.flip();
        return exp;
    }

    public  static void printBuffer(ByteBuffer buffer, Charset charset) {
        var stringBuilder = new StringBuilder();

        var cb = charset.decode(buffer);
        stringBuilder.append(cb);
        buffer.clear();

        System.out.println(stringBuilder);
    }

    public static String getBufferValue(ByteBuffer buffer, Charset charset) {
        var stringBuilder = new StringBuilder();
        var cb = charset.decode(buffer);
        stringBuilder.append(cb);
        buffer.clear();
        return stringBuilder.toString();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usage();
            return;
        }


        var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        var cs = Charset.forName(args[2]);
        var buffer = ByteBuffer.allocate(BUFFER_SIZE);
        var abq = new ArrayBlockingQueue<String>(10);
        String msg = null;


        try (var scanner = new Scanner(System.in)) {
            var client = openChannel().bind(null);
            var listener = new Thread(() -> {
                InetSocketAddress exp = null;
                try {
                    while (!Thread.currentThread().isInterrupted()){
                        exp = recieveMessage(client, buffer);
                        abq.put(getBufferValue(buffer,cs));
                        System.out.println("Received "+ buffer.remaining() + " bytes from "+ getBufferValue(buffer,cs));
                    }

                } catch (IOException | InterruptedException e) {
                    throw new AssertionError();
                }
                /*
                  try catch in recieve for bad case and normal case
                 * */
                // NetcatUDP.printBuffer(buffer, cs);
            });
            listener.start();

            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();

                sendMessage(client,line,server,cs);

                msg = abq.poll(1, TimeUnit.SECONDS);
                if (msg == null) {
                    sendMessage(client, line, server, cs);
                } else {
                    System.out.println(msg);
                }
            }
        } catch (InterruptedException e) {
            throw new AssertionError();
        }
    }
}
