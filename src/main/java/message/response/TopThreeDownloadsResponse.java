package message.response;

import message.Response;

public class TopThreeDownloadsResponse implements Response {
	private static final long serialVersionUID = 4550680230065708876L;

	private String file1 = "";
	private String file2 = "";
	private String file3 = "";
	private int downloads1 = 0;
	private int downloads2 = 0;
	private int downloads3 = 0;

	public TopThreeDownloadsResponse() {
	}

	public String getFile1() {
		return file1;
	}

	public void setFile1(String file1) {
		this.file1 = file1;
	}

	public String getFile2() {
		return file2;
	}

	public void setFile2(String file2) {
		this.file2 = file2;
	}

	public String getFile3() {
		return file3;
	}

	public void setFile3(String file3) {
		this.file3 = file3;
	}

	public int getDownloads1() {
		return downloads1;
	}

	public void setDownloads1(int downloads1) {
		this.downloads1 = downloads1;
	}

	public int getDownloads2() {
		return downloads2;
	}

	public void setDownloads2(int downloads2) {
		this.downloads2 = downloads2;
	}

	public int getDownloads3() {
		return downloads3;
	}

	public void setDownloads3(int downloads3) {
		this.downloads3 = downloads3;
	}
	
	@Override
	public String toString() {
		return String.format("Top Three Downloads:\n1. %s %s\n2. %s %s\n3. %s %s",file1,file1.equals("") ? "":downloads1,file2,file2.equals("") ? "":downloads2,file3,file3.equals("") ? "":downloads3);
	}
}
