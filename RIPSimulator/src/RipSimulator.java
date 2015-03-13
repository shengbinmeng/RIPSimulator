public class RipSimulator {	
	public static void main(String[] args) {
		System.out.println("Simulator started.");
		AutonomousSystem as = new AutonomousSystem();
		as.configure();
		as.simulate(true);
		System.out.println("End of simulation.");
	}
}