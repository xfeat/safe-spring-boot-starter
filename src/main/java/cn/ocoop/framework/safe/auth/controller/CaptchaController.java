package cn.ocoop.framework.safe.auth.controller;

import cn.ocoop.framework.safe.CaptchaProperties;
import cn.ocoop.framework.safe.SessionManager;
import cn.ocoop.framework.safe.ann.handler.CaptchaAnnotationMethodInterceptor;
import com.wf.captcha.base.Captcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.io.IOException;


@RestController
@RequestMapping("/secure")
public class CaptchaController {

    @Autowired
    private CaptchaProperties captchaProperties;

    @GetMapping("/captcha")
    public String captcha() throws IOException, FontFormatException {
        Captcha captcha = captchaProperties.getCaptcha();
        SessionManager.setAttribute(CaptchaAnnotationMethodInterceptor.DEFAULT_SESSION_CAPTCHA, captcha.text());
        return captcha.toBase64();
    }
}
