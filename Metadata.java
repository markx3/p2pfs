import java.util.*;
import java.lang.*;
import java.io.Serializable;

class Metadata implements Serializable {
	private String filename;
	private long filesize;
	private LinkedList<Data> chunks;

	public Metadata(String name, long size) {
		this.filename = name;
		this.filesize = size;
		chunks = new LinkedList<>();
	}

	public String getFilename() {
		return this.filename;
	}

	public long getFilesize() {
		return this.filesize;
	}

	public LinkedList<Data> getChunks() {
		return this.chunks;
	}

	public void setFilename(String name) {
		this.filename = name;
	}

	public void setFilesize(long size) {
		this.filesize = size;
	}

	public void addChunk(Data data) {
		chunks.add(data);
	}


}
