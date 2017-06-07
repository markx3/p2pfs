import java.net.*;
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
	private static Hashtable<String,Metadata> metadadosHt = new Hashtable<String, Metadata>();
	private static String addr;
	private static BroadcastServer service;
	private static ServerSocket listener;
	private static Scanner scanner = new Scanner(System.in);

	public Client () throws IOException, ClassNotFoundException {
	}

	public static void main(String[] args) {
		try {
			if(verifyMetadata()) metadadosHt = recoverMetadata();
			URL url = new URL("http://127.0.0.1:9876/p2pfs?wsdl");
			QName qname = new QName("http://broadcast/", "BroadcastServerImplService");
			addr = getIp();
			Service ws = Service.create(url, qname);
			service = ws.getPort(BroadcastServer.class);
			service.helloPeer(addr);
			BroadcastServer service = ws.getPort(BroadcastServer.class);
			new Thread (getPeerList).start();
			listener = new ServerSocket(9877);
			//peers.remove(addr); // pode causar exception
			//System.out.println(peers.toString());
			//serializeMetadata();
			//readFileToBytes(service);
			//service.byePeer("teste");
			//serializeMetadata();
			//System.out.println("::");
			while(true) {
				menu();
				//System.out.println(metadados.get(0).getFilename());
				//System.out.println(metadados.getFirst().getFilename() + "\n" + metadados.size());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void menu () throws IOException, ClassNotFoundException {
		int opt = -1;
		System.out.println("1 - Armazenar arquivo.\n2 - Requisitar arquivo.\n3 - Listar arquivos.\n");
		opt = Integer.parseInt(scanner.nextLine());
		switch (opt) {
			case 1:
				storeFile();
				break;
			case 2:
				requestFile();
				break;
			case 3:
				listFiles();
				break;
			default:
				System.out.println("Opção inválida.");
		}
	}

	// Lê um arquivo, transforma em chunks, envia aos peers e serializa
	// Hashtable de metadados após término das operações.
	// IDEA Paralelizar envio de chunks aos peers.
	private static boolean storeFile() throws IOException, ClassNotFoundException {
		System.out.println("Insira o caminho para o arquivo.");
		String filename = scanner.nextLine();
		File file = new File(filename);
		if(!file.exists()) return false;
		readFileToBytes(filename);
		deliverChunksToPeers(filename); // TODO Enviar chunks aos peers.
		serializeMetadata();			// IDEA Após enviar chunks, deletar os
		return true;					// vetores byte[] de cada chunk do
										// cliente de origem

		// TODO Armazenar apenas nome do arquivo caso usuário passe um caminho.
		// Ex. /tmp/teste.txt (guardar apenas teste.txt)
	}

	private static boolean deliverChunksToPeers(String filename) {
		// TODO Método p/ enviar chunks aos peers (preferencialmente de forma
		// balanceada :>)
		return true;
	}

	private static boolean requestFile() {
		System.out.println("Insira o nome do arquivo a ser requisitado.");
		String filename = scanner.nextLine();
		if (!metadadosHt.containsKey(filename)) return false;
		Metadata meta = metadadosHt.get(filename);
		for (Data d : meta.getChunks()) {
			d.setData(requestChunk(d.getHashChunk(), d.getPeers())); // Pode dar exception!
		}

		// IDEA Pra cada chunk, seta de volta o byte[] (que foi deletado na
		// hora de enviar pros peers). Com todos os chunks reconstruímos o
		// arquivo e gg.
		// TODO Reconstruir arquivo a partir da lista de chunks

		return true;
	}

	// Faz requisição de um chunk
	// @params hash_chunk, peers (hash do chunk e lista de peers que possuem o chunk)
	private static byte[] requestChunk(long hash_chunk, LinkedList<String> peers) {
		// TODO Método p/ pedir chunks dos peers.
		// IDEA Armazenar lista com todos os hash_chunks que o cliente possui,
		// Fazer request pelo hash e tals.
		return null;
	}

	// Lista os nomes de arquivos de um cliente.
	private static void listFiles() {
		Set<String> keys = metadadosHt.keySet();
		System.out.println();
        for(String key: keys){
            System.out.println(key);
        }
		System.out.println();
	}

	static void readFileToBytes(String filename)  throws IOException {
		RandomAccessFile raf = new RandomAccessFile(filename, "r");
		Metadata m = new Metadata("metadado1", raf.length());
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
		metadadosHt.put(filename, m);
	}

	static byte[] readWrite(RandomAccessFile raf, long numBytes) throws IOException {
		byte[] buf = new byte[(int) numBytes];
		int val = raf.read(buf);
		if(val != -1) return buf;
		return buf;
	}

	private static Runnable getPeerList = new Runnable () {
		public void run() {
			try {
				while (true) {
					peers = service.getPeers(addr);
					System.out.println(peers.toString());
					Thread.sleep(30000);
				}
			} catch (Exception e) {}
		}
	};

	public static String getExtIp() throws Exception {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            String ip = in.readLine();
            return ip;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

	public static String getIp() throws Exception {
		String ret = null;
		try {
            InetAddress ipAddr = InetAddress.getLocalHost();
			ret = ipAddr.getHostAddress();
            System.out.println(ret);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
		return ret;
	}

	static void serializeMetadata() throws IOException, ClassNotFoundException {
		FileOutputStream fos = new FileOutputStream("/tmp/metadata.dat");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(metadadosHt);
		oos.flush();
		oos.close();
		fos.close();
	}

	static Hashtable <String,Metadata> recoverMetadata() throws IOException, FileNotFoundException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream("/tmp/metadata.dat");
		ObjectInputStream ois = new ObjectInputStream(fis);
		Hashtable <String,Metadata> aux = (Hashtable<String,Metadata>) ois.readObject();
		fis.close();
		ois.close();
		return aux;
	}

	static Boolean verifyMetadata() throws IOException, FileNotFoundException {
		File file = new File("/tmp/metadata.dat");
		if(file.exists()) return true;
		return false;
	}
}
