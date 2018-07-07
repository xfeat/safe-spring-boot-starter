package cn.ocoop.framework.safe.auth.controller;


import cn.ocoop.framework.safe.SessionManager;
import cn.ocoop.framework.safe.utils.Result;
import cn.ocoop.framework.safe.ann.RequiresAuthentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@Validated
@RestController
public class LogoutController {

    @RequiresAuthentication
    @RequestMapping("/logout")
    public Result logout(HttpServletResponse response) {
        SessionManager.logout(response);
        return Result.build("SUCCESS", "退出登录成功");
    }

}
