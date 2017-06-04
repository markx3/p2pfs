import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.util.*;
import java.io.*;
import java.nio.file.*;

import broadcast.BroadcastServer;
import broadcast.Peer;


class Client {
	private static LinkedList<String> peers;
	private static LinkedList<Metadata> metadados = new LinkedList<>();

	public Client () throws IOException, ClassNotFoundException {
		peers = new LinkedList<>();
		if(verifyMetadata()) recoverMetadata();
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
			peers = service.getPeers("teste");
			System.out.println(peers.toString());
			System.out.println(metadados.get(0).getFilename());
			readFileToBytes(service);
			service.byePeer("teste");
			serializeMetadata();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void readFileToBytes(BroadcastServer service)  throws IOException {
		RandomAccessFile raf = new RandomAccessFile("/tmp/teste.txt", "r");
		Metadata m = new Metadata("teste", raf.length());
		long sizePerChunk = raf.length()/3;
		long remainingBytes = raf.length() % 3;
		long maxReadBytes = 64 * 1024; // 8KB
		for(int i = 1; i <= 3; i++) {
			if(sizePerChunk < maxReadBytes) {
				long numReads = sizePerChunk / maxReadBytes;
				long numRemainingReads = sizePerChunk % maxReadBytes;
				for(int j = 0; j <numReads; j++) {
					byte[] d = readWrite(raf, maxReadBytes);
					m.addChunk(new Data(d));
				}
				if(numRemainingReads > 0) {
					byte[] d = readWrite(raf, numRemainingReads);
					m.addChunk(new Data(d));
				}
			} else {
				byte[] d = readWrite(raf, sizePerChunk);
				m.addChunk(new Data(d));
			}
			
		}
		if(remainingBytes > 0) {
			byte[] d = readWrite(raf, remainingBytes);
			m.addChunk(new Data(d));
		}
		for(int i = 0; i < m.getChunks().size(); i++)
			System.out.println(new String(m.getChunks().get(i).getData(), "UTF-8"));
		metadados.add(m);
		raf.close();
	}

	static void serializeMetadata() throws IOException {
		FileOutputStream idx = new FileOutputStream("/tmp/metadata.bin");
		ObjectOutputStream oos = new ObjectOutputStream(idx);
		oos.writeObject(metadados);
		oos.flush();
		idx.close();
		oos.close();
	}

	private Boolean verifyMetadata() throws IOException, FileNotFoundException {
		File idx = new File("/tmp/metadata.bin");
		if(!idx.exists()) return false;
		return true;
	}

	private void recoverMetadata() throws IOException, FileNotFoundException, ClassNotFoundException {
		FileInputStream m = new FileInputStream("/tmp/metadata.bin");
		ObjectInputStream ois = new ObjectInputStream(m);
		metadados = (LinkedList<Metadata>) ois.readObject();
		ois.close();
		m.close();
	}

	static byte[] readWrite(RandomAccessFile raf, long numBytes) throws IOException {
		byte[] buf = new byte[(int) numBytes];
		int val = raf.read(buf);
		if(val != -1) return buf;
		return buf;
	}
}
