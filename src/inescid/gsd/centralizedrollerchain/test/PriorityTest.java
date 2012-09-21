package inescid.gsd.centralizedrollerchain.test;

import inescid.gsd.centralizedrollerchain.utils.PriorityPair;

import java.util.concurrent.PriorityBlockingQueue;

public class PriorityTest {

	public static void main(String[] args) {
		PriorityBlockingQueue<PriorityPair<Integer, Integer>> queue = new PriorityBlockingQueue<PriorityPair<Integer, Integer>>();

		queue.add(new PriorityPair<Integer, Integer>(1, 4, 0));
		queue.add(new PriorityPair<Integer, Integer>(2, 4, 0));
		queue.add(new PriorityPair<Integer, Integer>(3, 4, 1));
		queue.add(new PriorityPair<Integer, Integer>(4, 4, 0));
		queue.add(new PriorityPair<Integer, Integer>(5, 4, 0));
		queue.add(new PriorityPair<Integer, Integer>(6, 4, 2));
		queue.add(new PriorityPair<Integer, Integer>(7, 4, 0));
		queue.add(new PriorityPair<Integer, Integer>(8, 4, 0));

		while (queue.size() > 0)
			try {
				System.out.println(queue.take());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
}
