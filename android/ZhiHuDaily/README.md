# ZhiHuDaily
---
[original repo](https://github.com/Uphie/ZhiHuDaily)

fresco: 图片库

网络异步请求
  https://github.com/loopj/android-async-http

注意App.java的使用
    CrashHandler

AbsBaseOnItemClickListener
  模拟了一个closure

HomeFragment
  这个实现需要特别注意下, TopStory 的类的声明并没有放到java典型的PO中



HomeFragment:SwipeRefreshLayout
  lv_stories:ListView
    setHeaderView()
      layout_home_banner:FrameLayout
        viewPager: ViewPager
        group_dots: LinearLayout
    if type == label
      list_item_story_label: TextView
    else type == news
      list_item_story: FrameLayout ({padding: 0 10dp})
        RelativeLayout
          FrameLayout
            TextView
            TextView
          FrameLayout
            facebook.SimpleDraweeView


  


view.setTag 的用法
  AbsBaseAdapter
    convertView.setTag(holder);

  HomeFragment
    convertView.setTag(labelHolder);//创建一个viewHodler然后setTag用来缓存view内的各个组件
    @getView//的写法




todos:
  android themes
  友盟插件的使用
    AbsBaseActivity 中 onPause onResume

  AbsBaseAdapter
    convertView.setTag(holder);

  private SparseArray<View> views = new SparseArray<View>();
  
  public abstract View getItemView(int position, View convertView,//这个方法的使用
  http://www.rogerblog.cn/2016/03/17/android-proess/

  android.os.Looper:
    http://developer.android.com/training/multiple-threads/communicate-ui.html
    http://www.cnblogs.com/codingmyworld/archive/2011/09/12/2174255.html


  android:layout_height="?attr/actionBarSize"

  styles.xml

  android 布局
    <android.support.v4.view.ViewPager
    android.support.v4.widget.SwipeRefreshLayout // 这是轮播图片？
    android:foreground="@drawable/img_shelter"
    android:scaleType="centerCrop"
    android:layout_gravity="bottom"
    android:textSize="18sp"

  values-v21: 代表 sdk version >21?


  HomeFragment # others mark with `:todo`
    sendEmptyMessageDelayed
    layoutRefresh.setRefreshing(false);
    android.widget.AbsListView


  ！将这个项目用scala重写掉（这个月的任务）or ReactNative
    scala 重写的挑战:
      如何将mutable variable尽量换成immutable的
      scala 库有没有足够的优势和能力写view层
        组件的复用
        样式
        第三方库的支持(facebook fresco )


    ReactNative 的挑战
      动画如何处理
      ReactNative 如何和 Native 自由交互, 互相启动对方的View(Activity)
      theme如何处理
      crashHandler又是如何处理






-----------
>非官方版知乎日报实现

这个项目开始是因为2月底时一家公司给的一道测试题目，加之想尝试新的App交互设计和陌生的Android新特性及设计，于是有了这个。

**项目中的Api来源于网络及后期的非正常渠道获取，如有侵权请告知。**

部分功能需要匿名登录或第三方登录，因此未给与实现或仅效果实现。知乎日报的交互很优秀，做的过程中学到很多，部分逆向分析了官方应用，图标资源取自官方应用，尽可能的去还原了原应用，部分交互效果暂未实现，欢迎技术交流。

引用的库
----------

 * [butterknife](https://github.com/JakeWharton/butterknife)
 * gson
 * [android-async-http](https://github.com/loopj/android-async-http)
 * [fresco](https://github.com/facebook/fresco)

第三方服务
-----------

* 友盟统计Sdk
* 友盟自动更新Sdk

预览
-----------
由于Gif截取遇到问题，于是改为短视频，[点此播放](http://www.meipai.com/media/489977367)。

* 二维码下载

	![download qrcode](https://o1wjx1evz.qnssl.com/app/qrcode/WlZE)
* [链接下载](https://www.pgyer.com/WlZE)

版本
----------
注：建议使用最新代码编译后再预览，代码会不时小幅更新。

* 2016-03-15。

联系我
------------
* 邮箱: uphie7@gmail.com
* 微博: http://weibo.com/u/5737193521
