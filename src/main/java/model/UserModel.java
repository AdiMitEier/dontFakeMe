package model;

import java.util.HashMap;
import java.util.Map;

import client.IClientRMI;

public class UserModel {

	private String name;
	private String password;
	private boolean online;
	private long credits;
	private IClientRMI clientObject;
	// a subscription is saved in the format FILENAME,{#downloads till notification, #downloads before subscription}
	private Map<String,Integer[]> subscriptions;
	
	public UserModel(String name, String password, boolean online, long credits) {
		this.setName(name);
		this.setPassword(password);
		this.setOnline(online);
		this.setCredits(credits);
		subscriptions = new HashMap<String,Integer[]>();
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

	public IClientRMI getClientObject() {
		return clientObject;
	}

	public void setClientObject(IClientRMI clientObject) {
		this.clientObject = clientObject;
	}

	public Map<String,Integer[]> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(Map<String,Integer[]> subscriptions) {
		this.subscriptions = subscriptions;
	}
}
