package tp2.exo1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Scanner;


public class NetcatUDP {
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

    public  static  InetSocketAddress recieveMessage(DatagramChannel client, ByteBuffer buffer) throws IOException {
        InetSocketAddress exp=(InetSocketAddress) client.receive(buffer);
        buffer.flip();
        return exp;
    }

    public  static void printBuffer(ByteBuffer buffer, Charset charset) {
        var stringBuilder = new StringBuilder();
       // buffer.flip();
        var cb = charset.decode(buffer);
        stringBuilder.append(cb);
        buffer.clear();

        System.out.println(stringBuilder);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usage();
            return;
        }

        var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        var cs = Charset.forName(args[2]);
        var buffer = ByteBuffer.allocate(BUFFER_SIZE);

        try (var scanner = new Scanner(System.in);) {
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                try(var client = NetcatUDP.openChannel().bind(null)) {
                    NetcatUDP.sendMessage(client,line,server,cs);
                    InetSocketAddress exp = NetcatUDP.recieveMessage(client, buffer);
                    System.out.println("Received "+ buffer.remaining() + " bytes from "+ exp);
                    NetcatUDP.printBuffer(buffer, cs);
                }
            }
        }
    }
}