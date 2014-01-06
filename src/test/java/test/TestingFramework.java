package test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import proxy.IProxyCli;
import server.IFileServerCli;
import util.ComponentFactory;
import util.Config;
import util.Util;
import cli.Shell;
import cli.TestInputStream;
import cli.TestOutputStream;
import client.IClientCli;

public class TestingFramework {
	static ComponentFactory componentFactory = new ComponentFactory();
	IProxyCli proxy;
	IFileServerCli server;
	IClientCli client1;
	IClientCli client2;
	Config config;
	int clients;
	int uploadsPerMin;
	int downloadsPerMin;
	int fileSizeKB;
	double overwriteRatio;
	Timer testTimer;
	Timer downloadTimer;
	
	public static void main(String[] args) {
		try {
			new TestingFramework();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public TestingFramework() throws Exception {
		readConfig();
		proxy = componentFactory.startProxy(new Config("proxy"), new Shell("proxy", new TestOutputStream(new FileOutputStream("lol.txt")), new TestInputStream()));
		Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);
		
		server = componentFactory.startFileServer(new Config("fs1"), new Shell("fs1", new TestOutputStream(new FileOutputStream("lol.txt")), new TestInputStream()));
		Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);
		
		client1 = componentFactory.startClient(new Config("client"), new Shell("client", new TestOutputStream(System.out), new TestInputStream()));
		Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);
		
		client2 = componentFactory.startClient(new Config("client"), new Shell("client", new TestOutputStream(System.out), new TestInputStream()));
		Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);
		
		client1.login("alice", "12345");
		client1.buy(1000);
		client2.login("bill", "23456");
		client2.buy(1000);
		testTimer = new Timer();
		testTimer.schedule(new EndTestTask(), (long)15000, (long)15000);
		downloadTimer = new Timer();
		downloadTimer.schedule(new DownloadTask(client1), 0l, (long)60000/downloadsPerMin);
		downloadTimer.schedule(new UploadTask(client1), 0l, (long)60000/uploadsPerMin);
		downloadTimer.schedule(new UploadTask(client2), 0l, (long)60000/uploadsPerMin);
	}
	
	private void readConfig() {
		config = new Config("loadtest");
		clients = config.getInt("clients");
		uploadsPerMin = config.getInt("uploadsPerMin");
		downloadsPerMin = config.getInt("downloadsPerMin");
		fileSizeKB = config.getInt("fileSizeKB");
		overwriteRatio = Double.parseDouble(config.getString("overwriteRatio"));
		System.out.println("Clients: " + clients);
		System.out.println("UploadsPerMin: " + uploadsPerMin);
		System.out.println("DownloadsPerMin: " + downloadsPerMin);
		System.out.println("FileSizeKB: " + fileSizeKB);
		System.out.println("OverwriteRatio: " + overwriteRatio);
	}
	
	private class DownloadTask extends TimerTask {
		
		IClientCli client;
		
		DownloadTask(IClientCli client) {
			this.client = client;
		}
		
		public void run() {
			try {
				System.out.println(client.getLoggedInUserName()+": "+client.download("short.txt").toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
private class UploadTask extends TimerTask {
		
		IClientCli client;
		
		UploadTask(IClientCli client) {
			this.client = client;
		}
		
		public void run() {
			try {
				System.out.println(client.getLoggedInUserName()+": "+client.upload("short.txt").toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private class EndTestTask extends TimerTask {
		public void run() {
			testTimer.cancel();
			downloadTimer.cancel();
			try {
				proxy.exit();
				server.exit();
				client1.exit();
				client2.exit();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
