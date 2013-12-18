package model;

public class UserModel {

	private String name;
	private String password;
	private boolean online;
	private long credits;
	
	public UserModel(String name, String password, boolean online, long credits) {
		this.setName(name);
		this.setPassword(password);
		this.setOnline(online);
		this.setCredits(credits);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public long getCredits() {
		return credits;
	}

	public void setCredits(long credits) {
		this.credits = credits;
	}
}
