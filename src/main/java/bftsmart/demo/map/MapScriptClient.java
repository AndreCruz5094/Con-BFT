package bftsmart.demo.map;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Throughput - Operations per second.
//Latency - Time between request and Answer.
public class MapScriptClient {
	
	public static ConcurrentLinkedQueue<Double> clientLatency = new ConcurrentLinkedQueue<>();
	public static AtomicInteger numTimeouts = new AtomicInteger(0);
	
	public static void main(String[] args) {
		if(args.length != 2) {
			System.out.println("Usage: MapClientScript <NumClients> <NumOperations>");
			System.exit(-1);
		}
		
		int numClients = Integer.parseInt(args[0]);
		int numOperations = Integer.parseInt(args[1]);
		List<ClientThread> clients = new ArrayList<>();
		for(int i = 0; i < numClients;i++) {
			clients.add(new ClientThread(new MapClient<String,String>(i+4),numOperations,i+4));
			System.out.println("Created Client " + (i + 4));
		}
		
		long startTime = System.nanoTime();
		//Starts the ClientThreads:
		for(ClientThread c : clients) {
			c.start();
		}
		
		//Awaits the end of the Threads:
		for(ClientThread c: clients) {
			try {
				c.join();
				System.out.println("Finished");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		long endTime = System.nanoTime();
		long result = endTime - startTime;
		System.out.println("Total Time: (seconds) " + (result / Math.pow(10, 9)));
		System.out.println("Time per Operation (Milliseconds) : " + (result/Math.pow(10,6))/numOperations);
		System.out.println("Number of Timeouts: " + numTimeouts.intValue());
		DoubleStream s = clientLatency.stream().mapToDouble(d->d);
		double latencyResult = (double)s.sum() / (double)clientLatency.size();
		System.out.println("Average Latency for the system (Milliseconds) : " + latencyResult);
		System.exit(0);
	}
	
	
	
	private static class ClientThread extends Thread{
		
		MapClient<String,String> map;
		private int numOps;
		private Logger log;
		private int clientId;
		private Random r;

		public ClientThread(MapClient<String,String> m,int numOps, int clientId) {
			map = m;
			this.numOps = numOps;
			this.clientId = clientId;
			log = LoggerFactory.getLogger(ClientThread.class.getName());
			r = new Random();
		}
		
//		@Override
//		public void run() {
//			
//			for(int i = 0; i < numOps;i++) {
//				byte[] key = new byte[10];
//				byte[] value = new byte[10];
//				int n = r.nextInt(5);
//				switch(n) {
//				case 0:
//					r.nextBytes(key);
//					r.nextBytes(value);
//					map.put(new String(key,StandardCharsets.UTF_8), new String(value,StandardCharsets.UTF_8));
//					break;
//				case 1:
//					r.nextBytes(key);
//					map.get(new String(key,StandardCharsets.UTF_8));
//					break;
//				case 2:
//					r.nextBytes(key);
//					map.remove(new String(key,StandardCharsets.UTF_8));
//					break;
//				case 3:
//					map.keySet();
//				}
//				
//				
//			}
//		}
		
		@Override
		public void run() {
			log.info("Started Client " + this.clientId);
			long total_latency = 0;
			for(int i = 0; i < numOps; i++) {
				byte[] key = new byte[10];
				byte[] value = new byte[10];
				r.nextBytes(key);
				r.nextBytes(value);
				long x = System.currentTimeMillis();
				String result = map.put(new String(key,StandardCharsets.UTF_8),new String(value,StandardCharsets.UTF_8));
				long y = System.currentTimeMillis();
				total_latency += y - x;
			}
			double latency_average = (double)total_latency / (double)numOps;
			log.info("Closing client " + this.clientId + ",Latency : " + total_latency);
			MapScriptClient.numTimeouts.set(MapScriptClient.numTimeouts.intValue() + map.getTimeouts());
			map.close();
			MapScriptClient.clientLatency.add(latency_average);
		}
	}
}
