#!/usr/bin/python
# -*- coding: utf8 -*-

'''
Created on Sep 16, 2010
kNN: k Nearest Neighbors

Input:      inX: vector to compare to existing dataset (1xN)
            dataSet: size m data set of known vectors (NxM)
            labels: data set labels (1xM vector)
            k: number of neighbors to use for comparison (should be an odd number)

Output:     the most popular class label

@author: pbharrin
'''


'''
a0 = random.rand(4, 4), create a random 4, 4 numpy array
mat(a0) , convert array to matrix

matrix[row_start: row_end, col_start: col_end], slice a matrix:
    row_start: 起始行
    row_end: 结束行
    col_start: 起始列
    col_end: 结束列

数字的图像识别资源文件放在了 digits.zip 中
'''
from numpy import *
import operator
from os import listdir


def classify0(inX, dataSet, labels, k):
    dataSetSize = dataSet.shape[0]
    diffMat = tile(inX, (dataSetSize,1)) - dataSet
    sqDiffMat = diffMat**2
    sqDistances = sqDiffMat.sum(axis=1)
    distances = sqDistances**0.5
    sortedDistIndicies = distances.argsort()
    classCount={}
    for i in range(k):
        voteIlabel = labels[sortedDistIndicies[i]]
        classCount[voteIlabel] = classCount.get(voteIlabel,0) + 1
    sortedClassCount = sorted(classCount.iteritems(), key=operator.itemgetter(1), reverse=True)
    return sortedClassCount[0][0]

def createDataSet():
    group = array([[1.0,1.1],[1.0,1.0],[0,0],[0,0.1]])
    labels = ['A','A','B','B']
    return group, labels


# 将文件提取成matrix,
def file2matrix(filename):
    love_dictionary={'largeDoses':3, 'smallDoses':2, 'didntLike':1}
    fr = open(filename)
    arrayOLines = fr.readlines()
    numberOfLines = len(arrayOLines)            #get the number of lines in the file
    returnMat = zeros((numberOfLines,3))        #prepare matrix to return, 以 4 行3列填充matrix
    classLabelVector = []                       #prepare labels return
    index = 0
    for line in arrayOLines:
        line = line.strip()
        listFromLine = line.split('\t')
        returnMat[index,:] = listFromLine[0:3] # returnMat[index,:] 和 returnMat[index]效果是一样的返回第一行, listFromLine[0:3]返回[0,3)
        if(listFromLine[-1].isdigit()):
            classLabelVector.append(int(listFromLine[-1]))
        else:
            classLabelVector.append(love_dictionary.get(listFromLine[-1]))
        index += 1
    return returnMat,classLabelVector

# 格式化 dataset: Matrix , 统一化. formula: value-min/max-min
def autoNorm(dataSet):
    minVals = dataSet.min(0) # return a new matrix with only one row that consisted only smallest num in each col from the original matrix
    maxVals = dataSet.max(0) # return max in each cols
    ranges = maxVals - minVals
    normDataSet = zeros(shape(dataSet))#create a empty matrix same with dataset
    m = dataSet.shape[0]# dataset'row
    normDataSet = dataSet - tile(minVals, (m,1))
    normDataSet = normDataSet/tile(ranges, (m,1))   #element wise divide
    return normDataSet, ranges, minVals

# 测试算法, 返回错误率, error_preditions / total_predictions
def datingClassTest():
    hoRatio = 0.50      #hold out 10%
    datingDataMat,datingLabels = file2matrix('datingTestSet2.txt')       #load data setfrom file
    normMat, ranges, minVals = autoNorm(datingDataMat)
    m = normMat.shape[0]
    numTestVecs = int(m*hoRatio)
    errorCount = 0.0
    for i in range(numTestVecs):
        classifierResult = classify0(normMat[i,:],normMat[numTestVecs:m,:],datingLabels[numTestVecs:m],3)
        print "the classifier came back with: %d, the real answer is: %d" % (classifierResult, datingLabels[i])
        if (classifierResult != datingLabels[i]): errorCount += 1.0
    print "the total error rate is: %f" % (errorCount/float(numTestVecs))
    print errorCount

def classifyPerson():
    resultList = ['not at all', 'in small doses', 'in large doses']
    percentTats = float(raw_input(\
                                  "percentage of time spent playing video games?"))
    ffMiles = float(raw_input("frequent flier miles earned per year?"))
    iceCream = float(raw_input("liters of ice cream consumed per year?"))
    datingDataMat, datingLabels = file2matrix('datingTestSet2.txt')
    normMat, ranges, minVals = autoNorm(datingDataMat)
    inArr = array([ffMiles, percentTats, iceCream, ])
    classifierResult = classify0((inArr - minVals)/ranges, normMat, datingLabels, 3)
    print "You will probably like this person: %s" % resultList[classifierResult - 1]

# 将图片信息转换成matrix
def img2vector(filename):
    returnVect = zeros((1,1024))# 1 行 1024 列
    fr = open(filename)
    for i in range(32):#每行 (32x32)的文本, 图片2位化后的
        lineStr = fr.readline()
        for j in range(32):#每列
            returnVect[0,32*i+j] = int(lineStr[j])
    return returnVect

def handwritingClassTest():
    # 填充数据集
    hwLabels = []
    trainingFileList = listdir('trainingDigits')           #load the training set
    m = len(trainingFileList)
    trainingMat = zeros((m,1024))
    for i in range(m):
        fileNameStr = trainingFileList[i]
        fileStr = fileNameStr.split('.')[0]     #take off .txt
        classNumStr = int(fileStr.split('_')[0]) #取出样本标示的数字
        hwLabels.append(classNumStr)
        trainingMat[i,:] = img2vector('trainingDigits/%s' % fileNameStr)

    # 测试算法精度
    testFileList = listdir('testDigits')        #iterate through the test set
    errorCount = 0.0
    mTest = len(testFileList)
    for i in range(mTest):
        fileNameStr = testFileList[i]
        fileStr = fileNameStr.split('.')[0]     #take off .txt
        classNumStr = int(fileStr.split('_')[0])
        vectorUnderTest = img2vector('testDigits/%s' % fileNameStr)
        classifierResult = classify0(vectorUnderTest, trainingMat, hwLabels, 3)
        print "the classifier came back with: %d, the real answer is: %d" % (classifierResult, classNumStr)
        if (classifierResult != classNumStr): errorCount += 1.0
    print "\nthe total number of errors is: %d" % errorCount
    print "\nthe total error rate is: %f" % (errorCount/float(mTest))


# > group, labels = kNN.createDataSet()
# > kNN.classify0([0, 0], group, labels, 3)
# > 'B'
def _classify0(inX, dataSet, labels, k):
    dataSetSize = dataSet.shape[0] # return (4, 2), which is 4 rows, 2 cols
    '''
    tile([0, 0], (4, 1)):
        array([[0, 0],
           [0, 0],
           [0, 0],
           [0, 0]])
    tile(inX, (dataSetSize, 1)) - dataSet: 输出每个点对应的距离差
        array([[-1. , -1.1],
           [-1. , -1. ],
           [ 0. ,  0. ],
           [ 0. , -0.1]])
    '''
    diffMat = tile(inX, (dataSetSize, 1)) - dataSet
    '''

    每个节点的平方：
        array([[ 1.  ,  1.21],
           [ 1.  ,  1.  ],
           [ 0.  ,  0.  ],
           [ 0.  ,  0.01]])
    '''
    sqDiffMat = diffMat**2
    # array([ 2.21,  2.  ,  0.  ,  0.01]), 返回x轴的和
    sqDistances = sqDiffMat.sum(axis=1)
    # array([ 1.48660687,  1.41421356,  0.        ,  0.1       ])
    distances = sqDistances**0.5 #开根号
    # array([2, 3, 1, 0]), argsort 升序排序, 返回坐标
    sortedDistIndicies = distances.argsort()
    classCount={}
    for i in range(k):
        voteIlabel = labels[sortedDistIndicies[i]] #返回头k个节点的标签
        classCount[voteIlabel] = classCount.get(voteIlabel, 0) + 1
    sortedClassCount = sorted(classCount.iteritems(), key=operator.itemgetter(1), reverse=True) #返回票数最高的节点
    return sortedClassCount[0][0]