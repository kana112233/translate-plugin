package com.youzi.util;

import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.youzi.constant.Contstants;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fest.swing.timing.Timeout;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.net.URLEncoder;
import java.util.List;

/**
 * GoogleTranslateUtil
 * @author tangyouzhi
 * @date 2018年12月26日14:30:06
 */
public class GoogleTranslateUtil {


    /**
     * 英译中API地址
     */
    private static final String YINGYIZHONG = "https://translate.google.cn/translate_a/single?client=webapp&sl=en&tl=zh-CN&hl=zh-CN&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&dt=t&source=bh&ssel=0&tsel=0&kc=1&tk=#{tk}&q=#{word}";

    /**
     * 中译英API地址
     */
    private static final String ZHONGYIYIN = "https://translate.google.cn/translate_a/single?client=webapp&sl=zh-CN&tl=en&hl=zh-CN&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&dt=t&source=bh&ssel=0&tsel=0&kc=1&tk=#{tk}&q=#{word}";

    /**
     * 编辑器实例
     */
    private static Editor editor;

    /**
     * 执行JS函数,模拟TK参数生成
     * @param word
     * @return
     * @throws Exception
     */
    private static String invokeJSFunc(String word) throws Exception {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("javascript");
        engine.eval(Contstants.TK_FUNC);
        Invocable inv = (Invocable) engine;
        Object res = (Object) inv.invokeFunction("tk", word);
        return res.toString();
    }


    /**
     * 调用接口,翻译并返回值
     * @param word
     * @param fyMethod
     * @param mEditor
     * @return
     * @throws Exception
     */
    public static String translate(String word, String fyMethod, Editor mEditor) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();

        //replace填坑参数地址值
        String url = getUrl(fyMethod).replace("#{tk}", invokeJSFunc(word)).replace("#{word}", URLEncoder.encode(word, "utf-8"));
        HttpGet get = new HttpGet(url);
        get.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        get.setHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9");

        //发起请求
        CloseableHttpResponse response = client.execute(get);
        if (200 == response.getStatusLine().getStatusCode()) {

            String responseText = EntityUtils.toString(response.getEntity(), "utf-8");
            Gson gson = new Gson();
            List<Object> list = gson.fromJson(responseText, List.class);

            StringBuilder result = new StringBuilder();
            if (null != list && list.size() > 0) {
                for (Object obj : (List<Object>) list.get(0)) {
                    result.append(null != ((List<String>) obj).get(0) ? ((List<String>) obj).get(0) + "\n" : "");
                }
            }

            GoogleTranslateUtil.editor = mEditor;
            return result.toString();
        }
        return null;
    }

    /**
     * 根据类型选择接口地址
     *
     * @param fyMethod
     * @return
     */
    private static String getUrl(String fyMethod) {
        if (fyMethod.equals(Contstants.CN_TO_EN)) {
            return ZHONGYIYIN;
        } else {
            return YINGYIZHONG;
        }
    }

    /**
     * 文本弹出显示
     *
     * @param result
     * @link ECTranslation->RequestRunnable
     */
    public static void showPopupBalloon(final String result) {
        ApplicationManager.getApplication().invokeLater((Runnable) new Runnable() {
            @Override
            public void run() {
                final JBPopupFactory factory = JBPopupFactory.getInstance();
                factory.createHtmlTextBalloonBuilder(result, (Icon) null, (Color) new JBColor(new Color(186, 238, 186), new Color(73, 117, 73)), (HyperlinkListener) null).setFadeoutTime(5000L).createBalloon().show(factory.guessBestPopupLocation(GoogleTranslateUtil.editor), Balloon.Position.below);
            }
        });
    }

}
