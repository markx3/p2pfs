import java.util.*;
import java.io.Serializable;
import java.io.*;
import java.nio.file.*;


import broadcast.Peer;

public class Data implements Serializable {
	private LinkedList<String> peers = new LinkedList<>();
	private byte[] data = new byte[64*1024];
	private long hash_chunk;

	public Data(byte[] d) {
		this.data = Arrays.copyOf(d, d.length);
		this.hash_chunk = this.data.hashCode();
	}

	public Data(byte[] d, LinkedList<String> peers) {
		this.peers = peers;
		this.data = Arrays.copyOf(d, d.length);
		this.hash_chunk = this.data.hashCode();
	}

	public void setData(byte[] data) throws NullPointerException {
		this.data = data;
	}

	public void freeData() {
		data = null;
		System.gc(); // Chama o garbage collector
	}

	public void addPeer(String s) {
		peers.add(s);
	}

	public byte[] getData() {
		return this.data;
	}

	public LinkedList<String> getPeers() {
		return this.peers;
	}

	public long getHashChunk() {
		return this.hash_chunk;
	}

	public void setHashChunk(long hash_chunk) {
		this.hash_chunk = hash_chunk;
	}

	@Override
	public String toString() {
		return data.toString();
	}
}
