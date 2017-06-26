import java.net.*;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.util.*;
import java.io.*;
import java.nio.file.*;

import broadcast.BroadcastServer;
import broadcast.Peer;
import broadcast.Config;

/*
 * IDEA Usar portas diferentes (pre-definidas) em diferentes ServerSockets
 * 		p/ diferentes tipos de requisições. Vamos usar TCP ou UDP?
 */

public class ClientImpl {


	private LinkedList<String> peers;
	private Hashtable<String,Metadata> metadados = new Hashtable<String, Metadata>();
	public static String addr;
	private BroadcastServer service;
	private ChunkServer chunkServer;
	private Scanner scanner = new Scanner(System.in);
	private FileHandler fileHandler = new FileHandler();


	public ClientImpl () throws IOException, ClassNotFoundException {

	}

	public void run() {
		try {
			metadados = fileHandler.recoverMetadata();
			URL url = new URL("http://"+ Config.wsIp + ":9876/p2pfs?wsdl");
			QName qname = new QName("http://broadcast/", "BroadcastServerImplService");
			addr = getIp();
			chunkServer = new ChunkServer(addr);
			Service ws = Service.create(url, qname);
			service = ws.getPort(BroadcastServer.class);
			service.helloPeer(addr);
			BroadcastServer service = ws.getPort(BroadcastServer.class);
			new Thread (getPeerList).start();
			//peers.remove(addr); // pode causar exception
			//System.out.println(peers.toString());
			//serializeMetadata();
			//readFileToBytes(service);
			//service.byePeer("teste");
			//serializeMetadata();
			//System.out.println("::");
			while(true) {
				menu();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void menu () throws IOException, ClassNotFoundException, InterruptedException {
		int opt = -1;
		System.out.println("\n1 - Armazenar arquivo.\n2 - Requisitar arquivo.\n3 - Listar arquivos.");
		System.out.println("0 - Sair.\n");
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
			case 0:
				logout();
				break;
			default:
				System.out.println("Opção inválida.");
		}
	}

	// Lê um arquivo, transforma em chunks, envia aos peers e serializa
	// Hashtable de metadados após término das operações.
	// IDEA Paralelizar envio de chunks aos peers.
	// TODO Armazenar apenas nome do arquivo caso usuário passe um caminho.
	// Ex. /tmp/teste.txt (guardar apenas teste.txt)
	private boolean storeFile() throws IOException, ClassNotFoundException, NoSuchFileException, DirectoryNotEmptyException, InterruptedException {
		System.out.println("Insira o caminho para o arquivo.");
		String filename = scanner.nextLine();
		File file = new File(filename);
		if(!file.exists()) {
			System.out.println("Arquivo não existe.");
			return false;
		}
		Metadata tmp = fileHandler.readFileToBytes(filename);

		chunkServer.deliverChunksToPeers(tmp, peers); // Envia chunks aos peers
		fileHandler.serializeMetadata(tmp); // Serializa o arquivo .sdi p/ persistencia de metadados
		metadados.put(tmp.getFilename(), tmp); // Coloca metadado numa hashtable

		Files.delete(Paths.get(filename)); // Deleta arquivo original
		return true;

	}

	private boolean requestFile() throws FileNotFoundException, IOException, ClassNotFoundException {
		System.out.println("Insira o nome do arquivo a ser requisitado.");
		String filename = scanner.nextLine();
		if (!metadados.containsKey(filename)) return false;
		Metadata meta = metadados.get(filename);
		LinkedList<Data> dataList = new LinkedList<Data>();

		try{
			for (Data d : meta.getChunks()) {
				System.out.println(d.toString());
				Data tmp = chunkServer.requestChunk(d.getHashChunk(), d.getPeers());
				if (tmp != null)
					dataList.add(tmp); // Pode dar exception!
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		fileHandler.restoreFile(dataList, filename);

		// IDEA Pra cada chunk, seta de volta o byte[] (que foi deletado na
		// hora de enviar pros peers). Com todos os chunks reconstruímos o
		// arquivo e gg.
		// TODO Reconstruir arquivo a partir da lista de chunks

		return true;
	}

	// Faz requisição de um chunk
	// @params hash_chunk, peers (hash do chunk e lista de peers que possuem o chunk)


	// Lista os nomes de arquivos de um cliente.
	private void listFiles() {
		Set<String> keys = metadados.keySet();
		System.out.println();
        for(String key : keys){
            System.out.println(key);
        }
		System.out.println();
	}

	private void logout() {
		service.byePeer(addr);
		System.exit(0);
	}

	private Runnable getPeerList = new Runnable () {
		public void run() {
			try {
				while (true) {
					peers = service.getPeers(addr);
					//System.out.println(peers.toString());
					Thread.sleep(3000);
				}
			} catch (Exception e) {}
		}
	};

	public String getExtIp() throws Exception {
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

	public String getIp() throws Exception {
		String ret = null;
		try {
            InetAddress ipAddr = InetAddress.getLocalHost();
			ret = ipAddr.getHostAddress();
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
		return ret;
	}
}
