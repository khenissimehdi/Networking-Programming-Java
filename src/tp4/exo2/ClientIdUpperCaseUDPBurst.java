package tp4.exo2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientIdUpperCaseUDPBurst {

        private static Logger logger = Logger.getLogger(ClientIdUpperCaseUDPBurst.class.getName());
        private static final Charset UTF8 = StandardCharsets.UTF_8;
        private static final int BUFFER_SIZE = 1024;
        private final List<String> lines;
        private final int nbLines;
        private final String[] upperCaseLines; //
        private final int timeout;
        private final String outFilename;
        private final InetSocketAddress serverAddress;
        private final DatagramChannel dc;
        private final AnswersLog answersLog; // Thread-safe structure keeping track of missing responses
        private long requestId;

        public static void usage() {
            System.out.println("Usage : ClientIdUpperCaseUDPBurst in-filename out-filename timeout host port ");
        }

    private void sendMessage(DatagramChannel client, String msg, SocketAddress serverAddress, Charset charset, long rqId) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        var s = charset.encode(msg);
        buffer.putLong(rqId);
        buffer.put(s);
        buffer.flip();
        requestId = rqId;


        client.send(buffer, serverAddress);
    }
    private record Response(long id, String message) {
    };

    private ByteBuffer recieveMessage(DatagramChannel client) throws IOException {
        var buffer = ByteBuffer.allocate(BUFFER_SIZE);
        InetSocketAddress exp=(InetSocketAddress) client.receive(buffer);
        return buffer;
    }

        public ClientIdUpperCaseUDPBurst(List<String> lines,int timeout,InetSocketAddress serverAddress,String outFilename) throws IOException {
            this.lines = lines;
            this.nbLines = lines.size();
            this.timeout = timeout;
            this.outFilename = outFilename;
            this.serverAddress = serverAddress;
            this.dc = DatagramChannel.open();
            dc.bind(null);
            this.upperCaseLines = new String[nbLines];
            this.answersLog = new AnswersLog(nbLines);
        }

        private void senderThreadRun()  {
            while (!Thread.currentThread().isInterrupted()) {
                for (int i = 0; i < nbLines; i++) {
                    try {
                        if(!answersLog.getBitAtIndex(i)) {
                            sendMessage(dc,lines.get(i), serverAddress, UTF8, i);
                        }
                    }catch (IOException e) {
                        throw new AssertionError(e.getMessage());
                    }
                }
                try{
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        public void launch() throws IOException  {
            Thread senderThread = new Thread(this::senderThreadRun);
            senderThread.start();

            while (answersLog.getCard() != nbLines) {
                var buffer = recieveMessage(dc);
                    buffer.flip();
                    var i = (int)buffer.getLong();
                    answersLog.setBitSet(i);
                    buffer.position(Long.BYTES);
                    var res = UTF8.decode(buffer).toString();
                    System.out.println(res);
                    upperCaseLines[i] = res;

            }

            Files.write(Paths.get(outFilename),Arrays.asList(upperCaseLines), UTF8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            senderThread.interrupt();
        }

        public static void main(String[] args) throws IOException, InterruptedException {
            if (args.length !=5) {
                usage();
                return;
            }

            String inFilename = args[0];
            String outFilename = args[1];
            int timeout = Integer.valueOf(args[2]);
            String host=args[3];
            int port = Integer.valueOf(args[4]);
            InetSocketAddress serverAddress = new InetSocketAddress(host,port);

            // Read all lines of inFilename opened in UTF-8
            List<String> lines= Files.readAllLines(Paths.get(inFilename),UTF8);
            // Create client with the parameters and launch it
            ClientIdUpperCaseUDPBurst client = new ClientIdUpperCaseUDPBurst(lines,timeout,serverAddress,outFilename);
            client.launch();

        }

        private static class AnswersLog {
            private final BitSet bitSet;
            private final Object lock = new Object();


            public AnswersLog(int nbLines) {
                synchronized (lock) {
                    this.bitSet = new BitSet(nbLines);
                }
            }

            public void setBitSet(int i) {
                synchronized (lock) {
                    bitSet.set(i);
                }
            }

            public boolean getBitAtIndex(int i) {
                synchronized (lock) {
                    return bitSet.get(i) ;
                }
            }

            public int getCard() {
                synchronized (lock) {
                    return bitSet.cardinality();
                }
            }

            public BitSet getBitSet() {
                synchronized (lock) {
                    return bitSet;
                }
            }
        }
    }

