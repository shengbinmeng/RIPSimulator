public class RipSimulatorInstable {	
	public static void main(String[] args) {
		System.out.println("Simulation started.");
		AutonomousSystem as = new AutonomousSystem();
		as.configure();
		// Simulate the instability of RIP, which means the system will never converge
		as.simulateInstability();
		System.out.println("End of simulation.");
	}
}