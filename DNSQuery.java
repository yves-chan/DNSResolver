import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

/**
 * Created by yves-chan on 02/11/16.
 */
public class DNSQuery {
    public static final int SO_TIMEOUT = 3000;
    public static final int RESPONSE_BYTE_SIZE = 512;
    public static final int PORT = 53;
    public static final int QUERY_HEADER_LENGTH = 12;
    public static final String QNAME_END = "00";
    public static final String QTYPE_DOMAIN_NAME = "0001";
    public static final String QCLASS_INTERNET = "0001";
    public static final int MAX_RAND_INT = 65536;
    private String lookup;
    private InetAddress fromAddress;
    private DatagramSocket socket;
    private DatagramPacket packet;
    private int queryID;
    private byte[] header;
    private byte[] data;
    private int retryAttempts = 0;



    public DNSQuery(InetAddress fromAddress, String lookup){
        this.lookup = lookup;
        this.fromAddress = fromAddress;
        this.header = generateHeader();
    }

    public void sendQuery(){
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        // send request
        data = makeRequest(lookup);
        try {
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, fromAddress, PORT);
            socket.send(sendPacket);
            socket.setSoTimeout(SO_TIMEOUT);

            // get response
            byte [] b = new byte[RESPONSE_BYTE_SIZE];
            packet = new DatagramPacket(b, b.length);
            socket.receive(packet);
        } catch (IOException e){
            sendQuery();
            retryAttempts++;
            if (retryAttempts > 10) {
                System.out.println("Reached maximum Level of retries");
                return;
            }
        }
    }
    public byte[] makeRequest(String lookup){
        byte[] encodedLookup = encodeLookup(lookup);
        byte[] request = new byte[encodedLookup.length + header.length];
        System.arraycopy(header, 0, request, 0, header.length);
        System.arraycopy(encodedLookup, 0, request, header.length, encodedLookup.length);
        this.data = request;
        return request;
    }

    public byte[] encodeLookup(String lookup){
        String[] domain = lookup.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String s : domain) {
            String nsLength = Integer.toHexString(s.length());
            //append 0 to length of string if less that 1
            if (nsLength.length() < 2) {
                nsLength = "0"+nsLength;
            }
            sb.append(nsLength);
            //for each of the strings in split, get their hex value of each char
            for (char c : s.toCharArray()) {
                sb.append(Integer.toHexString((int) c));
            }
        }
        //End of QNAME
        sb.append(QNAME_END);
        // Type of Qtype is internet
        sb.append(QTYPE_DOMAIN_NAME);
        // Q class is internet
        sb.append(QCLASS_INTERNET);
        String encodedLookup = sb.toString();
        return DatatypeConverter.parseHexBinary(encodedLookup);
    }


    public byte[] generateHeader() {
        byte[] qID = generateRandomByte();
        //Generate header
        byte[] header = new byte[QUERY_HEADER_LENGTH];
        System.arraycopy(qID, 0, header,0, qID.length);
        //set QDCOuNT to 1
        header[5] = (byte) 0x01;
        this.header = header;
        return header;
    }

    private byte[] generateRandomByte() {
        // Use two bytes for qID
        Random random = new Random();
        int randInt = random.nextInt(MAX_RAND_INT);
        this.queryID = randInt;
        return new byte[]{(byte) (randInt&0xFF), (byte) ((randInt >> 8) &0xFF)};
    }

    public int getQueryID() {
        return queryID;
    }

    public String getLookup() {
        return lookup;
    }

    public InetAddress getFromAddress() {
        return fromAddress;
    }

    public DatagramPacket getPacket() {
        return packet;
    }

}
