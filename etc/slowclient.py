#!/bin/python
# -*- coding: iso8859-1 -*-

"""
Prototype of the slow buffer algorithm.
"""

import random

class SlowClientBufferGrowthStrategy:
    
    def __init__(self, targetBaud, dampingFactor):
        self.targetBaud = targetBaud
        self.dampingFactor = dampingFactor
        self.sleepTime = 0
        self.lastBufferResizeTime = 0
        self.lastPosition = 0
    
    def calculateSleepTime(self, now, position):
        if position != 0:
            timeSinceLastResize = now - self.lastBufferResizeTime
            
            self.sleepTime += \
                ((position - self.lastPosition) * 8 * 1000 / self.targetBaud \
                - timeSinceLastResize) * self.dampingFactor;
            
            if self.sleepTime < 0: self.sleepTime = 0
            
        self.lastPosition = position          
        self.lastBufferResizeTime = now
        
        return self.sleepTime



class CumulativeSlowClientBufferGrowthStrategy:
    
    def __init__(self, targetBaud, dampingFactor):
        self.targetBaud = targetBaud
        self.dampingFactor = dampingFactor
    
    def calculateSleepTime(self, now, position):
        if position == 0:
            self.startTime = now
            self.sleepTime = 0
            self.damping = 2
        else:
            self.sleepTime +=  \
              ((position) * 8 * 1000 / self.targetBaud - \
                (now - self.startTime))  * \
                self.damping
            self.damping = self.dampingFactor
            
            if self.sleepTime < 0: self.sleepTime = 0
        
        self.sleepTime = int(self.sleepTime)
        return self.sleepTime

def run():
    target = 512000
    s = CumulativeSlowClientBufferGrowthStrategy(target, .33)
    lastSleepTime = 0
    
    now = 100
    
    def baud(bytes, milliseconds):
        if milliseconds: return bytes * 8 * 1000 / milliseconds
        else: return 0
    
    for run in range(1, 2):
        print "***** RUN %d *****" % run
        position = 0
        start = now
        
        for x in range(0, 200):
            before = now
            beforePosition = position
            sleepTime = s.calculateSleepTime(now, position)
            
            workTime = random.normalvariate(3, .5)
            increment = random.randint(900, 1000)

            position += increment
            now += sleepTime + workTime
            
            cumulativeBaud = baud(position, (now - start))
            instantBaud = baud(increment, sleepTime + workTime)
            
            print "f(%6d, %6d) -> %2d (%+d) => %6d (%.2f%%) %6d" % (before, beforePosition, sleepTime, sleepTime - lastSleepTime, cumulativeBaud, (cumulativeBaud - target)* 100.0/target, instantBaud)
            lastSleepTime = sleepTime

if __name__ == "__main__":
    run()
