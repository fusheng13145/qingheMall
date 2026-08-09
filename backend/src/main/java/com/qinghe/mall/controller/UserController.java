package com.qinghe.mall.controller;

import com.qinghe.mall.config.RateLimit;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private MerchantService merchantService;

    /**
     * 注册限流：5 次/秒，容量 10（防批量注册）。
     * role=USER 注册即生效；role=MERCHANT 创建商家账号 + 入驻申请（PENDING，待平台审核）。
     */
    @RateLimit(rate = 5, message = "注册过于频繁，请稍后再试")
    @PostMapping("/reg")
    public Result<User> reg(@RequestParam("userName") String userName,
                            @RequestParam("pwd") String pwd,
                            @RequestParam(value = "role", defaultValue = "USER") String role,
                            @RequestParam(value = "shopName", required = false) String shopName) {
        User user;
        if (UserDO.ROLE_MERCHANT.equals(role)) {
            user = merchantService.registerMerchant(userName, pwd, shopName, null, null);
        } else {
            user = userService.register(userName, pwd);
        }
        user.setPwd(null);
        return Result.success(user);
    }

    /** 登录限流：5 次/秒，容量 10（防暴力破解） */
    @RateLimit(rate = 5, message = "登录过于频繁，请稍后再试")
    @PostMapping("/login")
    public Result<User> login(@RequestParam("userName") String userName, @RequestParam("pwd") String pwd,
                              HttpServletRequest request) {
        User user = userService.login(userName, pwd);
        // P1-14：登录成功后更换会话 ID，防止会话固定攻击（攻击者预置会话被利用）。
        // Boot 3（Servlet 6）下 changeSessionId 要求会话已存在：先 getSession(true) 创建再换 ID
        jakarta.servlet.http.HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getUserName());
        session.setAttribute("nickName", user.getNickName());
        session.setAttribute("role", user.getRole());
        // 不返回密码
        user.setPwd(null);
        return Result.success(user);
    }

    @GetMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        request.getSession().removeAttribute("userId");
        request.getSession().removeAttribute("userName");
        request.getSession().removeAttribute("nickName");
        request.getSession().removeAttribute("role");
        return Result.success();
    }

    /**
     * 更新个人资料（昵称/头像）。
     */
    @PostMapping("/updateProfile")
    public Result<User> updateProfile(@RequestParam(value = "nickName", required = false) String nickName,
                                      @RequestParam(value = "avatar", required = false) String avatar,
                                      HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        // P2：头像 URL 协议白名单——仅允许站内相对路径或 http(s) 外链，拒绝 data:/javascript: 等
        if (StringUtils.isNotBlank(avatar)
                && !avatar.startsWith("/uploads/")
                && !avatar.startsWith("/api/")
                && !avatar.startsWith("https://")
                && !avatar.startsWith("http://")) {
            return Result.fail(400, "头像地址不合法");
        }
        User user = userService.updateProfile((Long) userIdObj, nickName, avatar);
        // 同步 Session 中的昵称
        if (user != null && user.getNickName() != null) {
            request.getSession().setAttribute("nickName", user.getNickName());
        }
        return Result.success(user);
    }

    @GetMapping("/checkLogin")
    public Result<User> checkLogin(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        User user = new User();
        user.setId((Long) userIdObj);
        user.setUserName((String) session.getAttribute("userName"));
        user.setNickName((String) session.getAttribute("nickName"));
        user.setRole((String) session.getAttribute("role"));
        return Result.success(user);
    }
}
