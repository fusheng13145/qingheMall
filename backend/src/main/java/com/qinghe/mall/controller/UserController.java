package com.qinghe.mall.controller;

import com.qinghe.mall.model.Result;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/reg")
    public Result<User> reg(@RequestParam("userName") String userName, @RequestParam("pwd") String pwd) {
        try {
            User user = userService.register(userName, pwd);
            return Result.success(user);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<User> login(@RequestParam("userName") String userName, @RequestParam("pwd") String pwd,
                              HttpServletRequest request) {
        try {
            User user = userService.login(userName, pwd);
            request.getSession().setAttribute("userId", user.getId());
            request.getSession().setAttribute("userName", user.getUserName());
            request.getSession().setAttribute("nickName", user.getNickName());
            request.getSession().setAttribute("role", user.getRole());
            // 不返回密码
            user.setPwd(null);
            return Result.success(user);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        request.getSession().removeAttribute("userId");
        request.getSession().removeAttribute("userName");
        request.getSession().removeAttribute("nickName");
        request.getSession().removeAttribute("role");
        return Result.success();
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
