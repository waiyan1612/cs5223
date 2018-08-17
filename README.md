# cs5223
P2P Distributed Maze Game

### Compile
```
cd src
javac Tracker.java
javac Game.java
```

### Start RMI Registry
```
rmiregistry &
```

### Kill RMI Registry
```
kill -9 $(ps aux | grep 'rmiregistry' | awk '{print $2}')
```

### Start Tracker
```
java Tracker
```
or with port number, N and K
```
java Tracker 1099 10 10
```

### Start Game
When DEBUG=TRUE
```
java Game
```
or
```
java Game playerName
```
When DEBUG=FALSE
```
java Game trackerIP trackerPort playerName
```