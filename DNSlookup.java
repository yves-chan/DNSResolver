
import java.net.InetAddress;

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
	static boolean tracingOn = false;
	static InetAddress rootNameServer;
	static InetAddress root;
	static String target;
	static boolean hasAnswer = false;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String fqdn;
		DNSResponse response = null; // Just to force compilation
		int argCount = args.length;
		
		if (argCount < 2 || argCount > 3) {
			usage();
			return;
		}

		rootNameServer = InetAddress.getByName(args[0]);
		fqdn = args[1];

		
		if (argCount == 3 && args[2].equals("-t"))
				tracingOn = true;


		root = rootNameServer;
		target = fqdn;
		while (!hasAnswer) {
			DNSQuery query = new DNSQuery(root, target);
			query.sendQuery();
			response = new DNSResponse(query);
			String test = response.getCNAME();
			//only want to look up CNAME when the answer ==1 and value is not IP address

			if(response.getAnswerCount()!=0) {
				if(response.getAnswerList()[0].getRecordType().equals(RRTypes.A)) {
					hasAnswer = true;
				} else if (response.getAnswerList()[0].getRecordType().equals(RRTypes.CNAME)){
					root = rootNameServer;
					target = response.getAnswerList()[0].getRecordValue();
				}
			} else if (response.getAdditionalCount()!=0) {
				root = response.reQuery();
			} else {

			}
			if (tracingOn) {
				response.dumpResponse();
			}
		}
		if (response != null && response.getAnswerCount()!= 0) {
			System.out.println(fqdn + " " + response.getAnswerList()[0].getTtl() + " " +
					response.getAnswerList()[0].getRecordValue());
		} else {
			System.out.println(fqdn + " -2 0.0.0.0");
		}





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


