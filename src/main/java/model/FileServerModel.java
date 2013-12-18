package model;

import java.net.InetAddress;
import java.util.Date;
import java.util.Set;

public class FileServerModel {
	
	private InetAddress address;
	private int port;
	private long usage;
	private Date alive;
	private boolean online;
	private Set<String> fileList;
	
	public FileServerModel(InetAddress address, int port, long usage,
			Date alive, boolean online, Set<String> fileList) {
		super();
		this.setAddress(address);
		this.setPort(port);
		this.setUsage(usage);
		this.setAlive(alive);
		this.setOnline(online);
		this.setFileList(fileList);
	}

	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public long getUsage() {
		return usage;
	}

	public void setUsage(long usage) {
		this.usage = usage;
	}

	public Date getAlive() {
		return alive;
	}

	public void setAlive(Date alive) {
		this.alive = alive;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public Set<String> getFileList() {
		return fileList;
	}

	public void setFileList(Set<String> fileList) {
		this.fileList = fileList;
	}
}
