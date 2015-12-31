# Original Repo
[Original Repo](https://github.com/pbharrin/machinelearninginaction)

# Desc
这里的代码是来自 [Machine.Learning.in.Action(2012.3)].Peter.Harrington 一书
这里的代码是用python2 写的, 主要依赖 numpy 和 matplotlib. 这里的代码有我阅读过的标注和修改。

# Tools
## wolframalpha
[wolframalpha](http://www.wolframalpha.com/)

    [wolf 画图简介]http://reference.wolfram.com/language/tutorial/BasicPlotting.zh.html

examples:

* 画出loge 为底的 -3 到 3范围的图, 注意 ln = loge [ref](http://www.rapidtables.com/math/algebra/Ln.htm):

    `plot loge(x) {x, -3, 3} `

    `plot f(x)=x^3/(x+1)^2 `

* 两个图画在一个上:

    `ContourPlot[x^2 + y^2 == 25 , y == -(1/2)x + 10]`


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

## chapter 4: ￼Classifying with probability theory: naive Bayes
这章主要是讲贝叶斯的应用, 判断 doc 是 offensive or non-offensive, 通过计算各个单词的概率为 offensive 和 non-offensive 的概率,
个总体上 doc 为 offensive 和 non-offensive 的概率。获得用于分类的基础数据。然后对于被测试的文章应用分词(这个地方涉及到 stop word, 处理
掉的话准确率会更高) 让后计算各个单词的概率, 并计算为 offensive 的概率和，并和训练数据的基准值进行比较, 来判断是否属于哪个分类。其中的
根据就是贝叶斯的公式:@Page 60, 但是这章和传统的贝叶斯应用还是有区别的, 传统是需要计算贝叶斯公式下面部分的。但是由于这章的实现是和训练数据
的基准值比大小，所以没有必要计算公式的下面部分。公式下面部分的应用,会计算一个概率值, 然后根据这个概率值的阀值进行判断是否属于哪个分类。具体
看下面链接部分的 `贝叶斯推断及其互联网应用（二）：过滤垃圾邮件`

关于可能的误分类:

>There are ways to bias the classifier to not make these errors, and we’ll talk about these in chapter 7.

links:

[贝叶斯推断及其互联网应用（一）：定理简介](http://www.ruanyifeng.com/blog/2011/08/bayesian_inference_part_one.html)

[贝叶斯推断及其互联网应用（二）：过滤垃圾邮件](http://www.ruanyifeng.com/blog/2011/08/bayesian_inference_part_two.html)


## chapter 5: Logistic regression (逻辑回归)
这章讲了应用, 但是关键的一个公式没有做任何解释: `weights = weights + alpha * error * dataMatrix[randIndex]`
这部分关键的信息缺失, 导致我并不能完全理解这其中的方式。中间搜索了相关材料, 包括wiki. 仍是无法看懂，主要卡在参数求解的位置了。
所以这一部分就先略过。

关键的链接写在这里:

[似然函数-ch](https://zh.wikipedia.org/wiki/%E4%BC%BC%E7%84%B6%E5%87%BD%E6%95%B0)

[Likelihood function, 似然函数-en, Recommaned](https://en.wikipedia.org/wiki/Likelihood_function)

着重看下 Log-likelihood 一节, 讲的很透彻, 具体就是 x 分布的 likehood function 依赖两个变量 alpha, beta 如何求得
beta 的 MLE(maximum-likelihood estimate)

似然函数和概率的语义差不多，区别是：
>概率用于在已知一些参数的情况下，预测接下来的观测所得到的结果，而似然性则是用于在已知某些观测所得到的结果时，对有关事物的性质的参数进行估计。

基本的意思就是在一直某些时间发生的情况下, 推断其事件影响因子参数在该事件发生的最优的情况(即最大可能)。

[Expectation–maximization algorithm](https://en.wikipedia.org/wiki/Expectation%E2%80%93maximization_algorithm)

[ ! 机器学习算法与Python实践之（七）逻辑回归（Logistic Regression）](http://blog.csdn.net/zouxy09/article/details/20319673)

[从最大似然到EM算法浅解](http://blog.csdn.net/zouxy09/article/details/8537620)

[Logistic Regression 模型简介](http://tech.meituan.com/intro_to_logistic_regression.html)

[Andrew NG: 逻辑回归笔记](http://blog.csdn.net/abcjennifer/article/details/7716281)

[Logistic regression](https://en.wikipedia.org/wiki/Logistic_regression)

logistic regression 的目标数据需是 independent variables

这章讲的内容应用于 binary Logistic regression ， 多级分类:
> Cases with more than two categories are referred to as multinomial logistic regression,
or, if the multiple categories are ordered, as ordinal logistic regression.


about optimization algorithms:
>Among the optimization algorithms, one of the most common algorithms is gradient ascent.
Gradient ascent can be simplified with stochastic gradient ascent.

* converging

>One way to look at how well the optimization algorithm is doing is to see if it’s converging. - @page 92

sigmoid 函数的作用:

随机梯度下降的作用:


## Python Snippets

`returnVec = [0]*3 # init 3 length arr filled with 0`

`set(0) | set(1) #union two set, return set(0, 1)`

## numpy

    //ones
    ones(3) -> array([ 1.,  1.,  1.])
    ones((1, 3)) -> array([[ 1.,  1.,  1.]])
    zeros(3) -> array([ 0.,  0.,  0.])


## Math
门这样的符号表示相乘的意思



## 术语

* Latent variable:

>In statistics, latent variables (from Latin: present participle of lateo (“lie hidden”),
as opposed to observable variables), are variables that are not directly observed but are rather inferred
(through a mathematical model) from other variables that are observed (directly measured).

* identically distributed:

>In probability theory and statistics, a sequence or other collection of random variables is independent and
identically distributed (i.i.d.) if each random variable has the same probability distribution as the others
and all are mutually independent.

* [奇点, singularity](https://zh.wikipedia.org/wiki/%E5%A5%87%E7%82%B9_(%E6%95%B0%E5%AD%A6)):

>在数学中，奇点（singularity）或奇点，是数学物件中无法处理的点

* [Probability density function-PDF, 概率密度函数](https://en.wikipedia.org/wiki/Probability_density_function)
指的是某个值对应的概率值

>In probability theory, a probability density function (PDF), or density of a continuous random variable,
is a function that describes the relative likelihood for this random variable to take on a given value.

* [Probability mass function-PMF](https://en.wikipedia.org/wiki/Probability_mass_function)
PMF 是指离散分布的各个值的概率分布图

>In probability theory and statistics, a probability mass function (pmf) is a function that gives the probability
that a discrete random variable is exactly equal to some value.[1] The probability mass function is often the primary
means of defining a discrete probability distribution, and such functions exist for either scalar or multivariate random
variables whose domain is discrete.

* [Parametric family](https://en.wikipedia.org/wiki/Parametric_family)
这个地方定义了一个关键的标注: fx(.;&#920;)

>In mathematics and its applications, a parametric family or a parameterized family is a family of objects
(a set of related objects) whose definitions depend on a set of parameters.

# links
[open class room: machine learning](http://openclassroom.stanford.edu/MainFolder/CoursePage.php?course=MachineLearning)

[理解矩阵乘法](http://www.ruanyifeng.com/blog/2015/09/matrix-multiplication.html)

[导数(Derivative)](https://zh.wikipedia.org/wiki/%E5%AF%BC%E6%95%B0)

[html entities](http://www.w3.org/TR/html4/sgml/entities.html)

## todos
    done:
        chapter 4:
            贝叶斯公式的意思，和这个公式和代码间的联系

    pendings:
        [what's ln means](http://betterexplained.com/articles/demystifying-the-natural-logarithm-ln/)

        chapter 5:
            [Simulated annealing](https://en.wikipedia.org/wiki/Simulated_annealing)

            sigmoid 函数的作用
            weights = weights + alpha * error * dataMatrix[randIndex] # 公式的作用

