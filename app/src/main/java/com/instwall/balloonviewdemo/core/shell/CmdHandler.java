package com.instwall.balloonviewdemo.core.shell;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.instwall.balloonviewdemo.core.shell.SimpleRemoteShell.RunContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class CmdHandler {
    public static final String ERROR_PRE = "Error:";
    public final String cmd;
    public final String desc;
    public final String[] helps;
    public final boolean showInHelp;
    String mDomain;

    /**
     * Cmd handler for player:xxx cmd.
     * <p>
     * If your cmd has more than one params, your cmd will be like this:
     * <p>
     * player:myCmd:param1#param2#param...
     * <p>
     * They are split by ' '
     *
     * @param cmd  cmd name, eg: screenInfo
     * @param desc cmd desc, eg: "player:screenInfo: Get current screen info.". This message
     *             will return in '--help' result.
     */
    protected CmdHandler(String cmd, String desc, String... helps) {
        this.cmd = cmd;
        this.desc = desc;
        this.helps = helps;
        this.showInHelp = true;
    }

    protected CmdHandler(String cmd, String desc, boolean showInHelp, String... helps) {
        this.cmd = cmd;
        this.desc = desc;
        this.showInHelp = showInHelp;
        this.helps = helps;
    }

    void setupDomain(String domain) {
        mDomain = domain;
    }

    protected void postHelp(RunContext context) {
        context.postRst(getHelp());
    }

    public String getDesc(@Nullable StringBuilder fillin) {
        if (fillin == null) fillin = new StringBuilder();
        fillin.append(mDomain).append(':').append(cmd).append(": ");
        if (TextUtils.isEmpty(desc)) {
            fillin.append(": <NO DESC>");
        } else {
            fillin.append(desc);
        }
        return fillin.toString();
    }

    public String getHelp() {
        StringBuilder sb = new StringBuilder();
        getDesc(sb);
        if (helps != null) {
            for (String h : helps) {
                sb.append('\n').append(h);
            }
        }
        return sb.toString();
    }


    /**
     * Handle remote debug cmd.
     *
     * @param context remote context, you may need post run result vir context.postRst(xxx)
     * @param params  cmd params,
     */
    protected abstract void handleCmd(RunContext context, String... params);

    protected void postSplitRstIfNeed(RunContext context, String rst, int splitSize) {
        if (TextUtils.isEmpty(rst)) return;
        int size = rst.length();
        if (size > splitSize) {
            String[] lines = rst.split("\n");
            StringBuilder sb = new StringBuilder();
            int packedSize = 0;
            for (String line : lines) {
                if (packedSize > splitSize) {
                    context.postRst(sb.toString());
                    sb = new StringBuilder().append('\n');
                    packedSize = 0;
                }
                sb.append(line).append('\n');
                packedSize += line.length();
            }
            if (sb.length() > 0) context.postRst(sb.toString());
        } else context.postRst(rst);
    }

    public static class Params {
        private HashMap<String, String> mParamMap;
        private List<String> mNormalParams;

        public Params(String... params) {
            if (params == null || params.length == 0) return;
            String key = null;
            for (String p : params) {
                if (key != null) {
                    if (mParamMap == null) mParamMap = new HashMap<>();
                    mParamMap.put(key, p);
                    key = null;
                    continue;
                }
                if (p.startsWith("-")) {
                    key = p;
                } else {
                    if (mNormalParams == null) mNormalParams = new ArrayList<>();
                    mNormalParams.add(p);
                }
            }
        }

        public int getInt(String key, int defaultValue) {
            if (mParamMap == null || key == null) return defaultValue;
            String v = mParamMap.get(key);
            if (v == null) return defaultValue;
            try {
                return Integer.valueOf(v);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public String getString(String key, String defaultValue) {
            if (mParamMap == null || key == null) return defaultValue;
            String v = mParamMap.get(key);
            if (v == null) return defaultValue;
            return v;
        }

        public boolean hasParam(String key) {
            return mNormalParams != null && mNormalParams.contains(key);
        }
    }
}

