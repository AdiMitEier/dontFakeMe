package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import client.IClientRMI;

public class UserModel {

	private String name;
	private String password;
	private boolean online;
	private long credits;
	private IClientRMI clientObject;
	private Set<String> subscriptions;
	
	public UserModel(String name, String password, boolean online, long credits) {
		this.setName(name);
		this.setPassword(password);
		this.setOnline(online);
		this.setCredits(credits);
		subscriptions = new HashSet<String>();
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

	public Set<String> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(Set<String> subscriptions) {
		this.subscriptions = subscriptions;
	}
}
