import java.util.ArrayList;

public class AutonomousSystem {
	static final int HOP_NUMBER_INFINITY = 16;
	static final int TIME_SPEEDUP = 120;
	
	private ArrayList<RipRouter> routers;
	private ArrayList<String> networks;
	private boolean instable;
	
	AutonomousSystem() {
		routers = new ArrayList<RipRouter>();
		networks = new ArrayList<String>();
		instable = false;
	}
	
	void configure() {
		// Add 7 networks
		for (int i = 1; i <= 7; i++) {
			String net = "N" + i;
			networks.add(net);
		}
		
		// Add 6 routers
		for (int i = 1; i <= 6; i++) {
			RipRouter router = new RipRouter("R" + i);
			String connectedNet1 = "N" + i;
			String connectedNet2 = "N" + (i+1);
			
			router.addConnectedNet(connectedNet1);
			router.addConnectedNet(connectedNet2);
			
			// Every router's routing table initially contains only its connected nets
			for (int j = 0; j < networks.size(); j++) {
				String net = networks.get(j);
				if (net.equals(connectedNet1) || net.equals(connectedNet2)) {
					TableEntry entry = new TableEntry();
					entry.destinationNet = net;
					entry.nextRouter = null;
					entry.hopNumber = 1;
					router.addTableEntry(entry);
				}
			}
			
			routers.add(router);
		}
		
		// Configure neighboring relationship of the routers
		for (int i = 0; i < routers.size(); i++) {
			RipRouter router = routers.get(i);
			if (i-1 >= 0) {
				router.addNeighbour(routers.get(i-1));
			}
			if (i+1 < routers.size()) {
				router.addNeighbour(routers.get(i+1));
			}
		}
	}
	
	void printAllRoutingTables() {
		for (int i = 0; i < routers.size(); i++) {
			RipRouter router = routers.get(i);
			System.out.println("Routing table of router " + router.getName() + ":");
			router.printRoutingTable();
			System.out.println("");
		}
	}
	
	void simulateInstability() {
		instable = true;
		simulate();
	}
	
	void simulate() {
		int round = 1;
		while (true) {
			System.out.println("## At beginning of Round " + round + "");
			printAllRoutingTables();
			
			System.out.println("Performing operations of Round " + round + "...");
			boolean converged = true;
			// All routers operate in order
			for (int i = 0; i < routers.size(); i++) {
				RipRouter router = routers.get(i);
				// In real RIP, a router operates once every 30 seconds
				// In the simulation, we speed it up to see results quickly
				try {
					Thread.sleep(30000 / TIME_SPEEDUP);
				} catch (InterruptedException e) {
				    e.printStackTrace();
				}
				ArrayList<RipRouter> neighbours = router.getNeighbours();
				boolean influenced = false;
				// This router sends its routing table to all its neighbours; we see if this has any influence.
				for (int j = 0; j < neighbours.size(); j++) {
					RipRouter neighbour = neighbours.get(j);
					boolean updated = neighbour.updateRoutingTable(router);
					if (updated) {
						influenced = true;
					}
				}
				
				// If this router's advertising has influence, the system is not converged.
				if (influenced) {
					converged = false;
				}
				
				// For the instable simulation, N1 is disconnected after R1's operation in round 1
				if (instable) {
					if (round == 1 && router.getName().equals("R1")) {
						ArrayList<TableEntry> table = router.getRoutingTable();
						for (int k = 0; k < table.size(); k++) {
							TableEntry entry = table.get(k);
							if (entry.destinationNet.equals("N1")) {
								entry.nextRouter = null;
								entry.hopNumber = HOP_NUMBER_INFINITY;
								
								// We are sure that only exists one entry with destination N1, so break here
								break;
							}
						}
					}
				}
				
			}

			System.out.println("Done operations of Round " + round + ".");
			System.out.println("");
			
			if (converged) {
				System.out.println("The system has converged.");
				break;
			}
			
			round++;
		}
	}
}

class RipRouter {
	// Connected nets have little use in this simulation, but are meaningful for a router
	private ArrayList<String> connectedNets;
	private ArrayList<TableEntry> routingTable;
	private ArrayList<RipRouter> neighbours;
	private String name;
	
	RipRouter(String routerName) {
		name = routerName;
		connectedNets = new ArrayList<String>();
		routingTable = new ArrayList<TableEntry>();
		neighbours = new ArrayList<RipRouter>();
	}
	
	public void addConnectedNet(String net) {
		connectedNets.add(net);
	}
	
	public void addTableEntry(TableEntry entry) {
		
		int i;
		// Find the appropriate position to insert so entries are sorted by destination nets
		// This is only for the convenience of human reading
		for (i = 0; i < routingTable.size(); i++) {
			TableEntry e = routingTable.get(i);
			if (e.destinationNet.compareTo(entry.destinationNet) > 0) {
				break;
			}
		}
		routingTable.add(i, entry);
	}
	
	public void addNeighbour(RipRouter router) {
		neighbours.add(router);
	}
	
	public boolean updateRoutingTable(RipRouter router) {
		ArrayList<TableEntry> receivedTable = router.getRoutingTable();
		boolean updated = false;
		
		// For every entry in the received table
		for (int i = 0; i < receivedTable.size(); i++) {
			TableEntry received = receivedTable.get(i);
			boolean existed = false;
			// Check the current routing table
			for (int j = 0; j < routingTable.size(); j++) {
				TableEntry current = routingTable.get(j);
				if (current.destinationNet.equals(received.destinationNet)) {
					// The received destination already exists, try to update it
					existed = true;
					if (current.nextRouter != null && current.nextRouter == router) {
						// If they have the same nextRouter, prefer the received one unless their hop numbers are equal
						if (current.hopNumber != received.hopNumber + 1) {
							current.hopNumber = received.hopNumber + 1;
							updated = true;
						}
					} else if (current.hopNumber > received.hopNumber + 1) {
						// The received one has smaller hop number, 
						current.hopNumber = received.hopNumber + 1;
						current.nextRouter = router;
						updated = true;
					}
					
					// Make sure the hop number does not exceed the infinity value 
					if (current.hopNumber > AutonomousSystem.HOP_NUMBER_INFINITY) {
						current.hopNumber = AutonomousSystem.HOP_NUMBER_INFINITY;
					}
				}
			}
			
			if (!existed) {
				// The received destination is not in current routing table, add it
				TableEntry entry = new TableEntry();
				entry.destinationNet = received.destinationNet;
				entry.nextRouter = router;
				entry.hopNumber = received.hopNumber + 1;
				addTableEntry(entry);
				updated = true;
			}
		}
		
		return updated;
	}
			
	public void printRoutingTable() {
		System.out.println("Destination Network | Next Router | Number of Hops to Destination");
		for (int i = 0; i < routingTable.size(); i++) {
			TableEntry entry = routingTable.get(i);
			entry.print();
		}
	}
	
	public String getName() {
		return name;
	}
	
	public ArrayList<RipRouter> getNeighbours() {
		return neighbours;
	}
	
	public ArrayList<TableEntry> getRoutingTable() {
		return routingTable;
	}
	
}

class TableEntry {
	public String destinationNet;
	public RipRouter nextRouter;
	public int hopNumber;
	
	public void print() {
		
		String routerName;
		if (nextRouter != null) {
			routerName = nextRouter.getName();
		} else {
			// Next router is not applied (NA), i.e., the destination is only 1 hop away
			routerName = "--";
		}
		String hops = "" + hopNumber;
		if (hopNumber == AutonomousSystem.HOP_NUMBER_INFINITY) {
			// More readable for the infinity hop number
			hops = "INF";
		}
		System.out.println(destinationNet + " | " + routerName  + " | " + hops);
	}
}
