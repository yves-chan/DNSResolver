
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


// Lots of the action associated with handling a DNS query is processing
// the response. Although not required you might find the following skeleton of
// a DNSreponse helpful. The class below has bunch of instance data that typically needs to be 
// parsed from the response. If you decide to use this class keep in mind that it is just a 
// suggestion and feel free to add or delete methods to better suit your implementation as 
// well as instance variables.



public class DNSResponse {
    public static final int RESPONSE_BYTE = 2;
    public static final int REPLY_CODE_BYTE = 3;
    public static final int QCOUNT_BYTE = 4;
    public static final int ANSWER_COUNT_BYTE = 6;
    public static final int NS_COUNT_BYTE = 8;
    public static final int ADDITIONAL_COUNT_BYTE = 10;
    public static int processingByteOffset = 12;
    private String Qfqdn;
    private int queryID;                  // this is for the response it must match the one in the request
    private int answerCount = 0;          // number of answers  
    private boolean decoded = false;      // Was this response successfully decoded
    private int nsCount = 0;              // number of nscount response records
    private int qCount = 0;               // number of qcounts
    private int additionalCount = 0;      // number of additional (alternate) response records
    private boolean authoritative = false;// Is this an authoritative record
    private int replyCode = 0xf;
    private DNSQuery query;
    private ResponseRecord[] answerList;
    private ResponseRecord[] additionalList;
    private ResponseRecord[] nsList;

    // Note you will almost certainly need some additional instance variables.

    // When in trace mode you probably want to dump out all the relevant information in a response

	void dumpResponse() {
        System.out.println();
        System.out.println();
        System.out.println("Query ID:     "+ query.getQueryID() + " " + query.getLookup() + " --> " +
            query.getFromAddress().toString().substring(1));
        System.out.println("Response ID:  "+ queryID + " Authoritative " + authoritative);
        System.out.println("  Answers("+answerCount+")");
        for (ResponseRecord r : answerList) {
            r.printItems();
        }
        System.out.println("  Nameservers("+nsCount+")");
        for (ResponseRecord r : nsList) {
            r.printItems();
        }
        System.out.println("  Additional Information("+additionalCount+")");
        for (ResponseRecord r : additionalList) {
            r.printItems();
        }
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
        if ((data[RESPONSE_BYTE] & 0xc0) != 0x80) {
            //not a response
            return;
        }

        //Check authoritative response
        if ((data[RESPONSE_BYTE] & 0x4) != 0) {
            authoritative = true;
        }

        //Check reply code
        replyCode = data[REPLY_CODE_BYTE] & 0xf;
        if (replyCode != 0) {
            return;
        }

        // Determine Qcount
        qCount = (data[QCOUNT_BYTE] << 8) & 0xff00;
        qCount |= data[QCOUNT_BYTE+1] & 0xff;

        //Double Check question size at this point
        if (qCount!=1) {
            return;
        }

        // Determine Answer count
        answerCount = data[ANSWER_COUNT_BYTE] << 8 & 0xFF00;
        answerCount |= data[ANSWER_COUNT_BYTE+1] & 0xff;
        answerList = new ResponseRecord[answerCount];

        // Determine NS count
        nsCount = data[NS_COUNT_BYTE] << 8 & 0xFF00;
        nsCount |= data[NS_COUNT_BYTE+1] & 0xff;
        nsList = new ResponseRecord[nsCount];

        // Determine Additional count
        additionalCount = data[ADDITIONAL_COUNT_BYTE] << 8 & 0xFF00;
        additionalCount |= data[ADDITIONAL_COUNT_BYTE+1] & 0xff;
        additionalList = new ResponseRecord[additionalCount];

        Qfqdn = getFQDN(data);
        //Bypass Qtype and Qname
        processingByteOffset += 4;

        //Generate Answer List
        for (int i=0; i<answerCount; i++){
            answerList[i] = makeResponseRecord(data);
        }
        //Generate nsList
        for (int i=0; i<nsCount; i++){
            nsList[i] = makeResponseRecord(data);
        }
        //Generate Additional information list
        for (int i=0; i<additionalCount; i++){
            additionalList[i] = makeResponseRecord(data);
        }



	    // Extract list of answers, name server, and additional information response 
	    // records
	}

    private String getFQDN(byte[] data) {
        List<String> fqdn = new ArrayList<>();
        String fqdn_string = "";
        //iterate to see the length of the fqDN
        int position;
        try {
            while ((position = (data[processingByteOffset++] & 0xff)) != 0) {
                //compressed FQDN case
                if ((position & 0xC0) > 0) {
                    position = ((position & 0x3f) << 8) & 0xff00;
                    position |= (data[processingByteOffset++]) & 0xff;
                    return getCompressedFQDN(fqdn_string, data, position);
                } else {
                    //normal case
                    String part = "";
                    for (int i = 0; i < position; i++) {
                        part += (char) data[processingByteOffset++];
                    }
                    fqdn.add(part);
                }
            }
            if (fqdn.size() != 0) {
                fqdn_string = fqdn.get(0);
                for (int i = 1; i < fqdn.size(); i++) {
                    fqdn_string += "." + fqdn.get(i);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception in getFQDN");
        }

        return fqdn_string;
    }

    private String getCompressedFQDN(String fqdn_string, byte[] data, int offset) {
        List<String> fqdn = new ArrayList<>();
        //iterate to see the length of the fqDN
        int position;
        while ((position = (data[offset++] & 0xff)) != 0) {
            //compressed FQDN case
            if ((position & 0xC0) > 0) {
                position = ((position & 0x3f) << 8);
                position |= (data[offset++]) & 0xff;
                return getCompressedFQDN(fqdn_string, data, position);
            } else {
                //normal case
                String part = "";
                for (int i = 0; i < position; i++) {
                    part += (char) data[offset++];
                }
                fqdn.add(part);
            }
        }
        if (fqdn.size() != 0) {
            fqdn_string = fqdn.get(0);
            for (int i = 1; i < fqdn.size(); i++) {
                fqdn_string += "." + fqdn.get(i);
            }
        }
        return fqdn_string;
    }

    public ResponseRecord makeResponseRecord(byte[] data) {
        //TODO: make response record
        String fqdn = getFQDN(data);

        //Get rtype
        int rtype = (data[processingByteOffset++] << 8) & 0xff00;
        rtype |= data[processingByteOffset++] & 0xff;

        //Get rclass
        int rclass = (data[processingByteOffset++] << 8) & 0xff00;
        rclass |= data[processingByteOffset++] & 0xff;

        // The TTL value is 4 bytes
        int ttl = 0;
        for (int i= 0; i < 4; i++) {
            ttl = ttl << 8;
            ttl |= (data[processingByteOffset++] & 0xFF);
        }

        int responseLength = (data[processingByteOffset++] << 8) & 0xff00 ;
        responseLength |= data[processingByteOffset++] & 0xff;

        InetAddress ipAddress;
        ResponseRecord responseRecord = null;

        if (rclass == RRClass.INTERNET_VAL) {
            if (rtype == RRTypes.A_VAL) {
                byte[] ipv4 = new byte[4];
                System.arraycopy(data, processingByteOffset, ipv4, 0, ipv4.length);
                try {
                    ipAddress = InetAddress.getByAddress(ipv4);
                    responseRecord = new ResponseRecord(fqdn, ttl, RRTypes.A, ipAddress.getHostAddress());
                } catch (UnknownHostException e) {
                    System.out.println("Unknown host");
                }
            } else if (rtype == RRTypes.CNAME_VAL) {
                String name = getCompressedFQDN(fqdn, data, processingByteOffset);
                responseRecord = new ResponseRecord(fqdn, ttl, RRTypes.CNAME, name);
            } else if (rtype == RRTypes.NS_VAL) {
                String name = getCompressedFQDN(fqdn, data, processingByteOffset);
                responseRecord = new ResponseRecord(fqdn, ttl, RRTypes.NS, name);
            } else if (rtype == RRTypes.AAAA_VAL) {
                byte[] ipv6 = new byte[16];
                System.arraycopy(data, processingByteOffset, ipv6, 0, ipv6.length);
                try {
                    ipAddress = InetAddress.getByAddress(ipv6);
                    responseRecord = new ResponseRecord(fqdn, ttl, RRTypes.AAAA, ipAddress.getHostAddress());
                } catch (UnknownHostException e) {
                    System.out.println("Unknown host");
                }
            } else {
                //Other RR type classes
            }
        }
        processingByteOffset += responseLength;
        return responseRecord;
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
    class ResponseRecord {
        private String name;
        private int ttl;
        private String recordType;
        private String recordValue;

        ResponseRecord(String name, int ttl, String recordType, String recordValue){
            this.name = name;
            this.ttl = ttl;
            this.recordType = recordType;
            this.recordValue = recordValue;
        }

        public String getName() {
            return name;
        }
        public int getTtl() {
            return ttl;
        }
        public String getRecordType() {
            return recordType;
        }
        public String getRecordValue() {
            return recordValue;
        }

        void printItems() {
            System.out.format("       %-30s %-10d %-4s %s\n", name, ttl, recordType, recordValue);
        }

    }

    final class RRTypes {
        final static String A = "A";
        final static int A_VAL = 1;
        final static String NS = "NS";
        final static int NS_VAL = 2;
        final static String AAAA = "AAAA";
        final static int AAAA_VAL = 28;
        final static String CNAME = "CNAME";
        final static int CNAME_VAL = 5;
    }

    final class RRClass {
        final static int INTERNET_VAL = 1;
    }
}



