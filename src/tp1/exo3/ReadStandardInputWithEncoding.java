package tp1.exo3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

public class ReadStandardInputWithEncoding {

	private static final int BUFFER_SIZE = 1024;

	private static void usage() {
		System.out.println("Usage: ReadStandardInputWithEncoding charset");
	}

	private static String stringFromStandardInput(Charset cs) throws IOException {

		var path = Path.of("./cutput.txt");
		try(FileChannel fc = FileChannel.open(path, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE);
			ReadableByteChannel in = Channels.newChannel(System.in);) {
			var buff = ByteBuffer.allocate(1024);

			while (in.read(buff) != -1) {
		       if ( !buff.hasRemaining() ) {
					buff = ByteBuffer.allocate(buff.capacity() * 2);
				}
				buff.flip();
				fc.write(buff);
				buff.clear();
			}
		}
		return null;
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			usage();
			return;
		}
		Charset cs = Charset.forName(args[0]);
		System.out.print(stringFromStandardInput(cs));
	}
}