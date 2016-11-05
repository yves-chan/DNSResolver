import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


// Lots of the action associated with handling a DNS query is processing
// the response. Although not required you might find the following skeleton of
// a DNSreponse helpful. The class below has bunch of instance data that typically needs to be 
// parsed from the response. If you decide to use this class keep in mind that it is just a 
// suggestion and feel free to add or delete methods to better suit your implementation as 
// well as instance variables.



public class DNSResponse {
    private static final int RESPONSE_BYTE = 2;
    private static final int REPLY_CODE_BYTE = 3;
    private static final int QCOUNT_BYTE = 4;
    private static final int ANSWER_COUNT_BYTE = 6;
    private static final int NS_COUNT_BYTE = 8;
    private static final int ADDITIONAL_COUNT_BYTE = 10;
    private int processingByteOffset = 12;
    private String Qfqdn;
    private int queryID;                  // this is for the response it must match the one in the request
    private int answerCount = 0;          // number of answers  
    private boolean decoded = false;      // Was this response successfully decoded
    private int nsCount = 0;              // number of nscount response records
    private int qCount = 0;               // number of qcounts
    private int additionalCount = 0;      // number of additional (alternate) response records
    private boolean authoritative = false;// Is this an authoritative record
    private int replyCode = 0x0;
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
        this.queryID = (data[1] << 8 ) & 0xff00;
        this.queryID = this.queryID | (data[0]) & 0xff;

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

        // Determine NS_DESCRIPTION count
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
                    fqdn.add(getCompressedFQDN(fqdn_string, data, position));
                    break;
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
                fqdn.add(getCompressedFQDN(fqdn_string, data, position));
                break;
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

        if (rclass == DNSRClass.INTERNET) {
            switch (rtype) {
                case DNSRTypes.A_CODE:
                    byte[] ipv4 = new byte[4];
                    System.arraycopy(data, processingByteOffset, ipv4, 0, ipv4.length);
                    try {
                        ipAddress = InetAddress.getByAddress(ipv4);
                        responseRecord = new ResponseRecord(fqdn, ttl, DNSRClass.INTERNET, DNSRTypes.A_DESCRIPTION,
                            ipAddress.getHostAddress());
                    } catch (UnknownHostException e) {
                        System.out.println("Unknown host");
                    }
                    break;
                case DNSRTypes.CNAME_CODE:
                    String name = getCompressedFQDN(fqdn, data, processingByteOffset);
                    responseRecord = new ResponseRecord(fqdn, ttl, DNSRClass.INTERNET, DNSRTypes.CNAME_DESCRIPTION, name);
                    break;
                case DNSRTypes.NS_CODE:
                    name = getCompressedFQDN(fqdn, data, processingByteOffset);
                    responseRecord = new ResponseRecord(fqdn, ttl, DNSRClass.INTERNET, DNSRTypes.NS_DESCRIPTION, name);
                    break;
                case DNSRTypes.AAAA_CODE:
                    byte[] ipv6 = new byte[16];
                    System.arraycopy(data, processingByteOffset, ipv6, 0, ipv6.length);
                    try {
                        ipAddress = InetAddress.getByAddress(ipv6);
                        responseRecord = new ResponseRecord(fqdn, ttl, DNSRClass.INTERNET, DNSRTypes.AAAA_DESCRIPTION,
                            ipAddress.getHostAddress());
                    } catch (UnknownHostException e) {
                        System.out.println("Unknown host");
                    }
                    break;
                default:
                    responseRecord = new ResponseRecord(fqdn, ttl, rclass, Integer.toString(rtype), "----");
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

    public int getReplyCode() {
        return replyCode;
    }

    public String getCNAME() {
        if(answerCount>0) {
            if(getRecordType().equals("CN")) {
                return answerList[0].getRecordValue();
            }
        }
        return null;
    }



    public String getRecordType() {
        if(answerCount>0) {
            return answerList[0].getRecordType();
        }
        return null;
    }

    public ResponseRecord[] getAnswerList() {
        return answerList;
    }

    public ResponseRecord[] getAdditionalList() {
        return additionalList;
    }

    public ResponseRecord[] getNsList() {
        return nsList;
    }

    public InetAddress reQuery() {

        for(int i = 0; i< additionalCount; i++) {
            if(additionalList[i].getRecordType().equals(DNSRTypes.A_DESCRIPTION)) {
                try {
                    return InetAddress.getByName(additionalList[i].getRecordValue());
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        for(int i = 0; i< answerCount; i++) {
            if(answerList[i].getRecordType().equals(DNSRTypes.A_DESCRIPTION)) {
                try {
                    return InetAddress.getByName(answerList[i].getRecordValue());
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    // You will probably want a methods to extract a compressed FQDN, IP address
    // cname, authoritative DNS servers and other values like the query ID etc.


    // You will also want methods to extract the response records and record
    // the important values they are returning. Note that an IPV6 reponse record
    // is of type 28. It probably wouldn't hurt to have a response record class to hold
    // these records.
    public class ResponseRecord {
        private String name;
        private int ttl;
        private String recordType;
        private String recordValue;
        private int rclass;

        ResponseRecord(String name, int ttl, int rclass, String recordType, String recordValue){
            this.name = name;
            this.ttl = ttl;
            this.rclass = rclass;
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
}



