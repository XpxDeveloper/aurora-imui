package imui.jiguang.cn.imuisample.messages;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.jiguang.imui.commons.ImageLoader;
import cn.jiguang.imui.commons.models.IMessage;
import cn.jiguang.imui.messages.ChatInputView;
import cn.jiguang.imui.messages.MsgListAdapter;
import cn.jiguang.imui.messages.RecordVoiceButton;
import imui.jiguang.cn.imuisample.R;
import imui.jiguang.cn.imuisample.models.DefaultUser;
import imui.jiguang.cn.imuisample.models.MyMessage;
import imui.jiguang.cn.imuisample.views.ChatView;

public class MessageListActivity extends Activity implements ChatView.OnKeyboardChangedListener,
        ChatView.OnSizeChangedListener {

    private MsgListAdapter<MyMessage> mAdapter;
    private Context mContext;
    private List<MyMessage> mData;
    private ChatView mChatView;
    private final int MY_REQUEST_CODE = 0x0001;
    private InputMethodManager mImm;
    private Window mWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.chat_activity);

        this.mImm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mWindow = getWindow();
        mChatView = (ChatView) findViewById(R.id.chat_view);
        mChatView.initModule();
        mData = getMessages();
        initMsgAdapter();
        mChatView.setKeyboardChangedListener(this);
        mChatView.setOnSizeChangedListener(this);
        mChatView.setMenuClickListener(new ChatInputView.OnMenuClickListener() {
            @Override
            public boolean onSubmit(CharSequence input) {
                if (input.length() == 0) {
                    return false;
                }
                mAdapter.addToStart(new MyMessage(input.toString(), IMessage.MessageType.SEND_TEXT), true);
                return true;
            }

            @Override
            public void onVoiceClick() {
                if ((ActivityCompat.checkSelfPermission(MessageListActivity.this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)) {
                    ActivityCompat.requestPermissions(MessageListActivity.this, new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO}, MY_REQUEST_CODE);
                }

                // Show record voice interface
                // 设置存放录音文件目录
                File rootDir = mContext.getFilesDir();
                String fileDir = rootDir.getAbsolutePath() + "/voice";
                mChatView.setRecordVoiceFile(fileDir, "temp_voice");
            }

            @Override
            public void onPhotoClick() {

            }

            @Override
            public void onCameraClick() {

            }
        });

        mChatView.setRecordVoiceListener(new RecordVoiceButton.RecordVoiceListener() {
            @Override
            public void onFinishRecord(File voiceFile, int duration) {
                MyMessage message = new MyMessage(null, IMessage.MessageType.SEND_VOICE);
                message.setContentFile(voiceFile.getPath());
                message.setDuration(duration);
                mAdapter.addToStart(message, true);
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_REQUEST_CODE) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Permission denied
            }
        }
    }

    private List<MyMessage> getMessages() {
        List<MyMessage> list = new ArrayList<>();
        Resources res = getResources();
        String[] messages = res.getStringArray(R.array.messages_array);
        for (int i = 0; i < 10; i++) {
            MyMessage message = new MyMessage(messages[i], i % 2 == 0 ?
                    IMessage.MessageType.RECEIVE_TEXT : IMessage.MessageType.SEND_TEXT);
            list.add(message);
        }
        return list;
    }

    private void initMsgAdapter() {
        ImageLoader imageLoader = new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, String url) {
                Picasso.with(mContext).load(url).into(imageView);
            }
        };
        MsgListAdapter.HoldersConfig holdersConfig = new MsgListAdapter.HoldersConfig();
        // Use default layout
        mAdapter = new MsgListAdapter<MyMessage>("0", holdersConfig, imageLoader);

        // If you want to customise your layout, try to create custom ViewHolder:
        // holdersConfig.setSenderTxtMsg(CustomViewHolder.class, layoutRes);
        // holdersConfig.setReceiverTxtMsg(CustomViewHolder.class, layoutRes);
        // CustomViewHolder must extends ViewHolders defined in MsgListAdapter.
        // Current ViewHolders are TxtViewHolder, VoiceViewHolder.

        mAdapter.setOnMsgClickListener(new MsgListAdapter.OnMsgClickListener<MyMessage>() {
            @Override
            public void onMessageClick(MyMessage message) {
                Toast.makeText(mContext, mContext.getString(R.string.message_click_hint),
                        Toast.LENGTH_SHORT).show();
                // do something
            }
        });

        mAdapter.setMsgLongClickListener(new MsgListAdapter.OnMsgLongClickListener<MyMessage>() {
            @Override
            public void onMessageLongClick(MyMessage message) {
                Toast.makeText(mContext, mContext.getString(R.string.message_long_click_hint),
                        Toast.LENGTH_SHORT).show();
                // do something
            }
        });

        mAdapter.setOnAvatarClickListener(new MsgListAdapter.OnAvatarClickListener<MyMessage>() {
            @Override
            public void onAvatarClick(MyMessage message) {
                DefaultUser userInfo = (DefaultUser) message.getUserInfo();
                Toast.makeText(mContext, mContext.getString(R.string.avatar_click_hint),
                        Toast.LENGTH_SHORT).show();
                // Do something
            }
        });
        mAdapter.addToStart(new MyMessage("Hello World", IMessage.MessageType.RECEIVE_TEXT), false);
        mAdapter.setOnLoadMoreListener(new MsgListAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore(int page, int totalCount) {
                if (totalCount < mData.size()) {
                    loadNextPage();
                }
            }
        });
        mChatView.setAdapter(mAdapter);
    }

    private void loadNextPage() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.addToEnd(mData, true);
            }
        }, 1000);
    }

    @Override
    public void onKeyBoardStateChanged(int state) {
        switch (state) {
            case ChatInputView.KEYBOARD_STATE_INIT:
                ChatInputView chatInputView = mChatView.getChatInputView();
                if (mImm != null) {
                    mImm.isActive();
                }
                if (mChatView.getChatInputView().getVisibility() == View.INVISIBLE
                        || (!chatInputView.getSoftInputState()
                        && mChatView.getChatInputView().getVisibility() == View.GONE)) {

                    mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mChatView.getChatInputView().setVisibility(View.GONE);
                }
                break;
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (oldh - h > 300) {
            mChatView.setMenuHeight(oldh - h);
        }
    }

}
