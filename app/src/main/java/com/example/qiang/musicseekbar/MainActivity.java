package com.example.qiang.musicseekbar;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.example.qiang.musicseekbar.adapter.MusicAdapter;
import com.example.qiang.musicseekbar.beans.ListPos;
import com.example.qiang.musicseekbar.util.AlbumDealUtil;
import com.example.qiang.musicseekbar.util.DBUtil;

public class MainActivity extends AppCompatActivity {

    //action的定义
    public final static String ACTION_BUTTON = "ButtonClick";

    //定义一个广播
    public ButtonBroadcastReceiver bReceiver;
    //消息的标识
    private static final int NOTIFICATION_FLAG = 1;
    /**
     * 播放/暂停 按钮点击 ID
     */
    public final static int BUTTON_SMALL_PALY_ID = 2;
    private static final int BUTTON_SMALL_NEXT_ID = 3;
    public final static int BUTTON_BIG_PALY_ID = 4;
    private static final int BUTTON_BIG_NEXT_ID = 5;
    private static final int BUTTON_BIG_PREVIEW_ID = 6;

    public static boolean PLAY_BUTTON_FLAG = false;

    public final static String INTENT_BUTTONID_TAG = "ButtonId";


    private ImageButton buttonstart, buttonnext;
    private TextView opTime, edTime, bottom_title, bottom_singer;
    private ImageView bottom_img;
    public ImageButton splay, bplay;

    private ImageButton small_play, big_play;


    private ListView mlistview;
    private SeekBar seekBar1;
    private ListPos lp = new ListPos();
    private MediaPlayer player;
    public static boolean ISPLAY = false;
    private Bitmap bm = null;
    private RelativeLayout main_layout;

    String url = null;
    String title = null;
    String images = null;
    String time = null;
    String singer = null;
    int m_position;
    int cur_position = -1;

    private MusicAdapter mAdapter;

    List<Map<String, Object>> mlist = new ArrayList<Map<String, Object>>();

    private int songIndex = 0;

    Notification myNotify = new Notification();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //定义一个新的MediaPlayer
        player = new MediaPlayer();
        //绑定界面id与activity
        findView();
        //读取配置文件以及初始化操作
        init();
        //activity中的按键控制
        setListener();
        //
        initButtonReceiver();

    }

    public void onDestroy() {
        super.onDestroy();
        //销毁程序时候的操作

        Log.i("======", "destroy");
    }

    private void init() {
        //
        try {
            player.reset();
            SharedPreferences share = getSharedPreferences("config", MODE_PRIVATE);
            //读取配置文件中的信息，url地址、曲名、歌曲时间、专辑图像显示
            url = share.getString("url", null);
            title = share.getString("title", null);
            images = share.getString("images", null);
            time = share.getString("time", null);
            singer = share.getString("artist", null);
            m_position = share.getInt("position", 0);
            notificationMethod(title, singer, images);
            //判断配置文件中是否存在URL地址
            if (url != null) {
                //如果存在，取出URL地址
                player.setDataSource(url);
                player.prepare();
                //设置歌曲名
                bottom_title.setText(title);
                bottom_singer.setText(singer);
                if (images == null) {
                    //如果不存在专辑图片，使用默认图片
                    main_layout.setBackgroundResource(R.drawable.natoli);
                    bottom_img.setImageResource(R.drawable.natoli);
                } else {
                    //存在专辑图片，使用bitmapfactory进行转换
                    AlbumDealUtil adu = new AlbumDealUtil();
                    bm = BitmapFactory.decodeFile(images);
                    Bitmap abm = bm;
                    Bitmap nbm = adu.blurBitmap(bm, MainActivity.this);
                    BitmapDrawable bmpDraw = new BitmapDrawable(nbm);
                    main_layout.setBackgroundDrawable(bmpDraw);
                    bottom_img.setImageBitmap(abm);
                }
                //设置播放时间
                edTime.setText(time);
                seekBar1.setMax(player.getDuration());

            } else {
                //Do nothing
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void findView() {
        //绑定
        buttonstart = (ImageButton) findViewById(R.id.play_button);
        buttonnext = (ImageButton) findViewById(R.id.next_music);
        opTime = (TextView) findViewById(R.id.opTime);
        edTime = (TextView) findViewById(R.id.edTime);
        main_layout = (RelativeLayout) findViewById(R.id.background_main);
        bottom_img = (ImageView) findViewById(R.id.bottom_bar_img);
        bottom_title = (TextView) findViewById(R.id.bottom_bar_title);
        bottom_singer = (TextView) findViewById(R.id.bottom_bar_singer);

        seekBar1 = (SeekBar) findViewById(R.id.seekbar1);

        mlistview = (ListView) findViewById(R.id.music_list);
        //listview数据的读取
        mlist = DBUtil.BaseMusicList(this);
//        mlistview.setAdapter(new MusicAdapter(this, mlist));

        mAdapter = new MusicAdapter(this, mlist);
        mlistview.setAdapter(mAdapter);

    }

    //进度条的控制
    Handler handler = new Handler();
    Runnable updateThread = new Runnable() {
        public void run() {
            //获得歌曲现在播放位置并设置成播放进度条的值
            seekBar1.setProgress(player.getCurrentPosition());
            //开始时间控制
            int some = player.getCurrentPosition();
            String smthings = formatTimeFromProgress(some);
            opTime.setText(smthings);
            //每次延迟100毫秒再启动线程
            handler.postDelayed(updateThread, 100);

        }
    };

    private void setListener() {

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //播放完成后，自动切换下一首
                if (songIndex < mlist.size() - 1) {
                    songIndex = songIndex + 1;
                    songplay(songIndex);
                } else {
                    songIndex = 0;
                    songplay(songIndex);
                }

            }
        });


        //item点击事件
        mlistview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //播放点击的歌曲
                songplay(position);
            }
        });

        //长按点击事件
        mlistview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
//                发现如果长按item，会造成onItemClickListener也会被触发，这是因为onItemLongClickListener默认返回为false，而返回false会触发onItemClickListener，这时可以通过将onItemLongClickListener返回true解决。
                cur_position = position;
                lp.setList_postion(cur_position);
                mAdapter.notifyDataSetChanged();
                notificationMethod(title, singer, images);
                Log.i("+++++", "LongClick");
                return true;
            }
        });

        //播放和暂停的切换
        buttonstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PauseMusic();
//                FlagChanges();
            }
        });

        buttonnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NextMusic(player);
            }
        });

        //seekbar的控制
        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // fromUser判断是用户改变的滑块的值
//                if (fromUser == true) {
//                    player.seekTo(progress);
//                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //放下滑块之后的操作
                int dest = seekBar.getProgress();
                player.seekTo(dest);
            }
        });
    }

    //监控按键
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //监控返回键
        int nps = ListPos.getList_postion();
        if (nps != -1) {
            nps = -1;
            lp.setList_postion(nps);
            mAdapter.notifyDataSetChanged();
        } else {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                exitBy2Click();      //调用双击退出函数
            }
        }
        return false;
    }

    private static Boolean isExit = false;

    private void exitBy2Click() {
        Timer tExit = null;
        if (isExit == false) {
            isExit = true; // 准备退出
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false; // 取消退出
                }
            }, 2000); // 如果2秒钟内没有按下返回键，则启动定时器取消掉刚才执行的任务

        } else {
            finish();
            System.exit(0);
        }
    }

    public void PauseMusic() {
        if (ISPLAY == false) {
            player.start();
            //启动
            handler.post(updateThread);
            buttonstart.setBackgroundResource(R.drawable.music_pause);
            PLAY_BUTTON_FLAG = true;
            notificationMethod(title, singer, images);
            ISPLAY = true;

            //打开notification
//                    notificationMethod();
        } else if (ISPLAY == true) {
            player.pause();
            PLAY_BUTTON_FLAG = true;
            notificationMethod(title, singer, images);
            buttonstart.setBackgroundResource(R.drawable.music_play);
            ISPLAY = false;

        }

    }

    public void NextMusic(MediaPlayer mp) {
        //播放完成后，自动切换下一首
        if (songIndex < mlist.size() - 1) {
            songIndex = songIndex + 1;
            songplay(songIndex);
        } else {
            songIndex = 0;
            songplay(songIndex);
        }
    }

    public void PreviewMusic() {
        //do last music
    }

    //每次播放时候的操作
    private void songplay(int position) {

        songIndex = position;
        //读取数据库中歌曲的信息
        HashMap<String, Object> geturl = (HashMap<String, Object>) mlist.get(position);
        String murl = geturl.get("url").toString();
        String back_img = geturl.get("images").toString();
        String mtitle = geturl.get("title").toString();
        String martist = geturl.get("artist").toString();
        String mtime = geturl.get("time").toString();
        //每次播放更新配置表中的配置数据
        SharedPreferences share = getSharedPreferences("config", MODE_PRIVATE);
        SharedPreferences.Editor edit = share.edit(); //编辑文件
        edit.putString("url", murl);
        edit.putString("title", mtitle);
        edit.putString("images", back_img);
        edit.putString("time", mtime);
        edit.putInt("position", position);
        edit.commit();  //保存数据信息

        notificationMethod(mtitle, martist, back_img);

        //重置MediaPlayer状态
        player.reset();
        try {
            player.setDataSource(murl);
            player.prepare();
            bottom_title.setText(mtitle);
            bottom_singer.setText(martist);
            Log.i(back_img, "++++++");
            if (back_img == "default") {
                main_layout.setBackgroundResource(R.drawable.natoli);
                bottom_img.setImageResource(R.drawable.natoli);
            } else {
                AlbumDealUtil adu = new AlbumDealUtil();
                bm = BitmapFactory.decodeFile(back_img);
                Bitmap abm = bm;
                Bitmap nbm = adu.blurBitmap(bm, MainActivity.this);
                BitmapDrawable bmpDraw = new BitmapDrawable(nbm);
                main_layout.setBackgroundDrawable(bmpDraw);
                bottom_img.setImageBitmap(abm);
            }


            //
            int time = player.getDuration();
            String edtime = formatTimeFromProgress(time);
            edTime.setText(edtime);

            player.start();
            //
            handler.post(updateThread);
            buttonstart.setBackgroundResource(R.drawable.music_pause);
            ISPLAY = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        seekBar1.setMax(player.getDuration());

    }

    private String formatTimeFromProgress(int progress) {
        // 总的秒数
        int msecTotal = progress / 1000;
        int min = msecTotal / 60;
        int msec = msecTotal % 60;
        String minStr = min < 10 ? "0" + min : "" + min;
        String msecStr = msec < 10 ? "0" + msec : "" + msec;
        return minStr + ":" + msecStr;
    }

    //右上角小menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.info, menu);

        return true;
    }

    //右上角小menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.quit1:
                //设置listview数据源，并自定义ListView
//                mlist.clear();
//                mlist.addAll(DBUtil.BaseMusicList(this));

                mlist = DBUtil.musicrs(this, this);
//                mlistview.setAdapter(new MusicAdapter(this, mlist));

                mAdapter = new MusicAdapter(this, mlist);
                mlistview.setAdapter(mAdapter);
//                mAdapter.notifyDataSetChanged();
                //数据库操作
                return true;
            default:
                return false;
        }
    }

    public void notificationMethod(String mtitle, String martist, String mimg) {
        // 在Android进行通知处理，首先需要重系统哪里获得通知管理器NotificationManager，它是一个系统Service。
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        myNotify.icon = R.drawable.natoli;
        myNotify.tickerText = "开始播放";
        myNotify.when = System.currentTimeMillis();
        myNotify.flags = Notification.FLAG_NO_CLEAR;// 不能够自动清除
        myNotify.priority = Notification.PRIORITY_MAX;//设置优先级
        //自定义消息框
        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.my_notification);
        RemoteViews rv_big = new RemoteViews(getPackageName(), R.layout.my_big_notification);
        //分别绑定大小消息框
        myNotify.contentView = rv;
        myNotify.bigContentView = rv_big;
        //默认notification
        rv.setTextViewText(R.id.no_small_title, mtitle);
        rv.setTextViewText(R.id.no_small_singer, martist);
        //扩大版的notification
        rv_big.setTextViewText(R.id.no_big_title, mtitle);
        rv_big.setTextViewText(R.id.no_big_singer, martist);
        //获取图片
        if (mimg == "default") {
            rv.setImageViewResource(R.id.no_small_img, R.drawable.natoli);
            rv_big.setImageViewResource(R.id.no_small_img, R.drawable.natoli);
        } else if (mimg != null) {
            bm = BitmapFactory.decodeFile(mimg);
            rv.setImageViewBitmap(R.id.no_small_img, bm);
            rv_big.setImageViewBitmap(R.id.no_big_img, bm);
        }


        Intent buttonplay = new Intent(ACTION_BUTTON);

        buttonplay.putExtra(INTENT_BUTTONID_TAG, BUTTON_BIG_PALY_ID);
        //这里加了广播，所及INTENT的必须用getBroadcast方法
        PendingIntent intent_big_play = PendingIntent.getBroadcast(this, BUTTON_BIG_PALY_ID, buttonplay, PendingIntent.FLAG_UPDATE_CURRENT);
        rv_big.setOnClickPendingIntent(R.id.no_big_play, intent_big_play);

        buttonplay.putExtra(INTENT_BUTTONID_TAG, BUTTON_BIG_NEXT_ID);
        PendingIntent intent_big_next = PendingIntent.getBroadcast(this, BUTTON_BIG_NEXT_ID, buttonplay, PendingIntent.FLAG_UPDATE_CURRENT);
        rv_big.setOnClickPendingIntent(R.id.no_big_next, intent_big_next);

        buttonplay.putExtra(INTENT_BUTTONID_TAG, BUTTON_BIG_PREVIEW_ID);
        PendingIntent intent_big_prev = PendingIntent.getBroadcast(this, BUTTON_BIG_PREVIEW_ID, buttonplay, PendingIntent.FLAG_UPDATE_CURRENT);
        rv_big.setOnClickPendingIntent(R.id.no_big_preview, intent_big_prev);

//        Intent intentnext = new Intent("next");
        buttonplay.putExtra(INTENT_BUTTONID_TAG, BUTTON_SMALL_NEXT_ID);
        PendingIntent intent_small_next = PendingIntent.getBroadcast(this, BUTTON_SMALL_NEXT_ID, buttonplay, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.no_small_next, intent_small_next);

        buttonplay.putExtra(INTENT_BUTTONID_TAG, BUTTON_SMALL_PALY_ID);
        PendingIntent intent_small_play = PendingIntent.getBroadcast(this, BUTTON_SMALL_PALY_ID, buttonplay, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.no_small_play, intent_small_play);

        if (PLAY_BUTTON_FLAG == true) {
            if (ISPLAY == false) {
                rv_big.setImageViewResource(R.id.no_big_play, R.drawable.music_pause);
                ISPLAY = true;
                PLAY_BUTTON_FLAG = false;
                Log.i("========", "|||" + ISPLAY);
            } else if (ISPLAY == true) {
                rv_big.setImageViewResource(R.id.no_big_play, R.drawable.music_play);
                ISPLAY = false;
                Log.i("========", "|||" + ISPLAY);
                PLAY_BUTTON_FLAG = false;
            }
        }

        PendingIntent contentIntent_main = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        myNotify.contentIntent = contentIntent_main;

        manager.notify(NOTIFICATION_FLAG, myNotify);

    }

    public class ButtonBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_BUTTON)) {
                //通过传递过来的ID判断按钮点击属性或者通过getResultCode()获得相应点击事件
                int buttonId = intent.getIntExtra(INTENT_BUTTONID_TAG, 0);
                switch (buttonId) {
                    case BUTTON_BIG_PALY_ID:
                        //do button click action
                        PauseMusic();

                        break;
                    case BUTTON_BIG_NEXT_ID:
                        NextMusic(player);

                        break;
                    case BUTTON_BIG_PREVIEW_ID:
                        PreviewMusic();

                        break;
                    case BUTTON_SMALL_PALY_ID:
                        PauseMusic();

                        break;
                    case BUTTON_SMALL_NEXT_ID:
                        NextMusic(player);
                        break;
                    default:
                        break;
                }
            }

        }
    }


    //绑定
    public void initButtonReceiver() {
        bReceiver = new ButtonBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_BUTTON);
        registerReceiver(bReceiver, intentFilter);

    }

//    public void FlagChanges() {
//        if (PLAY_FLAG == true) {
//            player.start();
//            //启动
//            handler.post(updateThread);
//            buttonstart.setImageResource(R.drawable.music_pause);
//            splay = (ImageButton) findViewById(R.id.no_small_play);
//            bplay = (ImageButton) findViewById(R.id.no_big_play);
//            PLAY_FLAG = false;
//        } else if (PLAY_FLAG == false) {
//            PLAY_FLAG = true;
//        }
//    }

//    public void btn_NotificationFlag() {
//        if (ISPLAY == false) {
//            myNotify.bigContentView.setImageViewResource(R.id.no_big_play, R.drawable.music_pause);
//            ISPLAY = true;
//            Log.i("========", "|||" + ISPLAY);
//            notificationMethod(title, singer, images);
//        } else if (ISPLAY == true) {
//            myNotify.bigContentView.setImageViewResource(R.id.no_big_play, R.drawable.music_play);
//            ISPLAY = false;
//            Log.i("========", "|||" + ISPLAY);
//            notificationMethod(title, singer, images);
//        }
//    }


}
