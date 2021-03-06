package driver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;

import optimizer.CostEstimator;

import output.kNNToken;

import data.*;

import index.*;
import exec.*;

public class kNNSelectQLevelOLD {

	private static String path = "//Users//ahmed//Desktop//kNNRelational Experimental Results//";			
	private static Tuple.Location qLocation;
	
	public static void main(String[] args) {

		Tuple t = new Tuple();
		t.location.xCoord = 35;
		t.location.yCoord = 17;
		qLocation = t.location;
		
		runAllK();
		//runAllPriceSelectivity();
		//runAllCountSelectivity();
	}
	
	private static void runAllSF() {

		int k = 16;

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "SF.csv"));
			//out.write("Selectivity, Locality Guided, Relational First\r\n");

			out.flush();

			for (int sf = 1; sf < 10; sf ++) {

				BPTree<Double, ArrayList<Tuple>> customerBPTree = new BPTree<Double, ArrayList<Tuple>>();
				ArrayList<Tuple> suppliers = new ArrayList<Tuple>();
				Rectangle R = new Rectangle(0, 0, 100, 100);
				QuadTree customerQTree = new QuadTree(R);
				HashMap<Integer, ArrayList<Order>> orders = new HashMap<Integer, ArrayList<Order>>();

				//readData(sf, customerQTree, customerBPTree);

				System.gc();

				System.out.println(sf + ", " + k + ",");
				out.write(sf + ", " + k + ",");
				//runJoin(out, k, suppliers, customerQTree, customerBPTree);
				out.flush();
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// customerQTree indexes the customers through a quad tree where the location is the key.
	// customerHashMap indexes the customers through a hash map where the customer id is the key.
	// orderBPTree indexes the orders through a B+Tree on the total price.
	// ordersHashMap indexes the orders through a hash map where the customer id is the key.
	private static void readData(int scaleFactor, QuadTree customerQTree, HashMap<Integer, Customer> customerHashMap,
			BPTree<Double, ArrayList<Order>> orderBPTree, HashMap<Integer, ArrayList<Order>> orderHashMap) {
		System.out.println("Reading Data");
		String path = "//Users//ahmed//Documents//MyWork//data//TPC-H//" + scaleFactor + "//";
		DataReader reader = new DataReader(path);

		reader.readOrdersIndexedByPriceAndCustID(orderBPTree, orderHashMap);
		reader.readCustomersIndexedByLocationAndCustID(customerQTree, customerHashMap);
		
		CostEstimator.estimatekNNCost(customerQTree);
		
	}

	private static void runAllK() {

		int scaleFactor = 1;

		Rectangle R = new Rectangle(0, 0, 100, 100);
		QuadTree customerQTree = new QuadTree(R);
		HashMap<Integer, Customer> customerHashMap = new HashMap<Integer, Customer>();
		
		BPTree<Double, ArrayList<Order>> orderBPTree = new BPTree<Double, ArrayList<Order>>();		
		HashMap<Integer, ArrayList<Order>> orderHashMap =  new HashMap<Integer, ArrayList<Order>>();

		readData(scaleFactor, customerQTree, customerHashMap, orderBPTree, orderHashMap);

		System.gc();

		System.out.println("Starting Execution");
		
		double priceThreshold = 1000;
		SortedMap<Double, ArrayList<Order>> sm = orderBPTree.subMap(orderBPTree.firstKey(), (double)priceThreshold);
		double priceSelectivity = 1 - ((double)sm.size() / orderBPTree.size());
		System.out.println("priceSelectivity = " + priceSelectivity);
		
		int countThreshold = 1;
		double countSelectivity = getCountSelectivity(countThreshold, orderHashMap, customerHashMap);
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "k.csv"));
			out.write("Scale factor, k, Price selectivity, Count selectivity, Relational First, kNN First\r\n");

			out.flush();

			for (int k = 2; k <= 1024; k*=2) {
			//for (int k = 2; k <= 131072; k*=2) {
				
				System.out.println(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity);
				System.out.println("priceThreshold = " + priceThreshold + ", countThreshold " + countThreshold);
				
				out.write(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity + ",");
				run(out, k, priceThreshold, countThreshold, qLocation, customerQTree, customerHashMap, orderBPTree, orderHashMap);
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void runAllPriceSelectivity() {

		int scaleFactor = 1;

		Rectangle R = new Rectangle(0, 0, 100, 100);
		QuadTree customerQTree = new QuadTree(R);
		HashMap<Integer, Customer> customerHashMap = new HashMap<Integer, Customer>();
		
		BPTree<Double, ArrayList<Order>> orderBPTree = new BPTree<Double, ArrayList<Order>>();		
		HashMap<Integer, ArrayList<Order>> orderHashMap =  new HashMap<Integer, ArrayList<Order>>();

		readData(scaleFactor, customerQTree, customerHashMap, orderBPTree, orderHashMap);

		System.gc();

		System.out.println("Starting Execution");
		
		int k = 131072;
		int countThreshold = 10;
		double countSelectivity = getCountSelectivity(countThreshold, orderHashMap, customerHashMap);
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "Price Selectivity.csv"));
			out.write("Scale factor, k, Price selectivity, Count selectivity, Relational First, kNN First\r\n");

			out.flush();

			for (int priceThreshold = 1000; priceThreshold <= 100000; priceThreshold+=5000) {
				SortedMap<Double, ArrayList<Order>> sm = orderBPTree.subMap(orderBPTree.firstKey(), (double)priceThreshold);
				double priceSelectivity = 1 - ((double)sm.size() / orderBPTree.size());
				
				System.out.println(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity);
				System.out.println("priceThreshold = " + priceThreshold + ", countThreshold " + countThreshold);
				
				out.write(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity + ",");
				run(out, k, priceThreshold, countThreshold, qLocation, customerQTree, customerHashMap, orderBPTree, orderHashMap);
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void runAllCountSelectivity() {

		int scaleFactor = 1;

		Rectangle R = new Rectangle(0, 0, 100, 100);
		QuadTree customerQTree = new QuadTree(R);
		HashMap<Integer, Customer> customerHashMap = new HashMap<Integer, Customer>();
		
		BPTree<Double, ArrayList<Order>> orderBPTree = new BPTree<Double, ArrayList<Order>>();		
		HashMap<Integer, ArrayList<Order>> orderHashMap =  new HashMap<Integer, ArrayList<Order>>();

		readData(scaleFactor, customerQTree, customerHashMap, orderBPTree, orderHashMap);

		System.gc();

		System.out.println("Starting Execution");
		
		//int k = 65536;
		int k = 128;
		int priceThreshold = 100000;
		SortedMap<Double, ArrayList<Order>> sm = orderBPTree.subMap(orderBPTree.firstKey(), (double)priceThreshold);
		double priceSelectivity = 1 - ((double)sm.size() / orderBPTree.size());
		System.out.println("priceSelectivity = " + priceSelectivity);
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "Count Selectivity.csv"));
			out.write("Scale factor, k, Price selectivity, Count selectivity, Relational First, kNN First\r\n");

			out.flush();

			for (int countThreshold = 0; countThreshold <= 40; countThreshold+=1) {
				double countSelectivity = getCountSelectivity(countThreshold, orderHashMap, customerHashMap);
								
				System.out.println(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity);
				System.out.println("priceThreshold = " + priceThreshold + ", countThreshold " + countThreshold);
				
				out.write(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity + ",");
				run(out, k, priceThreshold, countThreshold, qLocation, customerQTree, customerHashMap, orderBPTree, orderHashMap);
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void run(BufferedWriter out, int k, double priceThreshold, int countThreshold, Tuple.Location qLocation, QuadTree customerQTree, HashMap<Integer, Customer> customerHashMap,
			BPTree<Double, ArrayList<Order>> orderBPTree, HashMap<Integer, ArrayList<Order>> orderHashMap) throws IOException{

		QLevelSelect qLevel = new QLevelSelect();
		
		System.out.println("relational first");
		long startTime = System.nanoTime();
		kNNToken token = qLevel.relationalFirst(k, priceThreshold, countThreshold, qLocation, orderBPTree, customerHashMap, customerQTree);
		long endTime = System.nanoTime();
		
		while (token.size() > 0) {
			System.out.print(token.remove().distance + " - ");
		}
		System.out.println();
				
		double execTime = (endTime - startTime) / 1000000000.0;
		out.write(execTime + ", ");
		System.out.println("Execution Time " + execTime);
				
		
		System.out.println();
		System.out.println("kNN first");		
		startTime = System.nanoTime();
		token = qLevel.kNNFirst(k, priceThreshold, countThreshold, qLocation, customerQTree, orderHashMap);
		endTime = System.nanoTime();
		
		while (token.size() > 0) {
			System.out.print(token.remove().distance + " - ");
		}
		System.out.println();
				
		execTime = (endTime - startTime) / 1000000000.0;
		out.write(execTime + ", ");
		System.out.println("Execution Time " + execTime);
		
		System.out.println();
		out.write("\r\n");
		out.flush();
		
	}


	private static double getCountSelectivity(int countThreshold, HashMap<Integer, ArrayList<Order>> orderHashMap, HashMap<Integer, Customer> customerHashMap) {
		int count = 0;
		for (ArrayList<Order> list : orderHashMap.values()) {
			if (list.size() > countThreshold)
				count++;
		}
		
		return 1 - ((double)count / customerHashMap.size());
	}

}