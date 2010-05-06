#!/bin/python
# -*- coding: iso8859-1 -*-

"""
Demonstrates the validity of the following method:

Having done a little maths, I've convinced myself that a variance
based on an erroneous mean 'a' can be converted to one based on the
"correct" mean 'b' using:

Vb = Va + (a-b)^2

Here's my calculation:

Va = avg[ (x - (b + (a -b)))^2 ]
     = avg[ xx - xb - xa + xb - xb + bb + ab - bb - xa + ab + aa - ab + xb - bb - ab + bb ]
     = avg[ xx - 2xb + bb ] + avg[ - 2xa + 2xb  - bb  + aa ]
     = Vb - 2aa + 2ab - bb + aa
     = Vb - (a-b)^2

I was a flumoxed for a while by Vb always being larger than Va, but I
ended up figuring that was reasonable as the mean will differ from the
sample mean, so the sample values will be further away from the mean,
so the variance should be larger.


So here's what I propose to do:

 1. Each worker process will calculate the variance for the individual
 tests, and report this and the sample mean per worker sample.

 2. At the end of each console sample period, the console will have
 many tuples of (n, Va, a) - one per worker process report (each
 worker process typically reports many times in a sample period). It
 will use the sample period mean to calculate Vb for each tuple and
 the produce an overall variance figure by calculated the weighted
 average using n - the number of tests per report for each tuple.

 3. The "sample quality" will be calculated by taking the square root
 of the overall variance and dividing by the console mean.
"""


import math
import random

class BasicValueGenerator:
    def __init__(self):
        self.bias = random.randint(0, 100)

    def __call__(self):
        return self.bias + random.randint(0, self.bias/2)

class NormalValueGenerator:
    def __init__(self):
        self.mean = random.randint(-100, 100)
        self.sigma = random.randint(0, 100)        

    def __call__(self):
         return random.normalvariate(self.mean, self.sigma)

def _mean(values):
    if len(values) == 0:
        return 0 # Really NaN, but this is easier to work with.
    
    result = 0.0
    for v in values: result += v
    result /= len(values)
    return result

def _variance(values, mean):
    if len(values) == 0: return 0

    result = 0.0
    for v in values:
        x = v - mean
        result += x*x
    result /= len(values)
    return result

def _weightedMean(valueAndWeights):
    weightedTotal = 0
    totalWeight = 0

    for (value, weight) in valueAndWeights:
        weightedTotal += value * weight
        totalWeight += weight

    return weightedTotal/totalWeight

class Sample:
    def __init__(self, values):
        self.values = values
        self.n = len(values)
        self.mean = _mean(values)
        self.variance = _variance(values, self.mean)
        self.sigma = math.sqrt(self.variance)
        self.q = self.sigma and self.sigma/self.mean or 0

    def __repr__(self):
        return "n=%2s µ=%5.2f s²=%.2f s=%.2f q=%.2f" % \
               (self.n, self.mean, self.variance, self.sigma, self.q)

        
class Worker:
    def __init__(self,
                 generate,
                 maximumSampleSize = 30):
        self.maximumSampleSize = maximumSampleSize
        self.generate = generate

    def generateSample(self):
        sampleSize = random.randint(0, self.maximumSampleSize)
        values = [self.generate() for x in range(0, sampleSize)]
        return Sample(values)
        
def flattenValues(listOfSamples):
    result = []
    for sample in listOfSamples:
        result.extend(sample.values)
    return result



def calculateVarianceFromSamples(samples):
    """
    Calculate the overall variance from aggregate statistics for each
    sample, i.e. (mean, variance, n) for each sample. Doesn't use the
    underlying values at all.
    """
    
    # Overall mean.
    mean = _weightedMean([(sample.mean, sample.n) for sample in samples])

    def offsetVariance(sampleMean, sampleVariance, actualMean = mean):
        differenceInMeans = sampleMean - actualMean
        return sampleVariance + differenceInMeans * differenceInMeans

    return _weightedMean([(offsetVariance(s.mean, s.variance), s.n)
                          for s in samples])


def run(numberOfWorkers = 4, numberOfSamples = 10):
    allSamples = []
    workerAggregateSamples = []

    for workerNumber in range(0, numberOfWorkers):
        if workerNumber % 2 == 0:
            worker = Worker(BasicValueGenerator())
        else:
            worker = Worker(NormalValueGenerator())

        samples = [worker.generateSample() for x in range(0, numberOfSamples)]

        print "\nWorker %d" % workerNumber

        for sample in samples:
            print " Sample: %s" % sample

        aggregate = Sample(flattenValues(samples))
        print " All: %s" % aggregate

        allSamples.extend(samples)
        workerAggregateSamples.append(aggregate)

    total = Sample(flattenValues(allSamples))
    print "\nTotal: %s\n" % total

    print "Actual variance: %f" % total.variance

    varianceFromSamples = calculateVarianceFromSamples(allSamples)
    print "Variance from worker samples: %f" % varianceFromSamples



if __name__ == "__main__":
    run()
