package tp6.exo3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Objects;
import java.util.logging.Logger;

public class ServerEchoMultiPort {
    private static final Logger logger = Logger.getLogger(ServerEchoMultiPort.class.getName());

    private final Selector selector;
    private final int BUFFER_SIZE = 1024;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final  int startPort;
    private final int endPort;

    private class Context {
         private ByteBuffer buffer;
         private InetSocketAddress ip;

         public Context(ByteBuffer buffer, InetSocketAddress ip) {
             this.buffer = Objects.requireNonNull(buffer);
             this.ip = Objects.requireNonNull(ip);
         }

         public Context() {
            this.buffer =  null;
            this.ip  = null;
         }

        public ByteBuffer getBuffer() {
            return buffer;
        }

        public InetSocketAddress getIp() {
            return ip;
        }
    }



    public ServerEchoMultiPort(int startPort, int endPort) throws IOException {
        selector = Selector.open();
        this.startPort = startPort;
        this.endPort = endPort;
        for (int i = startPort; i <= endPort; i++) {
            var uniqueDc = DatagramChannel.open();
            var port = new InetSocketAddress(i);
            uniqueDc.bind(port);
            uniqueDc.configureBlocking(false);

            // SelectionKey.OP_READ -> This depends on the state of the server, its echo, so you have to wait a value
            uniqueDc.register(selector, SelectionKey.OP_READ, new Context());
        }
    }



    public void serve() throws IOException {
        logger.info("ServerEcho started on ports starting from " + startPort + " to " + endPort );
        while (!Thread.interrupted()) {
            try {
                selector.select(this::treatKey);
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }

        }
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isWritable()) {
                doWrite(key);
            }
            if (key.isValid() && key.isReadable()) {
                doRead(key);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private void doRead(SelectionKey key) throws IOException {

         var dc  = (DatagramChannel) key.channel();
         var data = ByteBuffer.allocateDirect(BUFFER_SIZE);
         var sender = dc.receive(data);
        if(sender == null) {
            System.out.println("i got nth");
        } else {
            key.interestOps(SelectionKey.OP_WRITE);

            key.attach(new Context(data, (InetSocketAddress) sender));
            data.flip(); // we have to put this here cause we are sure we got the value and that we are going to the onWrite and not coming back to the OnRead

        }
    }

    private void doWrite(SelectionKey key) throws IOException {
        var context = (Context)key.attachment();
        if(context.ip != null) {
            // No Flip here you dumb fuck you gonna flip the shit two times
            var dc  = (DatagramChannel) key.channel();
            dc.send(context.buffer, context.ip);
            if(context.buffer.remaining() != 0) {
              System.out.println("nth was sent");

            } else {
                context.buffer.clear();
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    public static void usage() {
        System.out.println("Usage : ServerMulti need starting port and ending port");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            usage();
            return;
        }
        new ServerEchoMultiPort(Integer.parseInt(args[0]), Integer.parseInt(args[1])).serve();
    }
}