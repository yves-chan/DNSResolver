
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

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
		int rcodeError = DNSRCodes.NO_ERROR_CODE;
		Set<String> cNamesSeen = new HashSet<String>();

		while (!hasAnswer && rcodeError==DNSRCodes.NO_ERROR_CODE && !target.equals("----")) {
			DNSQuery query = new DNSQuery(root, target);
			query.sendQuery();
			response = new DNSResponse(query);
			rcodeError = response.getReplyCode();

			if(response.getAnswerCount()!=0) {
				//If not resolving a name server
				if(response.getAnswerList()[0].getRecordType().equals(DNSRTypes.A_DESCRIPTION)
						&& !response.getAnswerList()[0].getName().substring(0,2).equals("ns")) {
					hasAnswer = true;
				} else if (response.getAnswerList()[0].getRecordType().equals(DNSRTypes.CNAME_DESCRIPTION)){
					root = rootNameServer;
					target = response.getAnswerList()[0].getRecordValue();
					for (DNSResponse.ResponseRecord r: response.getAnswerList()) {
						if (r.getRecordValue().equals(fqdn)) {
							//loop detected
							hasAnswer=true;
						}
					}
				} else {
					root = response.reQuery();
					target = fqdn;
				}
			} else if (response.getAdditionalCount()!=0) {
				root = response.reQuery();
			} else if (response.getNsCount() != 0) {
				target = response.getNsList()[0].getRecordValue();
				root = rootNameServer;
			} else {
				break;
			}
			if (tracingOn) {
				response.dumpResponse();
			}
		}

		if (hasAnswer) {
			System.out.println(fqdn + " " + response.getAnswerList()[0].getTtl() + " " +
					response.getAnswerList()[0].getRecordValue());
		} else {
			switch (rcodeError) {
				case DNSRCodes.FORMAT_ERROR_CODE:
					System.out.println(fqdn + " -4 0.0.0.0");
					System.out.println("Rcode Error: " + DNSRCodes.FORMAT_ERROR_DESCRIPTION);
					break;
				case DNSRCodes.SERVER_FAIL_CODE:
					System.out.println(fqdn + " -4 0.0.0.0");
					System.out.println("Rcode Error: " + DNSRCodes.SERVER_FAIL_DESCRIPTION);
					break;
				case DNSRCodes.NX_DOMAIN_CODE:
					System.out.println(fqdn + " -1 0.0.0.0");
					System.out.println("Rcode Error: " + DNSRCodes.NX_DOMAIN_DESCRIPTION);
					break;
				case DNSRCodes.NOT_IMP_CODE:
					System.out.println(fqdn + " -4 0.0.0.0");
					System.out.println("Rcode Error: " + DNSRCodes.NOT_IMP_DESCRIPTION);
					break;
				case DNSRCodes.REFUSED_CODE:
					System.out.println(fqdn + " -4 0.0.0.0");
					System.out.println("Rcode Error: " + DNSRCodes.REFUSED_DESCRIPTION);
					break;
				case DNSRCodes.NO_ERROR_CODE:
					//No errors, but no answer
					System.out.println(fqdn + " -4 0.0.0.0");
					break;
				default:
					System.out.println(fqdn + " -4 0.0.0.0");
			}
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


