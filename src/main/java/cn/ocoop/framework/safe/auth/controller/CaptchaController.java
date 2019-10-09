package cn.ocoop.framework.safe.auth.controller;

import cn.ocoop.framework.safe.CaptchaProperty;
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
    private CaptchaProperty captchaProperty;

    @GetMapping("/captcha")
    public String captcha() throws IOException, FontFormatException {
        Captcha captcha = captchaProperty.getCaptcha();
        SessionManager.setAttribute(CaptchaAnnotationMethodInterceptor.DEFAULT_SESSION_CAPTCHA, captcha.text());
        return captcha.toBase64();
    }
}
