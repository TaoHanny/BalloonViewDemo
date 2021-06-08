package com.instwall.balloonviewdemo.core.shell;

import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;


import com.instwall.server.base.KvStorage;
import com.instwall.balloonviewdemo.core.shell.SimpleRemoteShell.RunContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ashy.earl.common.app.App;
import ashy.earl.common.closure.EarlCall;
import ashy.earl.common.closure.Method1_0;
import ashy.earl.common.closure.Method2_0;
import ashy.earl.common.closure.Method3_0;
import ashy.earl.common.closure.Params1;
import ashy.earl.common.closure.Params2;
import ashy.earl.common.closure.Params3;
import ashy.earl.common.task.MessageLoop;

import static ashy.earl.common.closure.Earl.bind;

/**
 * KvStorage debug.
 */
class KvStorageHandler extends CmdHandler {
    private final MessageLoop mDbLoop = App.getDbLoop();
    private final KvStorage mKvStorage = KvStorage.get();

    protected KvStorageHandler() {
        super("kv", "Player kv storage debug.", "kv list: list all keys",
                "kv get key: get kv storage value of key", "kv getall: get all values",
                "kv put key value: put kv storage key-value pair");
    }


    @Override
    @MainThread
    protected void handleCmd(RunContext context, String... params) {
        if (params == null || params.length < 1) {
            postHelp(context);
            return;
        }
        if ("list".equals(params[0])) {
            mDbLoop.postTask(bind(listKeys, this, context));
        } else if ("get".equals(params[0])) {
            if (params.length != 2) {
                postHelp(context);
                return;
            }
            if (TextUtils.isEmpty(params[1])) {
                context.postRst("Empty key!");
                return;
            }
            mDbLoop.postTask(bind(get, this, context, params[1]));
        } else if ("getall".equals(params[0])) {
            mDbLoop.postTask(bind(getall, this, context));
        } else if ("put".equals(params[0])) {
            if (params.length != 3) {
                postHelp(context);
                return;
            }
            if (TextUtils.isEmpty(params[1])) {
                context.postRst("Empty key!");
                return;
            }
            mDbLoop.postTask(bind(put, this, context, params[1], params[2]));
        }
    }

    @EarlCall
    private void put(RunContext context, String key, String value) {
        boolean rst = mKvStorage.save(key, value);
        context.postRst("put " + key + " : " + value + " " + (rst ? "succeed" : "failed"));
    }

    @EarlCall
    private void getall(RunContext context) {
        HashMap<String, String> kvs = mKvStorage.readKeyValues();
        if (kvs == null || kvs.isEmpty()) {
            context.postRst("Empty key:values");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Kv storage kvs[").append(kvs.size()).append(" items]\n");
        for (Map.Entry<String, String> entry : kvs.entrySet()) {
            sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append('\n');
        }
        context.postRst(sb.toString());
    }

    @EarlCall
    private void get(RunContext context, String key) {
        String value = mKvStorage.read(key);
        if (TextUtils.isEmpty(value)) {
            context.postRst("key[" + key + "] value:null!");
            return;
        }
        context.postRst(value);
    }

    @EarlCall
    private void listKeys(RunContext context) {
        Set<String> keys = mKvStorage.readKeys();
        if (keys == null || keys.isEmpty()) {
            context.postRst("Empty keys");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Kv storage keys[").append(keys.size()).append(" items]\n");
        List<String> keyList = new ArrayList<>(keys);
        Collections.sort(keyList);
        for (String key : keyList) {
            sb.append(key).append('\n');
        }
        context.postRst(sb.toString());
    }

    private static final Method3_0<KvStorageHandler, Void, RunContext, String, String> put = new Method3_0<KvStorageHandler, Void, RunContext, String, String>(
            KvStorageHandler.class, "put") {
        @Override
        public Void run(KvStorageHandler target,
                        @NonNull Params3<RunContext, String, String> params) {
            target.put(params.p1, params.p2, params.p3);
            return null;
        }
    };
    private static final Method1_0<KvStorageHandler, Void, RunContext> getall = new Method1_0<KvStorageHandler, Void, RunContext>(
            KvStorageHandler.class, "getall") {
        @Override
        public Void run(KvStorageHandler target, @NonNull Params1<RunContext> params) {
            target.getall(params.p1);
            return null;
        }
    };
    private static final Method2_0<KvStorageHandler, Void, RunContext, String> get = new Method2_0<KvStorageHandler, Void, RunContext, String>(
            KvStorageHandler.class, "get") {
        @Override
        public Void run(KvStorageHandler target, @NonNull Params2<RunContext, String> params) {
            target.get(params.p1, params.p2);
            return null;
        }
    };
    private static final Method1_0<KvStorageHandler, Void, RunContext> listKeys = new Method1_0<KvStorageHandler, Void, RunContext>(
            KvStorageHandler.class, "listKeys") {
        @Override
        public Void run(KvStorageHandler target, @NonNull Params1<RunContext> params) {
            target.listKeys(params.p1);
            return null;
        }
    };
}