package top.anets.module.ding;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author ftm
 * @date 2023/3/23 0023 15:02
 */
@Slf4j
@Service
public class SendDingService
{
    @Value("${ding.robot.webhook:https://oapi.dingtalk.com/robot/send?access_token=c9c9b1c10740df95524b5aef39e27bf1240848ac27bd1b406b4cd4a8106e74f9}")
    private String robotWebhook;

    @Value("${ding.robot.key:SEC638456b26e8209809ee536d57db0633f8fd2a8ea213dfa10425de96bf7c0c2b9}")
    private String robotKey;

    @Autowired
    private RestTemplate restTemplate;

    private final static String MSG_TYPE_TEXT = "text";
    private final static String MSG_TYPE_LINK = "link";
    private final static String MSG_TYPE_MARKDOWN = "markdown";
    private final static String MSG_ACTION_CARD = "actionCard";
    private final static String MSG_FEED_CARD = "feedCard";

    private ThreadLocal<SendRequestParam> paramLocal = new ThreadLocal<>();

    /**
     * 发送文本消息
     *
     * @param text
     * @return OapiRobotSendRequest
     * @author 千面
     * @date 2021/11/25 21:21
     */
    public SendDingService sendText(SendRequestParam.Text text)
    {
        SendRequestParam request = new SendRequestParam();
        request.setText(text);
        request.setMsgtype(MSG_TYPE_TEXT);
        paramLocal.set(request);
        return this;
    }

    /**
     * 发送带连接的消息
     *
     * @param link
     * @return OapiRobotSendRequest
     * @author 千面
     * @date 2021/11/25 21:21
     */
    public SendDingService sendLink(SendRequestParam.Link link)
    {
        SendRequestParam request = new SendRequestParam();
        request.setMsgtype(MSG_TYPE_LINK);
        request.setLink(link);
        paramLocal.set(request);
        return this;
    }

    /**
     * 发送markdown消息
     *
     * @param markdown
     * @return OapiRobotSendRequest
     * @author 千面
     * @date 2021/11/25 21:21
     */
    public SendDingService sendMarkdown(SendRequestParam.Markdown markdown)
    {
        SendRequestParam request = new SendRequestParam();
        request.setMsgtype(MSG_TYPE_MARKDOWN);
        request.setMarkdown(markdown);
        paramLocal.set(request);
        return this;
    }

    public String sendActionCard(SendRequestParam.Actioncard actioncard)
    {
        SendRequestParam request = new SendRequestParam();
        request.setMsgtype(MSG_ACTION_CARD);
        request.setActionCard(actioncard);
        paramLocal.set(request);
        return this.send();
    }


    public String sendFeedcard(SendRequestParam.Feedcard feedcard)
    {
        SendRequestParam request = new SendRequestParam();
        request.setMsgtype(MSG_FEED_CARD);
        request.setFeedCard(feedcard);
        paramLocal.set(request);
        return this.send();
    }

    /**
     * 推送钉钉机器人消息
     *
     * @return
     */
    public String send()
    {
        return this.send(null);
    }

    /**
     * 推送钉钉机器人消息
     *
     * @return
     */
    public String send(SendRequestParam.At at)
    {
        String json = null;
        String dingUrl = null;
        try
        {
            dingUrl = this.getDingUrl();
            //组装请求内容
            SendRequestParam param = paramLocal.get();
            param.setAt(at);
            json = JSON.toJSONString(param);
            return sendRequest(dingUrl, json);
        }
        catch (Exception e)
        {
            log.error("盯盯消息发送错误，接口地址:[{}]，请求参数:[{}]", dingUrl, json, e);
        }
        finally
        {
            paramLocal.remove();
        }
        return null;
    }

    /**
     * 获取 钉钉机器人地址
     *
     * @param
     * @return String
     * @author 千面
     * @date 2021/11/26 13:36
     */
    private String getDingUrl()
    {
        long timestamp = System.currentTimeMillis();
        String sign = HmacSha256Util.dingHmacSHA256(System.currentTimeMillis(), robotKey);
        // 钉钉机器人地址（配置机器人的 webhook） https://oapi.dingtalk.com/robot/send?access_token=XXXXXX&timestamp=XXX&sign=XXX
        return String.format(robotWebhook + "&timestamp=%d&sign=%s", timestamp, sign);
    }

    private String sendRequest(String url, String params)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(params, headers);
        ResponseEntity<String> entity = restTemplate.postForEntity(url, httpEntity, String.class);
        String body = entity.getBody();
        log.info("sendRequest()>>>[{}]", body);
        return body;
    }
}