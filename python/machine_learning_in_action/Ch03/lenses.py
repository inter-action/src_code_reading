#!/usr/bin/python
# -*- coding: utf8 -*-
#
import trees
import treePlotter

fr = open('lenses.txt')
lenses = [line.strip().split('\t') for line in fr.readlines()]
lensesLabel = ['age', 'prescript', 'astigmatic', 'tearRate']
lensesTree = trees.createTree(lenses, lensesLabel)
treePlotter.createPlot(lensesTree)