package com.instwall.balloonviewdemo.core.shell;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import ashy.earl.common.closure.EarlCall;
import ashy.earl.common.closure.Method0_0;
import ashy.earl.common.closure.Method1_0;
import ashy.earl.common.closure.Method2_0;
import ashy.earl.common.closure.Params0;
import ashy.earl.common.closure.Params1;
import ashy.earl.common.closure.Params2;
import ashy.earl.common.task.MessageLoop;
import ashy.earl.common.task.Task;
import ashy.earl.common.util.ModifyList;

import static ashy.earl.common.closure.Earl.bind;

class ShellManager {
    private static final int MAX_SHELL_COUNT = 3;
    private static final int IDLE_TIME = 20000;//20s
    private static ShellManager sSelf;
    private MessageLoop mShellLoop;
    private List<ShellJob> mRunningShells = new ArrayList<>();
    private ModifyList<ShellIdleListener> mShellIdleListeners = new ModifyList<>();
    private Task mCleanupTask;

    public interface ShellIdleListener {
        void onShellIdle(int canRunShellCount);
    }

    public static ShellManager get() {
        if (sSelf != null) return sSelf;
        synchronized (ShellManager.class) {
            if (sSelf == null) sSelf = new ShellManager();
        }
        return sSelf;
    }

    interface ResultListener {
        void onRunFinish(boolean isTimeout, int resultCode, IOException error);
    }

    private ShellManager() {
    }

    void addShellIdleListener(ShellIdleListener l) {
        mShellIdleListeners.add(l);
    }

    void removeShellIdleListener(ShellIdleListener l) {
        mShellIdleListeners.remove(l);
    }

    boolean isIdle() {
        return mRunningShells.size() < MAX_SHELL_COUNT;
    }

    String maybeCountMatch() {
        if (mRunningShells.size() < MAX_SHELL_COUNT) return null;
        StringBuilder sb = new StringBuilder();
        sb.append("Max shell count[").append(MAX_SHELL_COUNT)
                .append("], you need retry later,current:\n");
        for (ShellJob job : mRunningShells) {
            sb.append("  from:").append(job.mFrom).append(", cmd:").append(job.mFrom).append('\n');
        }
        return sb.toString();
    }

    ShellJob shell(ResultListener resultListener, long maxWait, String from, String cmd,
                   OutputStream saveTo) {
        if (saveTo == null) throw new NullPointerException("saveTo is null!");
        if (mRunningShells.size() >= MAX_SHELL_COUNT) return null;
        if (mShellLoop == null) mShellLoop = MessageLoop.prepare("shell", 2);
        ShellJob job = new ShellJob(mShellLoop, maxWait, resultListener, from, cmd, saveTo);
        mRunningShells.add(job);
        if (job.shell()) return job;
        return null;
    }

    void runFinish(ShellJob job) {
        mRunningShells.remove(job);
        if (mRunningShells.size() < MAX_SHELL_COUNT) {
            for (ShellIdleListener l : mShellIdleListeners) {
                int count = MAX_SHELL_COUNT - mRunningShells.size();
                if (count <= 0) return;
                l.onShellIdle(count);
            }
        }
        if (mRunningShells.isEmpty()) {
            if (mCleanupTask != null) mCleanupTask.cancel();
            mCleanupTask = bind(cleanup, this).task();
            MessageLoop.current().postTaskDelayed(mCleanupTask, IDLE_TIME);
        }
    }

    @EarlCall
    private void cleanup() {
        mCleanupTask = null;
        if (!mRunningShells.isEmpty()) return;
        mShellLoop.quit();
        mShellLoop = null;
    }

    private static final Method0_0<ShellManager, Void> cleanup = new Method0_0<ShellManager, Void>(
            ShellManager.class, "cleanup") {
        @Override
        public Void run(ShellManager target, @NonNull Params0 params) {
            target.cleanup();
            return null;
        }
    };

    static class ShellJob {
        private static final String SH_PATH = "/system/bin/sh";
        private static final File EXC_DIR = new File("/system/bin");
        private final MessageLoop mRunLoop;
        private final long mMaxTime;
        private final ShellManager.ResultListener mResultListener;
        private final MessageLoop mCreateLoop;
        private final String mFrom;
        private final String mCmd;
        private final OutputStream mSaveTo;
        private Task mTimeoutTask;
        private Process mProcess;

        private ShellJob(MessageLoop runLoop, long maxTime,
                         ShellManager.ResultListener resultListener, String from, String cmd,
                         @NonNull OutputStream saveTo) {
            mRunLoop = runLoop;
            mMaxTime = maxTime;
            mResultListener = resultListener;
            mFrom = from;
            mSaveTo = saveTo;
            mCreateLoop = MessageLoop.current();
            mCmd = cmd;
        }

        boolean shell() {
            try {
                final Process process = new ProcessBuilder(SH_PATH).directory(EXC_DIR)
                        .redirectErrorStream(
                                true).start();
                mProcess = process;
                mTimeoutTask = bind(didRunTimeout, this, process).task();
                mCreateLoop.postTaskDelayed(mTimeoutTask, mMaxTime);
                mRunLoop.postTask(bind(runShell, this, process).task());
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @EarlCall
        private void runShell(@NonNull Process process) {
            String cmd = mCmd + "\nexit\n";
            try {
                process.getOutputStream().write(cmd.getBytes());
                process.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputStream is = process.getInputStream();
            if (is == null) {
                mCreateLoop.postTask(
                        bind(didRunFinish, this, new IOException("input stream is null!"), 0)
                                .task());
                return;
            }
            // Copy stream
            byte[] buffer = new byte[1024];
            int length = 0;
            IOException error = null;
            try {
                while ((length = is.read(buffer)) != -1) {
                    mSaveTo.write(buffer, 0, length);
                }
            } catch (IOException e) {
                error = e;
            }
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int resultCode = process.exitValue();
            process.destroy();
            mCreateLoop.postTask(bind(didRunFinish, this, error, resultCode).task());
        }

        void abort() {
            if (MessageLoop.current() != mCreateLoop) {
                throw new IllegalAccessError("Wrong thread!");
            }
            // Already finished
            if (mTimeoutTask == null) return;
            mTimeoutTask.cancel();
            mTimeoutTask = null;
            if (mProcess == null) return;
            mProcess.destroy();
            ShellManager.get().runFinish(this);
        }

        @EarlCall
        private void didRunTimeout(Process process) {
            // Already finished
            if (mTimeoutTask == null) return;
            mTimeoutTask = null;
            process.destroy();
            ShellManager.get().runFinish(this);
            mResultListener.onRunFinish(true, 9, null);
        }

        @EarlCall
        private void didRunFinish(IOException error, int resultCode) {
            // Already finished
            if (mTimeoutTask == null) return;
            mTimeoutTask.cancel();
            mTimeoutTask = null;
            ShellManager.get().runFinish(this);
            mResultListener.onRunFinish(false, resultCode, error);
        }

        private static final Method1_0<ShellJob, Void, Process> runShell = new Method1_0<ShellJob, Void, Process>(
                ShellJob.class, "runShell") {
            @Override
            public Void run(ShellJob target, @NonNull Params1<Process> params) {
                target.runShell(params.p1);
                return null;
            }
        };
        private static final Method1_0<ShellJob, Void, Process> didRunTimeout = new Method1_0<ShellJob, Void, Process>(
                ShellJob.class, "didRunTimeout") {
            @Override
            public Void run(ShellJob target, @NonNull Params1<Process> params) {
                target.didRunTimeout(params.p1);
                return null;
            }
        };
        private static final Method2_0<ShellJob, Void, IOException, Integer> didRunFinish = new Method2_0<ShellJob, Void, IOException, Integer>(
                ShellJob.class, "didRunFinish") {
            @Override
            public Void run(ShellJob target, @NonNull Params2<IOException, Integer> params) {
                target.didRunFinish(params.p1, u(params.p2));
                return null;
            }
        };
    }
}
