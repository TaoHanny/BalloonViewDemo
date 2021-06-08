package com.instwall.balloonviewdemo.core.shell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ConditionVariable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.BinderThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.instwall.im.ImClient;
import com.instwall.im.ImMsgInterupter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import ashy.earl.common.app.App;
import ashy.earl.common.app.Module;
import ashy.earl.common.closure.EarlCall;
import ashy.earl.common.closure.Method1_0;
import ashy.earl.common.closure.Params1;
import ashy.earl.common.task.MessageLoop;
import ashy.earl.common.task.Task;
import ashy.earl.common.util.Util;

import static ashy.earl.common.closure.Earl.bind;

public class SimpleRemoteShell extends Module {
    private static final String TAG = "shell";
    private static final String ACTION_QUERY_PLAYER_CMD = "ashy.earl.queryPlayerCmdDesc";
    private static final String ACTION_QUERY_SCREEN_ID = "ashy.earl.queryScreenId";
    private static final String ACTION_PLAYER_CMD_DESC = "ashy.earl.playerCmdHandlerDesc";
    private static final String ACTION_SCREEN_ID_RST = "ashy.earl.screenIdRst";
    private static final String SHELL_START = "balloonshell:";
    private static final String HARDWARE_START = "balloon:";
    private static final String HELP = "--help";
    private static final String FROM_ADB = "from-adb";
    //
    private static final int MAX_SHELL_RST_SIZE = 10 * 1024;//10kb
    private static final int SINGLE_SHELL_RST_SIZE = 1024;
    private static final int MAX_SHELL_WAIT_TIME = 10 * 1000;//10s
    private static com.instwall.balloonviewdemo.core.shell.SimpleRemoteShell sSelf;
    private String mHelpContext;
    private final HashMap<String, com.instwall.balloonviewdemo.core.shell.CmdHandler> mEarlCmdHandlers = new HashMap<>();
    private final MessageLoop mMainLoop = App.getMainLoop();
    private MessageLoop mCmdHandleLoop;
    private ImMsgInterupter mImMsgInterupter = new ImMsgInterupter() {
        @Override
        public boolean handleMsg(String from, String msg) {
            return newMessage(from, msg);
        }
    };

    /**
     * Cmd thread, this annotation method will run in background thread.
     */
    @Retention(RetentionPolicy.SOURCE)
    @WorkerThread
    public @interface CmdThread {
    }

    public static SimpleRemoteShell get() {
        if (sSelf != null) return sSelf;
        synchronized (SimpleRemoteShell.class) {
            if (sSelf == null) {
                sSelf = new SimpleRemoteShell();
            }
        }
        return sSelf;
    }

    private SimpleRemoteShell() {
        if (!"com.instwall.balloonviewdemo".equals(Util.currentProcessName())) return;
        List<CmdHandler> earlCmds = new ArrayList<>();
        // Regist earl:xxx cmd handlers.
        earlCmds.add(new KvStorageHandler());
        earlCmds.add(new LogHandler());
        for (CmdHandler handler : earlCmds) {
            handler.setupDomain("balloon");
            mEarlCmdHandlers.put(handler.cmd, handler);
        }
    }

    // FIXME We need better idea
    private long mScreenId = -1;

    synchronized long getScreenId() {
        if (mScreenId > 0) return mScreenId;
        final ConditionVariable condition = new ConditionVariable();
        App.getAppContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    condition.open();
                    App.getAppContext().unregisterReceiver(this);
                    return;
                }
                mScreenId = intent.getLongExtra("id", -1);
                condition.open();
                App.getAppContext().unregisterReceiver(this);
            }
        }, new IntentFilter(ACTION_SCREEN_ID_RST));
        Intent query = new Intent(ACTION_QUERY_SCREEN_ID);
        App.getAppContext().sendBroadcast(query);
        condition.block(2000);
        return mScreenId;
    }

    @EarlCall
    private void test(String cmd) {
        try {
            //            final java.lang.Process process = Runtime.getRuntime()
            //                                                     .exec("sh", null, new File("/system/bin/"));
            final Process process = new ProcessBuilder("sh")
                    .directory(new File("/system/bin/")).redirectErrorStream(true).start();
            cmd += "\nexit\n";
            process.getOutputStream().write(cmd.getBytes());
            App.getMainLoop().postTaskDelayed(new Task() {
                @Override
                public void run() {
                    process.destroy();
                }
            }, 1000);

            Log.e(TAG, "waitFor start ");
            InputStream is = process.getInputStream();
            InputStream error = process.getErrorStream();
            Log.e(TAG, "getInputStream:" + getString(is));
            Log.e(TAG, "getErrorStream:" + getString(error));
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "waitFor finish:" + process.exitValue());
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getString(InputStream inputStream) {
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        int length = 0;
        try {
            while ((length = inputStream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, length));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    @BinderThread
    @EarlCall
    private boolean newMessage(String from, String msg) {
        if (TextUtils.isEmpty(msg)) return false;
        try {
            if (msg.startsWith(SHELL_START)) {
                RunContext context = new RunContext(from, msg.substring(SHELL_START.length()));
                getCmdLoop().postTask(bind(shell, this, context));
                return true;
            } else if (msg.startsWith(HARDWARE_START)) {
                RunContext context = new RunContext(from, msg.substring(HARDWARE_START.length()));
                if (msg.startsWith(HARDWARE_START + "log")) {
                    getCmdLoop().postTask(bind(earl, this, context));
                } else mMainLoop.postTask(bind(earl, this, context));
                return true;
            } else if (msg.equals(HELP)) {
                new RunContext(from, null).postRst(helpContext());
                return true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    @EarlCall
    private void shell(final RunContext context) {
        String[] args = context.cmd.split(" ");
        if (args.length == 0) {
            context.postRst(CmdHandler.ERROR_PRE + "no cmd!");
            return;
        }
        long maxTime = 20000;//20s
        final String cmd;
        if ("-t".equals(args[0])) {
            if (args.length < 3) {
                context.postRst(CmdHandler.ERROR_PRE + "no cmd or max time not set!");
                return;
            }
            try {
                maxTime = Integer.valueOf(args[1]) * 1000;
            } catch (NumberFormatException e) {
                context.postRst(CmdHandler.ERROR_PRE + "Unknow max time:" + args[1]);
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            cmd = sb.toString();
        } else cmd = context.cmd;
        ShellManager shellManager = ShellManager.get();
        String countError = shellManager.maybeCountMatch();
        if (countError != null) {
            context.postRst(CmdHandler.ERROR_PRE + countError);
            return;
        }
        if (maxTime > 10 * 60 * 1000) {
            maxTime = 10 * 60 * 1000;
            context.postRst("max time too big[" + maxTime + "], adjust to:" + maxTime);
        }
        final MyByteArrayOutputStream baos = new MyByteArrayOutputStream();
        final long start = SystemClock.elapsedRealtime();
        shellManager.shell(new ShellManager.ResultListener() {
            @Override
            public void onRunFinish(boolean isTimeout, int resultCode, IOException error) {
                long useTime = SystemClock.elapsedRealtime() - start;
                String msg;
                int size = baos.size();
                if (isTimeout) {
                    msg = String
                            .format(Locale.CHINA, "run [%s] timeout(%d ms), size:%d", cmd, useTime,
                                    size);
                } else {
                    msg = String
                            .format(Locale.CHINA, "run [%s] finish(%d ms), exitCode:%d, size:%d",
                                    cmd, useTime, resultCode, size);
                }
                context.postRst(msg);
                if (size > MAX_SHELL_RST_SIZE) {
                    context.postRst(CmdHandler.ERROR_PRE + "Result too big[" + baos.size() +
                            "], you may need use earl:log " + cmd + "!");
                    return;
                }
                if (size > SINGLE_SHELL_RST_SIZE) {
                    int split = size / SINGLE_SHELL_RST_SIZE +
                            ((size % SINGLE_SHELL_RST_SIZE == 0) ? 0 : 1);
                    for (int i = 0; i < split; i++) {
                        int len = i == split - 1 ? size % SINGLE_SHELL_RST_SIZE :
                                SINGLE_SHELL_RST_SIZE;
                        context.postRst(
                                "\n" + new String(baos.getBytes(), i * SINGLE_SHELL_RST_SIZE, len));
                    }
                } else context.postRst(baos.toString());
            }
        }, maxTime, context.from, cmd, baos);
    }


    private static class MyByteArrayOutputStream extends ByteArrayOutputStream {
        public byte[] getBytes() {
            return buf;
        }
    }

    @EarlCall
    private void earl(RunContext context) {
        String earl = context.cmd;
        String cmd;
        String[] params;
        int splitIndex = earl.indexOf(' ');
        if (splitIndex == -1) {
            cmd = earl;
            params = null;
        } else {
            cmd = earl.substring(0, splitIndex);
            params = earl.substring(splitIndex + 1).split(" ");
        }
        if (TextUtils.isEmpty(cmd)) {
            context.postRst("Empty cmd for earl:xxx !");
            return;
        }
        CmdHandler cmdHandler = mEarlCmdHandlers.get(cmd);
        if (cmdHandler == null) {
            String versionName = null;
            try {
                versionName = App.getAppContext().getPackageManager()
                        .getPackageInfo(App.getPkg(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            context.postRst("No handler for earl:" + cmd +
                    "! You may need check your command, or this is a old client[" +
                    versionName + "]");
            return;
        }
        if (params != null && params.length == 1 &&
                ("-h".equals(params[0]) || "--help".equals(params[0]))) {
            context.postRst(cmdHandler.getHelp());
            return;
        }
        cmdHandler.handleCmd(context, params);
    }

    public void registHardwareCmd(@NonNull CmdHandler handler) {
        handler.setupDomain("hardware");
        CmdHandler old = mEarlCmdHandlers.put(handler.cmd, handler);
        if (old != null) {
            throw new IllegalArgumentException(
                    "cmd[" + handler.cmd + "] already regist of " + old + ", current is:" +
                            handler);
        }
        Log.e(TAG, "register cmd: " + handler.cmd);
    }

    private synchronized String helpContext() {
        if (mHelpContext != null) return mHelpContext;
        final StringBuilder sb = new StringBuilder();
        sb.append("\n-welcome to AshyEarl remote shell 1.1-\n\n")
                // shell:xxx
                .append("shell:cmd [params]\n")
                .append("  Run linux shell cmd as player user, may only have limit permission,\n")
                .append("  but it has [dump,sdcard] permission\n\n")

                // instwallshell:xxx
                .append("magicshell:cmd [params]\n")
                .append("  Run linux shell cmd as system user, need instwall shell installed,\n")
                .append("  it has all system level permission\n\n")

                // earl:xxx
                .append("earl:cmd [params]\n")
                //
                .append("  Run Earl cmd. These are system level cmd.\n");
        fillEarlCmdDesc(sb);

        // player:xxx
        sb.append("\nplayer:cmd [params]\n")
                .append("  run Player cmd. These are player's module debug cmd.\n");
        final ConditionVariable condition = new ConditionVariable();
        App.getAppContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    App.getAppContext().unregisterReceiver(this);
                    return;
                }
                String desc = intent.getStringExtra("desc");
                sb.append(desc);
                condition.open();
                App.getAppContext().unregisterReceiver(this);
            }
        }, new IntentFilter(ACTION_PLAYER_CMD_DESC));
        Intent query = new Intent(ACTION_QUERY_PLAYER_CMD);
        App.getAppContext().sendBroadcast(query);
        condition.block(2000);

        mHelpContext = sb.toString();
        return mHelpContext;
    }

    private static String getCmdHelp(CmdHandler handler, StringBuilder optSb) {
        if (optSb == null) optSb = new StringBuilder();
        if (TextUtils.isEmpty(handler.desc)) {
            optSb.append(handler.mDomain).append(handler.cmd).append(": <NO DESC>\n");
        } else {
            optSb.append(handler.desc).append('\n');
        }
        return optSb.toString();
    }

    private void fillEarlCmdDesc(StringBuilder sb) {
        int index = 1;
        for (CmdHandler handler : mEarlCmdHandlers.values()) {
            if (!handler.showInHelp) continue;
            sb.append("  ").append(index).append('.');
            handler.getDesc(sb);
            sb.append('\n');
            index += 1;
        }
    }

    @NonNull
    synchronized MessageLoop getCmdLoop() {
        if (mCmdHandleLoop != null) return mCmdHandleLoop;
        mCmdHandleLoop = MessageLoop.prepare("cmd");
        return mCmdHandleLoop;
    }

    @Override
    public void start() {
        init();
    }

    @Override
    protected void init() {
        ImClient.get().addInterrupter(mImMsgInterupter);
    }


    public static class RunContext {
        public final String from;
        public final String cmd;

        public RunContext(String from, String cmd) {
            this.from = from;
            this.cmd = cmd;
        }

        public void postRst(String rst) {
            if (TextUtils.isEmpty(rst)) return;
            if (FROM_ADB.equals(from)) {
                String[] lines = rst.split("\n");
                for (String line : lines) {
                    Log.d(TAG, "-----> " + line);
                }
                return;
            }
            ImClient.get().sendMsg(from, rst);
        }
    }

    private static final Method1_0<SimpleRemoteShell, Void, RunContext> shell = new Method1_0<SimpleRemoteShell, Void, RunContext>(
            SimpleRemoteShell.class, "shell") {
        @Override
        public Void run(SimpleRemoteShell target, @NonNull Params1<RunContext> params) {
            target.shell(params.p1);
            return null;
        }
    };
    private static final Method1_0<SimpleRemoteShell, Void, RunContext> earl = new Method1_0<SimpleRemoteShell, Void, RunContext>(
            SimpleRemoteShell.class, "earl") {
        @Override
        public Void run(SimpleRemoteShell target, @NonNull Params1<RunContext> params) {
            target.earl(params.p1);
            return null;
        }
    };
}
