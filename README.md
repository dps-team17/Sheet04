#Distributed Systems Lab ­ Homework 4
##Unstructured P2P (12 points)
This assignment is focusing on the establishment of an unstructured P2P overlay network as has
already been mentioned within the previous assignment sheet. Till 19.04.2016

###Assignment:
An Unstructured P2P Network
Implement an application which is capable of interacting with other peers in a P2P­like
fashion. To save time, only implement features actually requested or required to support
those. Read the full assignment and think before implementing features.
Consider the following description of a P2P network:
Each node within the network maintains a ServerSocket instance bound to some (arbitrary)
port number. Further, each node maintains a set of n (e.g. n=3) known nodes (node table).
Every entry within this set should consist of an IP address and a port number which can be
used to contact remote nodes. When a peer is started, the user specifies the IP/port
number of a single other node to initialize the node table unless it is the first peer. In this
case the table is left empty.
Periodically (e.g. every 5 seconds) each node is picking a random node entry within its
table. Let A be the node picking an entry which is addressing node B. A establishes a
connection to B and sends its set of known nodes plus an entry addressing A itself to B. B
responds with his own set of known nodes. After the exchange both sides merge the local
and the received tables and remove their own address from the result (to avoid having a
self­reference within the node table). Finally, each side picks a random subset of n entries
and replaces its local node table by the result. In case A is not able to establish a
connection to B (since it is offline or broken), A removes the entry for B from its table and
picks another entry until there is no further entry left.
Process the following steps – as usual, tips regarding the structuring of your application
may be ignored:

    a. Implement a utility class encapsulating the management of the P2P overlay network as described above. 
    Let n be a runtime parameter. The utility should offer methods for connecting to the network, disconnecting 
    from the network, a method obtaining the local peer address and one listing the current state of the local 
    node (e.g. node table). It should hide the management of all required resources (sockets, threads, 
    node tables, potential shutdown hooks, …)

    b. Implement a client application utilizing your utility class to connect to a P2P network.

    c. Demonstrate the proper operation of your implementation by creating a network
    of 3∗n nodes, followed by removing and adding instances. Find a way to present
    the propagation of the information of entering and leaving/dying nodes
    throughout the network. You might run multiple peers within the same process.
    (a­c: 5 points)

    d. Describe an “effective” way of propagating a one­to­all message on top of your
    network. Try to keep the number of messages required to be forwarded low while
    still providing a “best effort” guarantee that messages are delivered to all nodes.

    e. Implement your one­to­all message propagation mechanism on top of your
    network infrastructure by adding a corresponding method to your network utility +
    a listener interface for the receiver side.

    f. Demonstrate the propagation of your message within a moderately sized network
    involving 3∗n nodes. (d­f: 3.5 points)

    g. Assign a name to every peer within the network. Describe a name­resolution
    protocol on top of your network infrastructure. For instance, somebody wants to
    contact peer ‘XY’ – it therefore needs his IP / port number. How can it be
    obtained? Consider the possibility of ‘XY’ not being present or reachable within the
    network.

    h. Implement your lookup protocol by adding a corresponding method to your utility
    class and demonstrate its proper operation. (g+h: 3.5 points)
