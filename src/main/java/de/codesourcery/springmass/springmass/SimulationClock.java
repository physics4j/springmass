package de.codesourcery.springmass.springmass;

import java.util.concurrent.CountDownLatch;

public abstract class SimulationClock extends Thread 
{
	private final Object LOCK = new Object();
	private final SimulationParameters parameters;

	private boolean runSimulation = false;
	private volatile boolean terminate;
	private final CountDownLatch threadTermination = new CountDownLatch(1);

	public SimulationClock(SimulationParameters parameters) {
		setDaemon(true);
		this.parameters = parameters;
	}

	public void stopClock() 
	{
		synchronized( LOCK ) 
		{
			if ( runSimulation ) 
			{
				System.out.println("Stopping simlation");
				runSimulation = false;
			} 
		}
	}

	public void destroy() 
	{
		terminate = true;
		synchronized( LOCK ) 
		{
			LOCK.notifyAll();
		}
		try 
		{
			threadTermination.await();
		} 
		catch (InterruptedException e) 
		{
			Thread.currentThread().interrupt();
		}
	}

	public void startClock() 
	{
		synchronized( LOCK ) 
		{
			if ( ! runSimulation ) 
			{
				System.out.println("Starting simlation");
				runSimulation = true;
				LOCK.notifyAll();
			}
		}
	}	

	public boolean isClockRunning() {
		synchronized( LOCK ) 
		{
			return runSimulation;
		}
	}

	@Override
	public void run() 
	{
		try {
			while(true) 
			{
				synchronized (LOCK) 
				{
					while( ! runSimulation && ! terminate) 
					{
						try {
							System.out.println("Simulation thread sleeping");
							LOCK.wait();
							System.out.println("Simulation thread woke up");
						} catch (InterruptedException e) {
						}
					}
				}
				
				if ( terminate ) {
					break;
				}

				tick();

				try {
					Thread.sleep(parameters.getFrameSleepTime());
				} 
				catch (InterruptedException e) {
				}
			}
		} finally {
			threadTermination.countDown();
		}
	}

	protected abstract void tick();
}