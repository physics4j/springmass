package de.codesourcery.springmass.springmass;

public abstract class Simulator {

	private final SpringMassSystem system;
	private final SimulationClock simulationClock;
	private final SimulationParameters parameters;
	
	public Simulator(SimulationParameters parameters) 
	{
		this.parameters = parameters;
		this.system = new SpringMassSystemFactory().create(parameters);

		this.simulationClock = new SimulationClock(parameters) {

			private long tickCounter = 0;
			
			@Override
			protected void tick() 
			{
				long stepTime = -System.currentTimeMillis();
				system.step();
				stepTime += System.currentTimeMillis();
				
				long renderTime = -System.currentTimeMillis();
				afterTick();
				renderTime += System.currentTimeMillis();
				
				tickCounter++;
				if ( (tickCounter%30) == 0 ) {
					System.out.println("Simulation time: "+stepTime+" ms / rendering time: "+renderTime+" ms / total: "+(stepTime+renderTime)+" ms");
				}
			}
			
		};
		simulationClock.start();		
	}
	
	protected abstract void afterTick();
	
	public void start() 
	{
		simulationClock.startClock();
	}
	
	public boolean isRunning() {
		return simulationClock.isClockRunning();
	}
	
	public void destroy() 
	{
		simulationClock.destroy();
	}
	
	public void stop() {
		simulationClock.stopClock();
	}	
	
	public SpringMassSystem getSpringMassSystem() {
		return system;
	}
	
	public SimulationParameters getSimulationParameters() {
		return parameters;
	}
}
