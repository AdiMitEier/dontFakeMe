package model;

public class FileModel {
	private String filename;
	private int version;
	
	public FileModel(String filename, int version){
		this.setFilename(filename);
		this.setVersion(version);
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
}
