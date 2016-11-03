
import java.net.InetAddress;
import java.util.Arrays;


// Lots of the action associated with handling a DNS query is processing
// the response. Although not required you might find the following skeleton of
// a DNSreponse helpful. The class below has bunch of instance data that typically needs to be 
// parsed from the response. If you decide to use this class keep in mind that it is just a 
// suggestion and feel free to add or delete methods to better suit your implementation as 
// well as instance variables.



public class DNSResponse {
    private int queryID;                  // this is for the response it must match the one in the request 
    private int answerCount = 0;          // number of answers  
    private boolean decoded = false;      // Was this response successfully decoded
    private int nsCount = 0;              // number of nscount response records
    private int qCount = 0;               // number of qcounts
    private int additionalCount = 0;      // number of additional (alternate) response records
    private boolean authoritative = false;// Is this an authoritative record
    private int replyCode = 0xf;
    private DNSQuery query;

    // Note you will almost certainly need some additional instance variables.

    // When in trace mode you probably want to dump out all the relevant information in a response

	void dumpResponse() {
        System.out.println("Query ID:          "+ query.getQueryID() + " " + query.getLookup() + " --> " +
            query.getFromAddress().toString().substring(1));
        System.out.println("Response ID:       "+ queryID + " Authoritative " + authoritative);
        System.out.println("    Answers("+answerCount+")");
        //TODO: print list of answers
        System.out.println("    Nameservers("+nsCount+")");
        //TODO: print list of nameservers
        System.out.println("    Additional Information("+additionalCount+")");
        //TODO: print list of additional information
    }

    // The constructor: you may want to add additional parameters, but the two shown are 
    // probably the minimum that you need.

	public DNSResponse (DNSQuery query) {
        this.query = query;
        byte[] data = query.getPacket().getData();
	    // The following are probably some of the things 
	    // you will need to do.
	    // Extract the query ID
        this.queryID = query.getQueryID();
        System.out.println(Arrays.toString(data));

        // Check if Data is a response
        if ((data[2] & 0xc0) != 0x80) {
            //not a response
            return;
        }

        //Check authoritative response
        if ((data[2] & 0x4) != 0) {
            authoritative = true;
        }

        //Check reply code
        replyCode = data[3] & 0xf;
        if (replyCode != 0) {
            return;
        }

        // Determine Qcount
        qCount = data[4] << 8 & 0xff00;
        qCount |= data[5] & 0xff;

        // Determine Answer count
        answerCount = data[6] << 8 & 0xff00;
        answerCount |= data[7] & 0xff;

        // Determine NS count
        nsCount = data[8] << 8 & 0xff00;
        nsCount |= data[9] &0xff;

        // Determine Additional count
        additionalCount = data[10] << 8 & 0xff00;
        additionalCount |= data[11] & 0xff;

	    // Extract list of answers, name server, and additional information response 
	    // records
	}

    public int getQueryID() {
        return queryID;
    }

    public int getAnswerCount() {
        return answerCount;
    }

    public boolean isDecoded() {
        return decoded;
    }

    public int getNsCount() {
        return nsCount;
    }

    public int getAdditionalCount() {
        return additionalCount;
    }

    public boolean isAuthoritative() {
        return authoritative;
    }


    // You will probably want a methods to extract a compressed FQDN, IP address
    // cname, authoritative DNS servers and other values like the query ID etc.


    // You will also want methods to extract the response records and record
    // the important values they are returning. Note that an IPV6 reponse record
    // is of type 28. It probably wouldn't hurt to have a response record class to hold
    // these records. 
}


