package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClientRMI extends Remote {
	void notifySubscription(String file) throws RemoteException;
}
