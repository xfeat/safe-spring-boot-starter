package cn.ocoop.framework.safe.auth.controller;

import cn.ocoop.framework.safe.SafeProperties;
import cn.ocoop.framework.safe.SessionManager;
import com.github.botaruibo.xvcode.generator.Generator;
import com.github.botaruibo.xvcode.generator.PngVCGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class CaptchaController {
    @Autowired
    SafeProperties safeProperties;

    @RequestMapping("/captcha")
    public void captcha(HttpServletResponse response) throws IOException {
        Generator generator = new PngVCGenerator(
                safeProperties.getCaptcha().getWidth(),
                safeProperties.getCaptcha().getHeight(),
                safeProperties.getCaptcha().getLength()
        );
        generator.write2out(response.getOutputStream());
        SessionManager.setAttribute(SafeProperties.SessionProperties.DEFAULT_SESSION_CAPTCHA, generator.text());
    }

}
