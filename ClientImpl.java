import java.net.*;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.util.*;
import java.io.*;
import java.nio.file.*;

import broadcast.BroadcastServer;
import broadcast.Peer;

/*
 * IDEA Usar portas diferentes (pre-definidas) em diferentes ServerSockets
 * 		p/ diferentes tipos de requisições. Vamos usar TCP ou UDP?
 */

public class ClientImpl {

	private LinkedList<String> peers;
	private Hashtable<String,Metadata> metadados = new Hashtable<String, Metadata>();
	private String addr;
	private BroadcastServer service;
	private ServerSocket listener;
	private Scanner scanner = new Scanner(System.in);
	private FileHandler fileHandler = new FileHandler();

	public ClientImpl () throws IOException, ClassNotFoundException {

	}

	public void run() {
		try {
			if(fileHandler.verifyMetadata()) metadados = fileHandler.recoverMetadata();
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void menu () throws IOException, ClassNotFoundException {
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
	private boolean storeFile() throws IOException, ClassNotFoundException {
		System.out.println("Insira o caminho para o arquivo.");
		String filename = scanner.nextLine();
		File file = new File(filename);
		if(!file.exists()) {
			System.out.println("Arquivo não existe.");
			return false;
		}
		metadados.put(filename, fileHandler.readFileToBytes(filename));
		deliverChunksToPeers(filename);
		fileHandler.serializeMetadata(metadados);
										// TODO Enviar chunks aos peers.
										// IDEA Após enviar chunks, deletar os
		return true;					// vetores byte[] de cada chunk do
										// cliente de origem

		// TODO Armazenar apenas nome do arquivo caso usuário passe um caminho.
		// Ex. /tmp/teste.txt (guardar apenas teste.txt)
	}

	private boolean deliverChunksToPeers(String filename) {
		// TODO Método p/ enviar chunks aos peers (preferencialmente de forma
		// balanceada :>)
		return true;
	}

	private boolean requestFile() {
		System.out.println("Insira o nome do arquivo a ser requisitado.");
		String filename = scanner.nextLine();
		if (!metadados.containsKey(filename)) return false;
		Metadata meta = metadados.get(filename);
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
	private byte[] requestChunk(long hash_chunk, LinkedList<String> peers) {
		// TODO Método p/ pedir chunks dos peers.
		// IDEA Armazenar lista com todos os hash_chunks que o cliente possui,
		// Fazer request pelo hash e tals.
		return null;
	}

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
					System.out.println(peers.toString());
					Thread.sleep(30000);
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
