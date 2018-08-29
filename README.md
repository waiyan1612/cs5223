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
```
java Game
```
or
```
java Game trackerIP trackerPort playerName
```

### Stress Test
```
java StressTest 127.0.0.1 1099 "java Game"
```