# CSD - TP1

## Arguments
### SSLRestServer0
```
 -bz,--byzantine <arg>   number of replicas that are byzantine
 -ch,--chance <arg>      probability of crashing/byzantine error
 -cr,--crash <arg>       number of replicas to crash
 -k,--keystore <arg>     keystore path
 -n,--number <arg>       number of replicas to spawn
 -q,--quorum <arg>       quorum size
 -r,--replicas <arg>     list of replica's adresses
 -t,--type <arg>         type of server (spawner1/spawner2/proxy)
```

### ClientGet0
```
 -k,--keystore <arg>     keystore path
 -h,--hostname <arg>     rest Server address 
```

## Running Instructions:
- Create Spawners with replicas e.g. srv.SSLRestServer0 -t spawner1 -n 4 or srv.SSLRestServer0 -t spawner2 -n 3
- Create a Proxy giving every replica's addresses as an argument e.g.:
```
srv.SSLRestServer0 -t proxy -k ./server.jks -r akka.ssl.tcp://Spawner1@192.168.99.1:2552/user/r1 akka.ssl.tcp://Spawner1@192.168.99.1:2552/user/r2 akka.ssl.tcp://Spawner1@192.168.99.1:2552/user/r3 akka.ssl.tcp://Spawner1@192.168.99.1:2552/user/r4 akka.ssl.tcp://Spawner2@192.168.99.1:2553/user/r1 akka.ssl.tcp://Spawner2@192.168.99.1:2553/user/r2 akka.ssl.tcp://Spawner2@192.168.99.1:2553/user/r3
```
- Create a Client to send requests or run benchmarks e.g. clt.ClientGet0 -k ./client.jks -h 192.168.99.1


