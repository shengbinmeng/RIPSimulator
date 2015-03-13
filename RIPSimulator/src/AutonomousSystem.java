import java.util.ArrayList;

public class AutonomousSystem {
	static final int HOP_NUMBER_INFINITY = 16;
	
	private ArrayList<RipRouter> routers;
	private ArrayList<String> networks;
	private boolean instability;
	
	AutonomousSystem() {
		routers = new ArrayList<RipRouter>();
		networks = new ArrayList<String>();
		instability = false;
	}
	
	void configure() {
		for (int i = 1; i <= 7; i++) {
			String net = "N" + i;
			networks.add(net);
		}
		for (int i = 1; i <= 6; i++) {
			RipRouter router = new RipRouter("R"+i);
			String connectedNet1 = "N"+ i;
			String connectedNet2 = "N"+ (i+1);
			
			router.addConnectedNet(connectedNet1);
			router.addConnectedNet(connectedNet2);
			
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
		
		// Configure neighbors
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
		instability = true;
		simulate();
	}
	
	void simulate() {
		int round = 1;
		while (true) {
			System.out.println("## At beginning of Round " + round + "");
			printAllRoutingTables();
			
			System.out.println("Performing operations of Round " + round + "...");
			boolean converged = true;
			for (int i = 0; i < routers.size(); i++) {
				RipRouter router = routers.get(i);
				ArrayList<RipRouter> neighbours = router.getNeighbours();
				boolean influenced = false;
				for (int j = 0; j < neighbours.size(); j++) {
					RipRouter neighbour = neighbours.get(j);
					boolean updated = neighbour.updateRoutingTable(router);
					if (updated) {
						influenced = true;
					}
				}
				if (influenced) {
					converged = false;
				}
				
				if (instability) {
					if (round == 1 && router.getName().equals("R1")) {
						ArrayList<TableEntry> table = router.getRoutingTable();
						for (int k = 0; k < table.size(); k++) {
							TableEntry entry = table.get(k);
							if (entry.destinationNet.equals("N1")) {
								entry.nextRouter = null;
								entry.hopNumber = HOP_NUMBER_INFINITY;
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
		for (int i = 0; i < receivedTable.size(); i++) {
			TableEntry received = receivedTable.get(i);
			boolean existed = false;
			for (int j = 0; j < routingTable.size(); j++) {
				TableEntry current = routingTable.get(j);
				if (current.destinationNet.equals(received.destinationNet)) {
					// The destination already exist, update it
					existed = true;
					if (current.nextRouter == router) {
						// They have the same nextRouter
						if (current.hopNumber != received.hopNumber + 1) {
							current.hopNumber = received.hopNumber + 1;
							updated = true;
						}
					} else if (current.hopNumber > received.hopNumber + 1) {
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
				// The received destination is not in my routing table, add it
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
		String routerName = "--";
		if (nextRouter != null) {
			routerName = nextRouter.getName();
		}
		String hops = "" + hopNumber;
		if (hopNumber == AutonomousSystem.HOP_NUMBER_INFINITY) {
			hops = "INF";
		}
		System.out.println(destinationNet + " | " + routerName  + " | " + hops);
	}
}
