package org.objectweb.proactive.multiactivity;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.Service;
import org.objectweb.proactive.core.body.request.Request;

/**
 * This class extends the {@link Service} class and adds the capability of serving
 * more methods in parallel. 
 * <br>The decision of which methods can run in parallel is made
 * based on annotations set by the user. These annotations are to be found in the <i>
 * org.objectweb.proactive.annotation.multiactivity</i> package.
 * @author Zsolt Istv�n
 *
 */
public class MultiActiveService extends Service {
	
	public int numServes = 0;
	
	Logger logger = Logger.getLogger(this.getClass());
	
	private ExecutorService executorService;
	
	CompatibilityTracker compatibility;
	
	public MultiActiveService(Body body) {
		super(body);
		
		compatibility = new CompatibilityTracker(new AnnotationProcessor(body.getReifiedObject().getClass()));
		
		executorService = Executors.newCachedThreadPool();
	}
	
	/**
     * Invoke the default parallel policy to pick up the requests from the request queue.
     * This does not return until the body terminate, as the active thread enters in
     * an infinite loop for processing the request in the FIFO order, and parallelizing where 
     * possible.
     */
	public void multiActiveServing(){
		boolean success;
		while (body.isActive()) {
			// try to launch next request -- synchrnoized inside
			success = parallelServeOldest();
			
			//if we were not successful, let's wait until a new request arrives
			synchronized (requestQueue) {
				if (!success) {
					try {
						requestQueue.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * Scheduling based on a user-defined policy
	 * @param policy
	 */
	public void policyServing(ServingPolicy policy) {
		List<Request> chosen;
		int launched;;
		
		while (body.isActive()) {
			synchronized (requestQueue) {
				launched = 0;
				// get the list of requests to run -- as calculated by the
				// policy
				chosen = policy.runPolicy(compatibility);
				if (chosen!=null) {
					for (Request mf : chosen) {
						try {
							internalParallelServe(mf);
							requestQueue.getInternalQueue().remove(mf);
							launched++;
						} catch (ClassCastException e) {
							// somebody implemented the RequestWrapper and passed an
							// instance to us
							// this is not good...
						}
	
					}
				}
				// if we could not launch anything, that we wait until
				// either a running serve finishes or a new req. arrives
				if (launched == 0) {
					try {
						requestQueue.wait();
						// logger.info(this.body.getID()+" Greedy scheduler - wake up");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * This method will find the first method in the request queue that is compatible
	 * with the currently running methods. In case nothing is executing, it will take the 
	 * first, thus acting like the default strategy.
	 * @return
	 */
	public boolean parallelServeFirstCompatible(){
		synchronized (requestQueue) {
			List<Request> reqs = requestQueue.getInternalQueue();
			for (int i = 0; i < reqs.size(); i++) {
				if (compatibility.isCompatibleWithExecuting(reqs.get(i))) {
					internalParallelServe(reqs.remove(i));
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * This method will find the first method in the request queue that is compatible
	 * with the currently running methods. In case nothing is executing, it will take the 
	 * first, thus acting like the default strategy.
	 * @return
	 */
	public boolean parallelServeOldestOptimal(){
/*
		synchronized (requestQueue) {
			List<Request> reqs = requestQueue.getInternalQueue();
			if (reqs.size() == 0)
				return false;

			if (compatibilityMap.isCompatibleWithAllNames(reqs.get(0).getMethodName(), runningServes.keySet())) {
				internalParallelServe(reqs.remove(0));
				return true;
			} else if (reqs.size() > 1
					&& compatibilityMap.areCompatible(reqs.get(0), reqs.get(1))
					&& compatibilityMap.isCompatibleWithAllNames(reqs.get(1).getMethodName(), runningServes.keySet())) {
				internalParallelServe(reqs.remove(1));
				return true;
			}
		}*/
		return false;
	}
	
	/**
	 * This method will try to start the oldest waiting method in parallel
	 * with the currently running ones. The decision is made based on the 
	 * information extracted from annotations.
	 * @return whether the oldest request could be started or not
	 */
	public boolean parallelServeOldest(){
		RunnableRequest asserve = null;
		//synchronize both the queue and the running status to be safe from any angle
		synchronized (requestQueue) {

			Request r = requestQueue.removeOldest();
			if (r != null) {
				if (compatibility.isCompatibleWithExecuting(r)) {
					asserve = internalParallelServe(r);
				} else {
					// otherwise put it back
					requestQueue.addToFront(r);
				}
			}
		}
		return asserve != null;
	}
	
	public void startServe(Request r) {
		internalParallelServe(r);
	}

	protected RunnableRequest internalParallelServe(Request r) {
		
		RunnableRequest asserve;
		//if there is no conflict, prepare launch
		asserve = new RunnableRequest(r);

		if (asserve!=null) {
			//logger.info(this.body.getID()+" Parallel serving '"+asserve.r.getMethodName()+"'");
			//(new Thread(asserve, body.getID()+" -> "+r.getMethodName())).start();
			executorService.execute(asserve);
			
			compatibility.addRunning(r);
		}
		
		return asserve;
	}
	
	/**
	 * Method called from the {@link RunnableRequest} to signal the end of a serving.
	 * State is updated here, and also a new request will be attempted to be started.
	 * @param r
	 * @param asserve
	 */
	protected void asynchronousServeFinished(Request r) {
		synchronized (requestQueue) {
			compatibility.removeRunning(r);
			requestQueue.notifyAll();	
		}
	}

	/**
	 * By wrapping the request, we can pass the 'method' to the 
	 * outside world without actually exposing internal information. 
	 * @author Zsolt Istv�n
	 *
	 */
	protected class RunnableRequest implements Runnable {
		private final Request r;
		
		public RunnableRequest(Request r){
			this.r = r;
		}
		
		public Request getRequest(){
			return r;
		}
		
		@Override
		public void run() {
			body.serve(getRequest());
			asynchronousServeFinished(getRequest());
		}
		
	}
	
	protected class CompatibilityTracker extends SchedulerCompatibilityMap {
		private Map<MethodGroup, Integer> compats = new HashMap<MethodGroup, Integer>();
		private Map<String, List<Request>> runningMethods = new HashMap<String, List<Request>>();
		private int runningCount = 0;
		private int maxCount = 0;
		
		public CompatibilityTracker(AnnotationProcessor annotProc) {
			super(annotProc);

			for (MethodGroup groupName : groups.values()) {
				compats.put(groupName, 0);
			}
		}
		
		/*adds the request to the running set
		* adds one to the compatibility count of all groups this request's group is
		* compatible with
		* 
		* Time: O(g) -- g is the number of groups
		*/
		protected void addRunning(Request request) {
			String method = request.getMethodName();
			if (methods.containsKey(method)) {
				for (MethodGroup mg : methods.get(method).getCompatibleWith()) {
					compats.put(mg, compats.get(mg)+1);
				}
			}
			if (!runningMethods.containsKey(method)) {
				runningMethods.put(method, new LinkedList<Request>());
			}
			runningMethods.get(method).add(request);
			runningCount++;
		}
		
		/*removes the request from the running set
		* removes one from the compatibility count of all groups this request's group is
		* compatible with
		* 
		* Time: O(g) -- g is the number of groups
		*/
		protected void removeRunning(Request request) {
			String method = request.getMethodName();
			if (methods.containsKey(method)) {
				for (MethodGroup mg : methods.get(method).getCompatibleWith()) {
					compats.put(mg, compats.get(mg)-1);
				}
			}
			runningMethods.get(method).remove(request);
			runningCount--;
		}
		
		@Override
		public boolean isCompatibleWithExecuting(Request r) {
			return isCompatibleWithExecuting(r.getMethodName());
		}
		
		/*Alternative for the isCompatibleWithRequests in case we check with the running ones.
		* This will return the answer in O(1) time, as opposed to O(n) worst-case time of the other.
		* Thsi works by checking if the count of compatible methods is equal to the number 
		* of running methods.
		* The count is stored for each group and updated upon addition/removal of requests 
		* to/from the running set. 
		* 
		* Time: O(1)
		*/
		public boolean isCompatibleWithExecuting(String method) {
			if (runningCount==0) return true;
			if (runningCount>maxCount) {
				maxCount=runningCount;
			}

			MethodGroup mg = methods.get(method);
			return (mg!=null && compats.containsKey(mg)) && (compats.get(mg)==runningCount);
		}

		public Set<String> getExecutingMethodNameSet(){
			return runningMethods.keySet();
		}

		@Override
		public List<String> getExecutingMethodNames() {
			List<String> names = new LinkedList<String>();
			for (String m : runningMethods.keySet()) {
				for (int i=0; i<runningMethods.get(m).size(); i++) {
					names.add(m);
				}
			}
			
			return names;
		}

		@Override
		public List<Request> getExecutingRequests() {
			List<Request> reqs = new LinkedList<Request>();
			for (List<Request> lrr : runningMethods.values()) {
				reqs.addAll(lrr);
			}
			
			return reqs;
		}

		@Override
		public List<Request> getExecutingRequestsFor(String method) {
			return (runningMethods.containsKey(method)) ? runningMethods.get(method) : new LinkedList<Request>();
		}

		@Override
		public Request getOldestInTheQueue() {
			return requestQueue.getOldest();
		}

		@Override
		public List<Request> getQueueContents() {
			return requestQueue.getInternalQueue();
		}
		
		@Override
		public int getNumberOfExecutingRequests() {
			return runningCount;
		}
		
	}
	
}
