package studio.uphie.zhihudaily.abs;

import android.view.View;
import android.view.View.OnClickListener;


// 这个类的实现就是模拟了一个closure, 用于储存clicked时候获取当初绑定的data数据
public abstract class AbsBaseOnItemClickListener<T> implements OnClickListener {

    private T data;

    public AbsBaseOnItemClickListener(T data) {
        this.data = data;
    }

    @Override
    public void onClick(View v) {
        onClick(v, data);
    }

    public abstract void onClick(View view, T data);
}
