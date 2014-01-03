package proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;

import client.IClientRMI;
import message.response.MessageResponse;
import message.response.TopThreeDownloadsResponse;

public interface IProxyRMI extends Remote {
	MessageResponse readQuorum() throws RemoteException;
	MessageResponse writeQuorum() throws RemoteException;
	TopThreeDownloadsResponse topThreeDownloads() throws RemoteException;
	MessageResponse subscribe(IClientRMI client, String userName, String file) throws RemoteException;
	MessageResponse getProxyPublicKey() throws RemoteException;
	MessageResponse setUserPublicKey() throws RemoteException;
}
