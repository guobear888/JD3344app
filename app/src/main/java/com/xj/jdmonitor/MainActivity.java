package com.xj.jdmonitor;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String PREF = "jd_price_monitor";
    private static final String KEY = "items";
    private static final String CHANNEL_ID = "price_alert";
    private LinearLayout listBox;
    private SharedPreferences sp;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences(PREF, MODE_PRIVATE);
        createChannel();
        requestNotifyPermission();

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(18));
        scroll.addView(root);

        TextView title = tv("京东价格提醒", 24, true);
        title.setTextColor(Color.rgb(190, 30, 45));
        root.addView(title);

        TextView tip = tv("合法省钱工具：手动记录商品价格，低于目标价时提醒。不会伪装新用户、不会绕过平台规则。", 14, false);
        tip.setPadding(0, dp(8), 0, dp(12));
        root.addView(tip);

        Button add = btn("＋ 添加商品");
        root.addView(add);
        add.setOnClickListener(v -> showAddDialog(null, -1));

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listBox.setPadding(0, dp(12), 0, 0);
        root.addView(listBox);
        setContentView(scroll);
        render();
    }

    private void showAddDialog(JSONObject old, int index) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), 0);
        EditText name = input("商品名称，例如：小米手机");
        EditText url = input("京东商品链接，可选");
        EditText target = input("目标价，例如：1999");
        target.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText current = input("当前价，例如：2099");
        current.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (old != null) {
            name.setText(old.optString("name"));
            url.setText(old.optString("url"));
            target.setText(String.valueOf(old.optDouble("target", 0)));
            current.setText(String.valueOf(old.optDouble("current", 0)));
        }
        box.addView(name); box.addView(url); box.addView(target); box.addView(current);

        new AlertDialog.Builder(this)
            .setTitle(index >= 0 ? "修改商品" : "添加商品")
            .setView(box)
            .setPositiveButton("保存", (d, w) -> {
                try {
                    String n = name.getText().toString().trim();
                    if (n.length() == 0) { toast("商品名称不能为空"); return; }
                    double t = parse(target.getText().toString());
                    double c = parse(current.getText().toString());
                    JSONObject obj = new JSONObject();
                    obj.put("name", n);
                    obj.put("url", url.getText().toString().trim());
                    obj.put("target", t);
                    obj.put("current", c);
                    obj.put("updated", System.currentTimeMillis());
                    JSONArray arr = getItems();
                    if (index >= 0) arr.put(index, obj); else arr.put(obj);
                    save(arr);
                    if (c > 0 && t > 0 && c <= t) notifyPrice(n, c, t);
                    render();
                } catch (Exception e) { toast("保存失败：" + e.getMessage()); }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void render() {
        listBox.removeAllViews();
        JSONArray arr = getItems();
        if (arr.length() == 0) {
            TextView empty = tv("还没有商品。点“添加商品”，填入目标价和当前价。", 16, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, 0);
            listBox.addView(empty);
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackgroundColor(Color.rgb(248, 248, 248));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.setMargins(0, 0, 0, dp(12));
            listBox.addView(card, cp);

            double cur = o.optDouble("current", 0);
            double tar = o.optDouble("target", 0);
            TextView name = tv(o.optString("name"), 18, true);
            card.addView(name);
            TextView price = tv("当前价：¥" + money(cur) + "    目标价：¥" + money(tar), 15, false);
            card.addView(price);
            TextView status = tv(cur > 0 && tar > 0 && cur <= tar ? "✅ 已达到目标价" : "⏳ 未达到目标价", 15, true);
            status.setTextColor(cur > 0 && tar > 0 && cur <= tar ? Color.rgb(30, 130, 60) : Color.DKGRAY);
            card.addView(status);

            LinearLayout row = new LinearLayout(this);
            row.setPadding(0, dp(8), 0, 0);
            row.setGravity(Gravity.RIGHT);
            card.addView(row);
            int idx = i;
            Button edit = smallBtn("更新价格");
            Button open = smallBtn("打开链接");
            Button del = smallBtn("删除");
            row.addView(edit); row.addView(open); row.addView(del);
            edit.setOnClickListener(v -> showAddDialog(o, idx));
            open.setOnClickListener(v -> openUrl(o.optString("url")));
            del.setOnClickListener(v -> { JSONArray a = getItems(); JSONArray b = new JSONArray(); for (int k=0;k<a.length();k++) if(k!=idx) b.put(a.optJSONObject(k)); save(b); render(); });
        }
    }

    private JSONArray getItems() { try { return new JSONArray(sp.getString(KEY, "[]")); } catch(Exception e) { return new JSONArray(); } }
    private void save(JSONArray arr) { sp.edit().putString(KEY, arr.toString()).apply(); }
    private double parse(String s) { try { return Double.parseDouble(s.trim()); } catch(Exception e) { return 0; } }
    private String money(double v) { return String.format(java.util.Locale.CHINA, "%.2f", v); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView tv(String s, int size, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); t.setTextColor(Color.rgb(35,35,35)); return t; }
    private EditText input(String hint) { EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(true); e.setPadding(0, dp(8), 0, dp(8)); return e; }
    private Button btn(String s) { Button b = new Button(this); b.setText(s); b.setTextSize(17); return b; }
    private Button smallBtn(String s) { Button b = new Button(this); b.setText(s); b.setTextSize(12); return b; }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private void openUrl(String u) { if (u == null || u.trim().length() == 0) { toast("没有填写链接"); return; } startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "价格提醒", NotificationManager.IMPORTANCE_DEFAULT);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }
    private void requestNotifyPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
    }
    private void notifyPrice(String name, double cur, double tar) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("京东价格提醒").setContentText(name + " 当前 ¥" + money(cur) + "，已低于目标价 ¥" + money(tar)).setAutoCancel(true);
        ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify((int)(System.currentTimeMillis()%100000), b.build());
    }
}
