- Decide on the communication channel & library to use
- Boundry restriction and invalid move detection should be done on player side or primary server side
    - Player cannot move south if he is already at southmost
    - Two player cannot coexist in same tile
- What is the procedure for primary & secondary to keep each other up-to-date. Step by step pseudo code might be useful to catch corner cases
- What is the procedure when the primary server crash?  How to know if a primary server crash (just because u can't communicate with server doesn't mean server crashed.  It might be ur network that is faulty). Step by Step pseudo code will be useful here as well
- What is the procedure when the secondary server crash?
- What is the procedure when the primary player exit the game.  will it be the same as the crash?
- What is the procedure when the secondary player exit the game.  Will it be the same as the crash?
- What is the procedure while during the primary server crash, players send play moves to the server? How should it be handled, how can we know who move what first?
- Any corner cases for when system is creating a new primary server (in case of crash)


Feel free to add questions and also to delete questions that might be redundant. 