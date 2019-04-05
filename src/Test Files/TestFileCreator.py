import random

startHour = 12
startMin = 56
startSec = 23
startMs = 453

hourStep = 0
minStep = 0
secStep = 7
msStep = 0

numFloors = 22

numRequests = 10

groundWeight = 0.5

up = "UP"
down = "DOWN"

with open("TestFile.txt", "w") as f:
    for i in range(0, numRequests):
        groundCheck = random.uniform(0, 1)
        
        if groundCheck <= groundWeight:
            startFloor = 0
        else:
            startFloor = random.randint(0, numFloors - 1)
            
        while (True):
            endFloor = random.randint(0, numFloors - 1)
            if (endFloor != startFloor):
                break
        
        dir = ""
        
        if (startFloor < endFloor):
            dir = up
        elif (startFloor > endFloor):
            dir = down
        elif (startFloor == endFloor):
            if (startFloor == 0):
                dir = up
            else:
                dir = down
        
            
        currHour = startHour + hourStep * i
        currMin = startMin + minStep * i
        currSec = startSec + secStep * i
        currMs = startMs + msStep * i
        
        currSec = currSec + currMs / 1000
        currMs = currMs % 999
        
        currMin = currMin + currSec / 60
        currSec = currSec % 60
        
        currHour = currHour + currMin / 60
        currMin = currMin % 60
        
        currHour = currHour % 24
        
        toPrint = (str(int(currHour)) + ":" +
                   str(int(currMin)) + ":" +
                   str(int(currSec)) + "." +
                   str(int(currMs)) + " " +
                   str(startFloor) + " " +
                   str(dir) + " " +
                   str(endFloor) + "\n")
        print(toPrint)
        f.write(toPrint)
        
print("DONE")
                

