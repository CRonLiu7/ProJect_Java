package org.example.controller;

import org.example.entity.Menu;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.mapper.MenuMapper;
import org.example.mapper.RoleMapper;
import org.example.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阶段 1～2：验证 Web、MyBatis 连库、权限与文件表。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;

    public HealthController(UserMapper userMapper, RoleMapper roleMapper, MenuMapper menuMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello, ProJect_Java.";
    }

    /**
     * 查库：若 Flyway + MyBatis 正常，会返回 admin 用户信息。
     */
    @GetMapping("/db-check")
    public Map<String, Object> dbCheck() {
        User admin = userMapper.findByUsername("admin");
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("message", admin != null ? "DB connected, sys_user found." : "DB connected, no admin user.");
        result.put("user", admin);
        return result;
    }

    /** 阶段 2：角色列表，验证 sys_role 与 RoleMapper。 */
    @GetMapping("/roles")
    public Map<String, Object> roles() {
        List<Role> list = roleMapper.findAll();
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("data", list);
        return result;
    }

    /** 阶段 2：菜单列表，验证 sys_menu 与 MenuMapper。 */
    @GetMapping("/menus")
    public Map<String, Object> menus() {
        List<Menu> list = menuMapper.findAll();
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("data", list);
        return result;
    }
}
