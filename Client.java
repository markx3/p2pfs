import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.util.*;
import java.io.*;

import broadcast.BroadcastServer;
import broadcast.Peer;


class Client {
	private static LinkedList<String> peers;
	private static LinkedList<Metadata> metadados;

	public Client () {
		peers = new LinkedList<>();
		metadados = new LinkedList<>();
	}
	
	public static void main(String[] args) {
		try {
			URL url = new URL("http://127.0.0.1:9876/p2pfs?wsdl");
			QName qname = new QName("http://broadcast/", "BroadcastServerImplService");
			Service ws = Service.create(url, qname);
			BroadcastServer service = ws.getPort(BroadcastServer.class);
			if(service.helloPeer("teste")) {
				System.out.println("Oi");
			}
			if(service.helloPeer("teste2")) {
				System.out.println("Oi2");
			}
			RandomAccessFile raf = new RandomAccessFile("/tmp/teste.txt", "r");
			Metadata m = new Metadata("teste", raf.length());

			long sizePerChunk = raf.length()/3;
			long remainingBytes = raf.length() % 3;
			long maxReadBytes = 64 * 1024; // 8KB
			for(int i = 1; i <= 3; i++) {
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("split4"));
				if(sizePerChunk < maxReadBytes) {
					long numReads = sizePerChunk / maxReadBytes;
					long numRemainingReads = sizePerChunk % maxReadBytes;
					for(int j = 0; j <numReads; j++) {
						String str = new String(readWrite(raf, bos, maxReadBytes), "UTF-8");
						System.out.println(str);
					}
					if(numRemainingReads > 0) {
						String str = new String(readWrite(raf, bos, numRemainingReads), "UTF-8");
						System.out.println(str);
					}
				} else {
					readWrite(raf, bos, sizePerChunk);
				}
				bos.close();
			}
			if(remainingBytes > 0) {
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("split4"));	
				String str = new String(readWrite(raf, bos, remainingBytes), "UTF-8");
						System.out.println(str);
				bos.close();
			}
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static byte[] readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException {
		byte[] buf = new byte[(int) numBytes];
		int val = raf.read(buf);
		if(val != -1) return buf;
		return buf;
	}
}