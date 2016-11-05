# How to Use
`java -jar DNSlookup.jar rootDNS name [-t].`

rootDNS - this is the IP address (in dotted form) of the DNS server you are to start your search at. It may or may
    not be a root DNS server.

name - this is the fully qualified domain name you are to lookup.

-t - This option specifies that the program is to print a trace of all the queries made and responses received.
    (In trace mode if a query is resent because of a timeout the resent query is printed.) The default behaviour is to
    print the name, its time to live and the resolved IP address.

# Example
`java -jar DNSlookup.jar 199.7.83.42 finance.google.ca -t`
