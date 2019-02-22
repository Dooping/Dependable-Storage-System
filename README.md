
# Dependable Storage System
## Reliability of Distributed Systems Final Project

The goal of this project was to implement the algorithm ABD (Attiya, Bar-Noy, Dolev) in secure channels.

The system supports byzantine faults and follows the ratio N=3f+1, where 'N' is the total number of replicas and 'f' is the number of faulty replicas.

The second part of this project was using Homomorphic encryption to maintain certain properties of the encrypted data.

The different homomorphic encryptions used were:
- CHE – Comparative Homomorphic Encryption
- OPE - Order Preserved Encryption
- LSE - Linear Serachable Encryption
- PSSE – Paillier Sum Supported Encryption
- MSE – Multiplication Supported Encryption

The system has detection of faulty replicas, taking them down if necessary and replacing them with new ones.

Evaluation was done by comparing the throughput (operations per second) of the system without faulty replicas, with the throughput of the system with faulty replicas (crashing or byzantine).

The weight of each homomorphic encryption was also evaluated.

For more information, consult report.pdf (written in english).


## Arguments

### SSLRestServer0
```
 -bz,--byzantine <arg>   number of replicas that are byzantine
 -ch,--chance <arg>      probability of crashing/byzantine error
 -cr,--crash <arg>       number of replicas to crash
 -e,--encrypt            use homomorfic encryption
 -f,--fault <arg>        fault detection server's address
 -k,--keystore <arg>     keystore path
 -n,--number <arg>       number of replicas to spawn
 -q,--quorum <arg>       quorum size
 -s,--sentinent <arg>    number of sentinent replicas to spawn
 -t,--type <arg>         type of server (spawner1/spawner2/proxy/fault)
 -th,--threshold <arg>   number of votes to kick a replica
```

### ClientGet0
```
 -k,--keystore <arg>     truststore path
 -h,--hostname <arg>     rest server address 
```

## Running Instructions:

- Create the Fault Detection Server e.g. srv.SSLRestServer0 -t fault
- Create Spawners with replicas e.g. srv.SSLRestServer0 -t spawner1 -n 4 -s 1 -f akka.ssl.tcp://FaultDetection@192.168.99.1:2563/user/faultDetection
- Create a Proxy e.g. srv.SSLRestServer0 -t proxy -k ./server.jks -e -f akka.ssl.tcp://FaultDetection@192.168.99.1:2563/user/faultDetection -bz 1 -ch 10
- Create a Client to send requests or run benchmarks e.g. clt.ClientGet0 -k ./client.jks -h 192.168.99.1
- The Client will immeadiately receive the allowed Entry configuration from the Server as soon as the Client makes a connection.

## Other notes:
- To change keystores and truststores used by the replicas and the proxy actors to communicate with each other we must go to main/resources/application.conf and change their path and passwords
