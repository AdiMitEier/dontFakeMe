package model;

import java.io.Serializable;

//STAGE 1
public class FileModel implements Serializable {
	private static final long serialVersionUID = 8140560012551898065L;
	
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
	
	@Override
	public String toString(){
		return this.filename;
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof FileModel)){
			return false;
		}
		FileModel fm = (FileModel)o;
		return fm.getFilename().equals(this.getFilename());
	}
}
