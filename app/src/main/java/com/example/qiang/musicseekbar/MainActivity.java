package com.example.qiang.musicseekbar;

import android.annotation.TargetApi;
import android.content.Intent;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.qiang.musicseekbar.adapter.MusicAdapter;
import com.example.qiang.musicseekbar.util.AlbumDealUtil;
import com.example.qiang.musicseekbar.util.DBUtil;

public class MainActivity extends AppCompatActivity {

    private Button buttonnext, buttonlast;

    private ImageButton buttonstart;
    private TextView opTime, edTime, bottom_title;
    private ImageView bottom_img;


    private ListView mlistview;
    private SeekBar seekBar1;
    private MediaPlayer player;
    private boolean ISPLAY = false;
    private Bitmap bm = null;
    private RelativeLayout main_layout;

    public static int currentListItme = 0;


    List<Map<String, Object>> mlist = new ArrayList<Map<String, Object>>();

    private int songIndex = 0;


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


    }

    public void onDestroy() {
        super.onDestroy();
        //销毁程序时候的操作
        //指定操作的文件名称
//        SharedPreferences share = getSharedPreferences("config", MODE_PRIVATE);
//        SharedPreferences.Editor edit = share.edit(); //编辑文件
//        edit.putString("url", "natoli");
//        edit.commit();  //保存数据信息

        Log.i("======", "destroy");
    }

    private void init() {
        //
        try {
            player.reset();
            SharedPreferences share = getSharedPreferences("config", MODE_PRIVATE);
            //读取配置文件中的信息，url地址、曲名、歌曲时间、专辑图像显示
            String url = share.getString("url", null);
            String title = share.getString("title", null);
            String images = share.getString("images", null);
            String time = share.getString("time", null);
            //判断配置文件中是否存在URL地址
            if (url != null) {
                //如果存在，取出URL地址
                player.setDataSource(url);
                player.prepare();
                //设置歌曲名
                bottom_title.setText(title);
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
        opTime = (TextView) findViewById(R.id.opTime);
        edTime = (TextView) findViewById(R.id.edTime);
        main_layout = (RelativeLayout) findViewById(R.id.background_main);
        bottom_img = (ImageView) findViewById(R.id.bottom_bar_img);
        bottom_title = (TextView) findViewById(R.id.bottom_bar_title);

        seekBar1 = (SeekBar) findViewById(R.id.seekbar1);

        mlistview = (ListView) findViewById(R.id.music_list);
        //listview数据的读取
        mlist = DBUtil.BaseMusicList(this);
        mlistview.setAdapter(new MusicAdapter(this, mlist));

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
//        button1.setOnClickListener(new Button.OnClickListener() {
//            @Override
//            public void onClick(View arg0) {
//                player.start();
//                //启动
//                handler.post(updateThread);
//            }
//        });
//        button2.setOnClickListener(new Button.OnClickListener() {
//            @Override
//            public void onClick(View arg0) {
//                player.pause();
//                //取消线程
////                handler.removeCallbacks(updateThread);
//            }
//        });

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //播放完成后，自动切换下一首
                if (songIndex < mlist.size() - 1) {
                    songIndex = songIndex + 1;
                    songplay(songIndex);
                } else {
                    mlist.clear();
                    songIndex = 0;

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

        //播放和暂停的切换
        buttonstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ISPLAY == false) {
                    player.start();
                    //启动
                    handler.post(updateThread);
                    buttonstart.setBackgroundResource(R.drawable.music_pause);
                    ISPLAY = true;
                } else if (ISPLAY == true) {
                    player.pause();
                    ISPLAY = false;
                    buttonstart.setBackgroundResource(R.drawable.music_play);
                }

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
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //放下滑块之后的操作
                int dest = seekBar.getProgress();
                player.seekTo(dest);
            }
        });
    }

    //每次播放时候的操作
    private void songplay(int position) {

        songIndex = position;
        //读取数据库中歌曲的信息
        HashMap<String, Object> geturl = (HashMap<String, Object>) mlist.get(position);
        String murl = geturl.get("url").toString();
        String back_img = geturl.get("images").toString();
        String mtitle = geturl.get("title").toString();
        String mtime = geturl.get("time").toString();
        //每次播放更新配置表中的配置数据
        SharedPreferences share = getSharedPreferences("config", MODE_PRIVATE);
        SharedPreferences.Editor edit = share.edit(); //编辑文件
        edit.putString("url", murl);
        edit.putString("title", mtitle);
        edit.putString("images", back_img);
        edit.putString("time", mtime);
        edit.commit();  //保存数据信息

        //重置MediaPlayer状态
        player.reset();
        try {
            player.setDataSource(murl);
            player.prepare();
            bottom_title.setText(mtitle);
            if (back_img == null) {
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
                mlist = DBUtil.musicrs(this);
                mlistview.setAdapter(new MusicAdapter(this, mlist));
                //数据库操作
                return true;
            default:
                return false;
        }
    }


}
