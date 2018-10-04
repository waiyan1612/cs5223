# Bugs

* When stress test begins, 4 games are initialized. Somehow, we can see all 4 players in all 4 games which shouldn't be the case. Ideally, the first game should show only one player until he makes a move etc. We might be fetching the latest game state somehow somewhere. 

* `acquireAndListen` sometimes cannot update the game state (usually happens with `ae` in stress test)

* `ITrackerState` can fail (Only saw it once during the stress test. Need to add error handling to retry in all the catch clauses instead of System.exit)

* PlayerList in `gameState` and `trackerState` are not in sync. Meaning some entries in `tracker` can never be removed. I tried to remove it in `Game.getStub()` but it makes things worse.

* Sometimes, primary and secondary server are the same in the UI.
