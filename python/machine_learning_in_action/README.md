# Original Repo
[Original Repo](https://github.com/pbharrin/machinelearninginaction)

# Desc
这里的代码是来自 [Machine.Learning.in.Action(2012.3)].Peter.Harrington 一书
这里的代码是用python2 写的, 主要依赖 numpy 和 matplotlib. 这里的代码有我阅读过的标注和修改。

# Tools
[wolframalpha](http://www.wolframalpha.com/)

# Notes

## chapter 3: Splitting datasets one feature at a time: decision trees
这章也是讲分类的一种方式，用 [ID3 algorithm](https://en.wikipedia.org/wiki/ID3_algorithm) 去将数据自动归类。
大概的方式就是计算计算数据的每个特性的 Shannon Entropy 来获取最优的特性进行分割，
直到没有数据去继续分割, 最终构建一个根据每个特性值节点的分类树.最终按照数据特性的值根据此决策树去分类。

lenses.py 是一个完整的应用

ID3 算法缺陷
>In chapter 9 we’ll also investigate another decision tree algorithm called CART. The algorithm we used in this chapter, ID3, is good but not the best. ID3 can’t handle numeric values. We could use continuous values by quantizing them into discrete bins, but ID3 suffers from other problems if we have too many splits.

* Information gain
    The change in information before and after the split is known as the information gain.
* [Shannon entropy](https://en.wikipedia.org/wiki/Entropy_(information_theory))
    其中公式中 Ixi = log(b, p(xi)), Ixi 代表了信息量的大小(具体看wikipedia的解释), p(xi) 的概率 [0, 1]. 1表示这个事件总会发生。信息量为0
    p(xi) 约接近0, 所代表的这个事件发生的时候产生的而外的信息量越大
    值越大代表数据越混乱
* [Gini impurity]Another common measure of disorder in a set is the Gini impurity,which is the probability of choosing an item from the set and the probability of that item being misclassified.

remain problems:

* @page 59

    >The tree in figure 3.8 matches our data well; however, it probably matches our data too well. This problem is known as overfitting. In order to reduce the problem of over- fitting, we can prune the tree. This will go through and remove some leaves. If a leaf node adds only a little information, it will be cut off and merged with another leaf. We’ll investigate this further when we revisit decision trees in chapter 9.