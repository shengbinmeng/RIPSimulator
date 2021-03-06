public class RipSimulator {	
	public static void main(String[] args) {
		System.out.println("Simulation started.");
		AutonomousSystem as = new AutonomousSystem();
		as.configure();
		// Simulate the operations of RIP on every router
		as.simulate();
		System.out.println("End of simulation.");
	}
}