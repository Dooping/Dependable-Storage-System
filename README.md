# CSD - TP1

## Arguments
### SSLRestServer0
```
 -bz,--byzantine <arg>   number of replicas that are byzantine
 -ch,--chance <arg>      probability of crashing/byzantine error
 -cr,--crash <arg>       number of replicas to crash
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
- Create a Proxy e.g. srv.SSLRestServer0 -t proxy -k ./server.jks -f akka.ssl.tcp://FaultDetection@192.168.99.1:2563/user/faultDetection  -bz 1 -ch 10
- Create a Client to send requests or run benchmarks e.g. clt.ClientGet0 -k ./client.jks -h 192.168.99.1

## Other notes:
- To change keystores and truststores used by the replicas and the proxy actors to communicate with each other we must go to main/resources/application.conf and change their path and passwords

