package com.lzh.easythreadmanager.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.lzh.easythread.AsyncCallback;
import com.lzh.easythread.EasyThread;
import com.lzh.easythread.Callback;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity{

    EasyThread executor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        executor = EasyThread.Builder
                .fixed(2)
                .priority(Thread.MAX_PRIORITY)
                .callback(new ToastCallback())
                .build();
    }

    // 启动普通Runnable任务
    public void runnableTask(View view) {
        executor.name("Runnable task")
                .execute(new NormalTask());
    }

    public void callableTask(View view) {
        Future<User> submit = executor.name("Callable task")
                .submit(new NormalTask());

        try {
            User user = submit.get(3, TimeUnit.SECONDS);
            toast("获取到任务返回信息：%s", user);
        } catch (Exception e) {
            toast("获取Callable任务信息失败");
        }
    }

    public void asyncTask(View view) {
        executor.name("Async task")
                .async(new NormalTask(), new AsyncCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        toast("执行异步回调任务成功：%s", user);
                    }

                    @Override
                    public void onFailed(Throwable t) {
                        toast("执行异步回调任务失败：%s", t.getMessage());
                    }
                });
    }

    public void onExceptionClick(View view) {
        executor.name("un catch task")
                .execute(new UnCatchTask());
    }

    public void delayTask(View view) {
        executor.name("delay task")
                .delay(3, TimeUnit.SECONDS)
                .execute(new NormalTask());
    }

    public void switchThread(View view) {
        executor.name("switch thread task")
                .deliver(executor.getExecutor())// 回调到自身的线程池任务中
                .execute(new NormalTask());
    }

    private class NormalTask implements Runnable, Callable<User> {
        @Override
        public void run() {
            toast("正在执行Runnable任务：%s", Thread.currentThread().getName());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public User call() throws Exception {
            toast("正在执行Callable任务：%s", Thread.currentThread().getName());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new User("Haoge", "123456");
        }
    }

    private class UnCatchTask implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            throw new RuntimeException("故意的");
        }
    }

    private class LogCallback implements Callback {

        private final String TAG = "LogCallback";

        @Override
        public void onError(Thread thread, Throwable t) {
            Log.e(TAG, String.format("[任务线程%s]/[回调线程%s]执行失败: %s", thread, Thread.currentThread(), t.getMessage()), t);
        }

        @Override
        public void onCompleted(Thread thread) {
            Log.d(TAG, String.format("[任务线程%s]/[回调线程%s]执行完毕：", thread, Thread.currentThread()));
        }

        @Override
        public void onStart(Thread thread) {
            Log.d(TAG, String.format("[任务线程%s]/[回调线程%s]执行开始：", thread, Thread.currentThread()));
        }
    }

    private class ToastCallback extends LogCallback {

        @Override
        public void onError(Thread thread, Throwable t) {
            super.onError(thread, t);
            toast("线程%s运行出现异常，异常信息为：%s", thread, t.getMessage());
        }

        @Override
        public void onCompleted(Thread thread) {
            super.onCompleted(thread);
            toast("线程%s运行完毕", thread);
        }
    }

    private void toast(final String message, final Object... args) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(this, String.format(message, args), Toast.LENGTH_SHORT).show();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, String.format(message, args), Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

}
