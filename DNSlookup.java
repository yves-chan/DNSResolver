
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * 
 */

/**
 * @author Donald Acton
 * This example is adapted from Kurose & Ross
 *
 */
public class DNSlookup {


	static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
	static final int QUERY_HEADER_LENGTH = 12;
	static boolean tracingOn = false;
	static InetAddress rootNameServer;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String fqdn;
		DNSResponse response; // Just to force compilation
		int argCount = args.length;
		
		if (argCount < 2 || argCount > 3) {
			usage();
			return;
		}

		rootNameServer = InetAddress.getByName(args[0]);
		fqdn = args[1];

		
		if (argCount == 3 && args[2].equals("-t"))
				tracingOn = true;
		
		// Start adding code here to initiate the lookup

		// Use two bytes for qID
		Random random = new Random();
		int randInt = random.nextInt(65536);
		byte[] qID= new byte[]{(byte) (randInt&0xFF), (byte) ((randInt >> 8) &0xFF)};

		//Generate header
		byte[] header = new byte[QUERY_HEADER_LENGTH];
		System.arraycopy(qID, 0, header,0, qID.length);
		//set QDCOuNT to 1
		header[6] = (byte) 0x01;



	}

	private static void usage() {
		System.out.println("Usage: java -jar DNSlookup.jar rootDNS name [-t]");
		System.out.println("   where");
		System.out.println("       rootDNS - the IP address (in dotted form) of the root");
		System.out.println("                 DNS server you are to start your search at");
		System.out.println("       name    - fully qualified domain name to lookup");
		System.out.println("       -t      -trace the queries made and responses received");
	}
}


