package com.instwall.balloonviewdemo.core.shell;

import android.os.Debug;
import android.text.TextUtils;

import androidx.annotation.NonNull;


import com.instwall.net.ErrorHandler;
import com.instwall.net.NetCore;
import com.instwall.net.NetCoreException;
import com.instwall.net.ResultParser;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ashy.earl.common.app.App;
import ashy.earl.common.closure.EarlCall;
import ashy.earl.common.closure.Method0_0;
import ashy.earl.common.closure.Params0;
import ashy.earl.common.task.Job;
import ashy.earl.common.task.MessageLoop;
import ashy.earl.common.task.Task;
import ashy.earl.common.util.IoUtil;
import ashy.earl.common.util.L;
import ashy.earl.common.util.Util;
import com.instwall.balloonviewdemo.core.shell.SimpleRemoteShell.RunContext;
import com.instwall.screen.ScreenClient;

import static ashy.earl.common.closure.Earl.bind;

public class LogHandler extends CmdHandler {
    private static final String TAG = "LogHandler";
    private LogJob mLogJob;

    protected LogHandler() {
        super("log", "Android logcat & module file log",
                "log -android -player -magic -anr -unstable -all/ abort / <shell cmd>",
                "-android: include Android Logcat log", "-player: include Player file logs",
                "-magic: include MagicShell log", "-anr: include system ANR log",
                "-unstable: include unstable module log", "log abort: abort current log collection",
                "log [-s] logcat -v threadtime: execute shell long output cmd, and upload, " +
                        "these cmd must run within 20s, or this cmd will be killed!",
                "  For logcat '-d' will auto added", "  -s: run cmd as system user");
    }

    @Override
    protected void handleCmd(RunContext context, String... params) {
        if (params == null || params.length == 0) {
            postHelp(context);
            return;
        }
        if (params.length == 1 && "abort".equals(params[0])) {
            if (mLogJob == null) context.postRst(ERROR_PRE + "No log job!");
            else {
                mLogJob.cancel();
                mLogJob.mContext.postRst(ERROR_PRE + "Canceled by " + context.from);
                context.postRst("Log job canceled, old:" + mLogJob.mContext.from);
                mLogJob = null;
            }
            return;
        }
        if (mLogJob != null) {
            context.postRst(ERROR_PRE + "Log job running[" + mLogJob.mContext.from +
                    "], you need abort it!");
            return;
        }
        LogConfig logConfig = new LogConfig();
        boolean hasShell = false;
        boolean needTimeout = false;
        StringBuilder sb = null;
        for (String p : params) {
            if (TextUtils.isEmpty(p)) continue;
            if (!hasShell) {
                if (needTimeout) {
                    try {
                        logConfig.cmdTimout = Integer.valueOf(p) * 1000;
                    } catch (NumberFormatException e) {
                        context.postRst(ERROR_PRE + "Unknow timeout:" + p);
                        return;
                    }
                    needTimeout = false;
                    continue;
                }
                if (!logConfig.includeLogcat && "-android".equals(p)) {
                    logConfig.includeLogcat = true;
                } else if (!logConfig.includeAnr && "-anr".equals(p)) {
                    logConfig.includeAnr = true;
                } else if (!logConfig.includeJavaHeap && "-heap".equals(p)) {
                    logConfig.includeJavaHeap = true;
                } else if ("-all".equals(p)) {
                    logConfig.includeLogcat = true;
                    logConfig.includeAnr = true;
                } else if ("-t".equals(p)) {
                    needTimeout = true;
                } else if (p.startsWith("-")) {
                    context.postRst(ERROR_PRE + "Unknow cmd:" + p);
                    postHelp(context);
                    return;
                } else {
                    if (needTimeout) {
                        context.postRst(ERROR_PRE + "Unknow timeout:" + p);
                        return;
                    }
                    hasShell = true;
                    sb = new StringBuilder();
                    sb.append(p).append(' ');
                    if ("logcat".equals(p)) sb.append("-d ");
                }
            } else {
                sb.append(p).append(' ');
                if ("logcat".equals(p)) sb.append("-d ");
            }
        }
        if (logConfig.cmdTimout <= 0) logConfig.cmdTimout = 10000;//10s
        logConfig.shellCmd = sb == null ? null : sb.toString();
        mLogJob = new LogJob(context, logConfig, this);
        mLogJob.start();
    }

    void jobFinished(LogJob job) {
        if (mLogJob == job) mLogJob = null;
    }

    public static boolean copy(File from, File to) throws IOException {
        if (!from.exists()) return false;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(from);
            if (!to.exists() && !to.createNewFile()) return false;
            fos = new FileOutputStream(to);
            byte[] buffer = new byte[8192];
            int read = -1;
            while ((read = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            return true;
        } finally {
            IoUtil.closeQuitly(fis);
            IoUtil.closeQuitly(fos);
        }
    }

    public static File zipFiles(File zipTo, File... files) {
        byte[] buffer = new byte[8 * 1024];// 8k
        int length = -1;
        if (files.length == 0) return zipTo;
        // WTF, in skyworth device, the dir is NOT create automatic.
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipTo));
            zos.setLevel(9);
            for (File f : files) {
                FileInputStream fis = null;
                ZipEntry zipEntry = new ZipEntry(f.getName());
                try {
                    zos.putNextEntry(zipEntry);
                    fis = new FileInputStream(f);
                    while ((length = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IoUtil.closeQuitly(fis);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            IoUtil.closeQuitly(zos);
        }
        return zipTo;
    }

    public static String upload(File zipedFile) throws Exception {
        Map<String, String> httpPostParam = new HashMap<>();
        httpPostParam.put("upl_app", "ds_player");
        httpPostParam
                .put("screen_id", String.valueOf(ScreenClient.get().getScreenInfoSynced(0).id));
        httpPostParam.put("caption", "caption");
        httpPostParam.put("log_type", "monitor_log");
        httpPostParam.put("create_time", String.valueOf(System.currentTimeMillis() / 1000));
        //                NetworkTagger.tagThread(CrashManager.class, NetworkTagger.TAG_API_MAX);
        NetCore netCore = NetCore.get();
        ResultParser<String> parser = new ResultParser.JSONResultParser<String>() {
            @Override
            protected String parse(@NonNull JSONObject json) throws Throwable {
                String url = json.optString("file_url");
                if (!TextUtils.isEmpty(url)) return url;
                return json.toString();
            }
        };
        try {
            return "Log upload succeed: " +
                    netCore.requestMediaApi("GC", "upload_ds_log", "/geo/upload_ds_log",
                            httpPostParam, "userfile", zipedFile.getAbsoluteFile(),
                            NetCore.probeMediaType(zipedFile), parser,
                            ErrorHandler.DEFALT_NODE_ERROR);
        } catch (NetCoreException e) {
            if (e.type == NetCoreException.TYPE_CLIENT && e.rawResponse != null &&
                    e.code == NetCoreException.CODE_IMPL_BUG && "No 'data' key!".equals(e.msg)) {
                JSONObject rst = new JSONObject(e.rawResponse);
                String url = rst.optString("file_url");
                return "Log upload succeed: " + url;
            }
            throw e;
        }
        //                JSONObject jsonObject = new JSONObject(responseStr);
        //                uploadSucceed = "ok".equals(jsonObject.getString("rst"));
    }

    private static final class LogConfig {
        boolean includeLogcat;
        boolean includeAnr;
        boolean includeJavaHeap;
        String shellCmd;
        int cmdTimout;
    }

    private static final class LogJob extends Job {
        private static final String TAG = "LogJob";
        private final RunContext mContext;
        private final LogConfig mLogConfig;
        private final File mDir;
        private final LogHandler mLogHandler;
        private int mWaitOperations;
        private ShellToFile mLogcatShell;
        private ShellToFile mCmdShell;
        private static final FilenameFilter LOG_FILTER = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.startsWith("log_");
            }
        };

        public LogJob(RunContext context, LogConfig config, LogHandler logHandler) {
            super("log");
            mContext = context;
            mLogConfig = config;
            mLogHandler = logHandler;
            mDir = new File(App.getAppContext().getFilesDir(), "logTemp");
        }

        @Override
        protected void onStart() {
            addMark("start");
            // Create dir first.
            if (!mDir.exists() && !mDir.mkdirs()) {
                L.e(TAG, "%s~ onStart, can't create dir: %s", TAG, mDir);
                mContext.postRst(CmdHandler.ERROR_PRE + "Can't create dir:" + mDir);
                mLogHandler.jobFinished(this);
                return;
            }
            // Clean dir
            File[] files = mDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.delete()) {
                        L.e(TAG, "%s~ onStart, clean up failed, can't delete:%s", TAG, f);
                        mContext.postRst(CmdHandler.ERROR_PRE + "Can't delete:" + f);
                        mLogHandler.jobFinished(this);
                        return;
                    }
                }
            }
            addMark("log-dir-clean");
            StringBuilder sb = new StringBuilder();
            sb.append("Collecting ");
            if (mLogConfig.includeLogcat) sb.append("logcat, ");
            if (mLogConfig.includeAnr) sb.append("anr, ");
            sb.delete(sb.length() - 2, sb.length() - 1);
            sb.append("logs...");
            mContext.postRst(sb.toString());
            if (mLogConfig.includeLogcat) {
                mWaitOperations += 1;
                File saveTo = new File(mDir, "log_logcat.txt");
                mLogcatShell = new ShellToFile("logcat -v threadtime -d", 5000,
                        mContext.from, saveTo);
                mLogcatShell.start(new ShellManager.ResultListener() {
                    @Override
                    public void onRunFinish(boolean isTimeout, int resultCode, IOException error) {
                        if (isFinished()) return;
                        mWaitOperations -= 1;
                        addMark("collect-logcat-finish");
                        finishCollection();
                    }
                });
                //
                File dir = App.getAppContext().getFilesDir();
                File[] logs = dir.listFiles(LOG_FILTER);
                if (logs != null) {
                    for (File log : logs) {
                        try {
                            copy(log, new File(mDir, log.getName()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                addMark("copy-player-logs-finish");
            }
            if (!TextUtils.isEmpty(mLogConfig.shellCmd)) {
                mWaitOperations += 1;
                File saveTo = new File(mDir, "log_cmd.txt");
                mCmdShell = new ShellToFile(mLogConfig.shellCmd, mLogConfig.cmdTimout,
                        mContext.from, saveTo);
                mCmdShell.start(new ShellManager.ResultListener() {
                    @Override
                    public void onRunFinish(boolean isTimeout, int resultCode, IOException error) {
                        if (isFinished()) return;
                        mWaitOperations -= 1;
                        addMark("run-cmd-finish-" + mLogConfig.shellCmd);
                        finishCollection();
                    }
                });
            }
            if (mLogConfig.includeAnr) {
                try {
                    copy(new File("/data/anr/traces.txt"), new File(mDir, "log_anr.txt"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                addMark("copy-anr-finish");
            }
            if (mLogConfig.includeJavaHeap) {
                File dumpTo = new File(mDir, "dump_heap.hprof");
                try {
                    Debug.dumpHprofData(dumpTo.getAbsolutePath());
                    addMark("dump-heap-finish");
                } catch (IOException e) {
                    e.printStackTrace();
                    addMark("dump-heap-error-" + e);
                }
                // Async dump
                //                IActivityManager am = null;
                //                am .dumpHeap()
            }
            finishCollection();
        }

        private void finishCollection() {
            if (mWaitOperations > 0) return;
            File zipFile = new File(mDir, "log.zip");
            File zipedFile = zipFiles(zipFile, mDir.listFiles());
            addMark("zip-file-finish");
            if (!zipedFile.exists() || zipedFile.length() == 0) {
                mContext.postRst(CmdHandler.ERROR_PRE + "No content ziped, abort!");
                finishWithError("no-content-ziped");
                mLogHandler.jobFinished(this);
                return;
            }
            finishWithOk("zip-ok-" + Util.getHumanSize(zipedFile.length()));
            mContext.postRst("Collect finish: " + Util.getHumanSize(zipedFile.length()));
            try {
                String rst = upload(zipedFile);
                mContext.postRst(rst);
            } catch (Exception e) {
                e.printStackTrace();
                mContext.postRst(CmdHandler.ERROR_PRE + e);
            }
            cleanup();
            mLogHandler.jobFinished(this);
        }

        void cancel() {
            if (isFinished()) return;
            finishWithCancel("cancel");
            cleanup();
            mLogHandler.jobFinished(this);
        }

        private void cleanup() {
            if (!mDir.exists()) return;
            File[] files = mDir.listFiles();
            for (File f : files) {
                if (!f.delete()) {
                    L.e(TAG, "%s~ cleanup, can't delete %s", TAG, f);
                }
            }
            if (mLogcatShell != null) {
                mLogcatShell.abort();
                mLogcatShell = null;
            }
            if (mCmdShell != null) {
                mCmdShell.abort();
                mCmdShell = null;
            }

        }


    }

    private static class ShellToFile
            implements ShellManager.ResultListener, ShellManager.ShellIdleListener {
        private final ShellManager mShellManager = ShellManager.get();
        private final String mCmd;
        private final int mMaxWait;
        private final String mFrom;
        private final File mSaveTo;
        private FileOutputStream mFos;
        private ShellManager.ResultListener mResultListener;
        private Task mWaitShellTask;
        private ShellManager.ShellJob mShellJob;

        ShellToFile(String cmd, int maxWait, String from, File saveTo) {
            mCmd = cmd;
            mMaxWait = maxWait;
            mFrom = from;
            mSaveTo = saveTo;
        }

        void start(ShellManager.ResultListener listener) {
            mResultListener = listener;
            if (mShellManager.isIdle()) {
                run();
            } else {
                mWaitShellTask = bind(waitShellTimeout, this).task();
                MessageLoop.current().postTaskDelayed(mWaitShellTask, mMaxWait);
                mShellManager.addShellIdleListener(this);
            }
        }

        void abort() {
            if (mShellJob != null) {
                mShellJob.abort();
                mShellJob = null;
            }
            if (mWaitShellTask != null) {
                mWaitShellTask.cancel();
                mWaitShellTask = null;
            }
        }

        private void run() {
            // Cancel time out task.
            if (mWaitShellTask != null) {
                mWaitShellTask.cancel();
                mWaitShellTask = null;
            }
            mShellManager.removeShellIdleListener(this);
            try {
                if (!mSaveTo.exists() && !mSaveTo.createNewFile()) {
                    onRunFinish(false, -1, new IOException("can't create file:" + mSaveTo));
                    return;
                }
                mFos = new FileOutputStream(mSaveTo);
            } catch (IOException e) {
                e.printStackTrace();
                onRunFinish(false, -1, e);
                return;
            }
            mShellJob = mShellManager.shell(this, mMaxWait, mFrom, mCmd, mFos);
        }

        @Override
        public void onShellIdle(int canRunShellCount) {
            run();
        }

        @Override
        public void onRunFinish(boolean isTimeout, int resultCode, IOException error) {
            if (mResultListener == null) return;
            mResultListener.onRunFinish(isTimeout, resultCode, error);
            mResultListener = null;
            if (mWaitShellTask != null) {
                mWaitShellTask.cancel();
                mWaitShellTask = null;
            }
        }

        @EarlCall
        private void waitShellTimeout() {
            if (mResultListener == null) return;
            mWaitShellTask = null;
            onRunFinish(true, 0, null);
        }

        private static final Method0_0<ShellToFile, Void> waitShellTimeout = new Method0_0<ShellToFile, Void>(
                ShellToFile.class, "waitShellTimeout") {
            @Override
            public Void run(ShellToFile target, @NonNull Params0 params) {
                target.waitShellTimeout();
                return null;
            }
        };

    }
}

