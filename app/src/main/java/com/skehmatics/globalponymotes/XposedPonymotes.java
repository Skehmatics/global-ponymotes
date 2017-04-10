package com.skehmatics.globalponymotes;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;


public class XposedPonymotes implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static final String PKG_NAME = "com.skehmatics.globalponymotes";
    public static final String PONY_REGEX = "\\[]\\(/[^\\()\\[]+\\)";
    public static final Pattern PONY_PATTERN = Pattern.compile(PONY_REGEX);

    public static String PATH_TO_PONY;

    private boolean shortcut;
    private float scale;
    private boolean altShortcutMethod;
    private boolean composeFlag;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XSharedPreferences prefs = new XSharedPreferences(PKG_NAME);
        shortcut = prefs.getBoolean("shortcutEnabled", false);
        altShortcutMethod = prefs.getBoolean("altShortcutMethod", false);
        scale = Float.valueOf(prefs.getString("scalePref", "1"));
        composeFlag = prefs.getBoolean("composeFlag", true);
        String customPath = prefs.getString("customPath", "");

        PATH_TO_PONY = customPath.trim().isEmpty() ? Environment.getExternalStorageDirectory().toString() + "/RedditEmotes/" : customPath;
        XposedBridge.log("GPM - Setting emote directory to " + PATH_TO_PONY);
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        //Blacklist certain apps
        for (String disabledPkg : new String[]{"reddit.news", "com.dinsfire.globalponymotes"}) {
            if (lpparam.packageName.contains(disabledPkg) || lpparam.processName.contains(disabledPkg)){
                return;
            }
        }

        //Self-hook.
        if(lpparam.packageName.equals(PKG_NAME)) {
            findAndHookMethod(PKG_NAME + ".MainActivity", lpparam.classLoader, "isXposedEnabled", XC_MethodReplacement.returnConstant(true));
        }

        final XC_MethodHook textMethodHook = new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                CharSequence actualText = (CharSequence) methodHookParam.args[0];
                TextView textView = (TextView) methodHookParam.thisObject;

                if (!textView.isFocused() && actualText != null && PONY_PATTERN.matcher(actualText).find()) {
                    List<Emote> emotes = findEmotes(actualText);

                    SpannableString newString = new SpannableString(actualText);
                    int flags = composeFlag ? Spannable.SPAN_MARK_POINT | Spannable.SPAN_COMPOSING : Spannable.SPAN_MARK_POINT;

                    for (final Emote emote : emotes) {

                        if (emote.name.equals("sp")) {
                            newString.setSpan(new ImageSpan(Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)), emote.start, emote.end, flags);
                            continue;
                        }

                        final Drawable emoteDrawable = Drawable.createFromPath(PATH_TO_PONY + emote.name + ".png");

                        if (emoteDrawable == null) {
                            XposedBridge.log("GPM - Failed Emote: " + emote.name);
                            continue;
                        }
                        float density = textView.getResources().getDisplayMetrics().density;

                        emoteDrawable.setBounds(0, 0,
                                Math.round(emoteDrawable.getIntrinsicWidth() * density * scale),
                                Math.round(emoteDrawable.getIntrinsicHeight() * density * scale));

                        newString.setSpan(new ImageSpan(emoteDrawable, ImageSpan.ALIGN_BOTTOM), emote.start, emote.end, flags);

                        final SpannableStringBuilder toastText = new SpannableStringBuilder();

                        if (emote.hasQuote()) {
                            toastText.append("/" + emote.name + " - " + emote.quote);
                            toastText.setSpan(new StyleSpan(Typeface.ITALIC), toastText.length() - emote.quote.length(), toastText.length() - 1, Spannable.SPAN_POINT_MARK);
                        } else {
                            toastText.append("/" + emote.name + "");
                        }

                        newString.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(View widget) {
                                Toast.makeText(AndroidAppHelper.currentApplication(), toastText, Toast.LENGTH_LONG).show();
                            }
                        }, emote.start, emote.end, flags);
                    }

                    methodHookParam.args[0] = newString;
                }
            }
        };

        final XC_MethodHook clipboardMenuHook = new XC_MethodHook() {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                int POPUP_TEXT_LAYOUT;
                Object mEditor;
                final TextView mTextView;
                TextView mMotesTextView;
                ViewGroup mContentView;

                mEditor = XposedHelpers
                        .getSurroundingThis(methodHookParam.thisObject);
                mTextView = (TextView) XposedHelpers.getObjectField(
                        mEditor, "mTextView");

                mContentView = (ViewGroup) XposedHelpers
                        .getObjectField(methodHookParam.thisObject,
                                "mContentView");

                ArrayList<View> views = new ArrayList<>();
                mContentView.findViewsWithText(views, "Ponymote", View.FIND_VIEWS_WITH_TEXT);
                if (!views.isEmpty()) {
                    return;
                }

                POPUP_TEXT_LAYOUT = XposedHelpers.getIntField(
                        methodHookParam.thisObject, "POPUP_TEXT_LAYOUT");

                LayoutInflater inflater = (LayoutInflater) mTextView
                        .getContext().getSystemService(
                                Context.LAYOUT_INFLATER_SERVICE);

                ViewGroup.LayoutParams wrapContent = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

                mMotesTextView = (TextView) inflater.inflate(
                        POPUP_TEXT_LAYOUT, null);
                mMotesTextView.setLayoutParams(wrapContent);
                mContentView.addView(mMotesTextView);
                mMotesTextView.setText("Ponymote");

                mMotesTextView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("ponymotes://"));
                        ((Activity) v.getContext()).startActivityForResult(i, 857);
                    }
                });
            }
        };

        //PonyMote injection hooks
        findAndHookMethod(TextView.class, "setText", CharSequence.class,
                TextView.BufferType.class, boolean.class, int.class, textMethodHook);
        findAndHookMethod(TextView.class, "setHint", CharSequence.class, textMethodHook);
        findAndHookMethod(TextView.class, "append", CharSequence.class, textMethodHook);
        findAndHookMethod(TextView.class, "append", CharSequence.class, int.class, int.class,
                textMethodHook);

        //Shortcut injection hooks
        if (shortcut) {
            findAndHookMethod(ClipboardManager.class, "hasPrimaryClip", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    return true;
                }
            });
            findAndHookMethod(Activity.class, "onActivityResult", int.class, int.class, Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    if (methodHookParam.args[0].equals(857)) {
                        Intent data = (Intent) methodHookParam.args[2];
                        if (data.getStringExtra(Intent.EXTRA_STREAM) != null) {
                            String emoteStream = data.getStringExtra(Intent.EXTRA_STREAM);
                            Activity thisActivity = (Activity) methodHookParam.thisObject;
                            EditText editText = (EditText) thisActivity.getCurrentFocus();
                            if (editText != null && !editText.getText().toString().contains(emoteStream)) {
                                editText.append(emoteStream);
                            }
                        }
                    }
                }
            });

            findAndHookMethod("android.widget.Editor.ActionPopupWindow",
                    lpparam.classLoader, "initContentView", clipboardMenuHook);
        }
    }

    public static List<Emote> findEmotes (CharSequence input){
        List<Emote> allMatches = new ArrayList<>();
        Matcher m = PONY_PATTERN.matcher(input);
        while (m.find()) {
            allMatches.add(new Emote(m.group(), m.start(), m.end()));
        }
        return allMatches;
    }
}
