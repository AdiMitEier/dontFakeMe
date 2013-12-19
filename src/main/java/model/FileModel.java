package model;

//STAGE 1
public class FileModel {
	private String filename;
	private int version;
	
	public FileModel(String filename, int version){
		this.filename = filename;
		this.version = version;
	}

	public String getFilename() {
		return filename;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
	
	public String toString(){
		return this.filename;
	}
}
