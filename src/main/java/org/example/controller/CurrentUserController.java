package org.example.controller;

import org.example.entity.Menu;
import org.example.entity.Role;
import org.example.entity.UserRole;
import org.example.mapper.MenuMapper;
import org.example.mapper.RoleMapper;
import org.example.mapper.UserRoleMapper;
import org.example.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class CurrentUserController {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;

    public CurrentUserController(UserRoleMapper userRoleMapper, RoleMapper roleMapper, MenuMapper menuMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
    }

    @GetMapping("/menus")
    public Map<String, Object> currentUserMenus(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.getId();

        List<UserRole> userRoles = userRoleMapper.findByUserId(userId);
        Set<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());

        // 角色转权限标识（如 ADMIN、USER）
        List<Role> roles = roleIds.stream()
                .map(roleMapper::findById)
                .filter(Objects::nonNull)
                .toList();
        List<String> roleCodes = roles.stream().map(Role::getCode).toList();

        // 简化：当前阶段先直接返回所有菜单树，后续再按角色过滤
        List<Menu> allMenus = menuMapper.findAll();
        List<Map<String, Object>> tree = buildMenuTree(allMenus);

        return Map.of(
                "roles", roleCodes,
                "menus", tree
        );
    }

    private List<Map<String, Object>> buildMenuTree(List<Menu> menus) {
        Map<Long, Map<String, Object>> idToNode = new HashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (Menu m : menus) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", m.getId());
            node.put("parentId", m.getParentId());
            node.put("name", m.getName());
            node.put("path", m.getPath());
            node.put("permission", m.getPermission());
            node.put("type", m.getType());
            node.put("children", new ArrayList<Map<String, Object>>());
            idToNode.put(m.getId(), node);
        }

        for (Menu m : menus) {
            Map<String, Object> node = idToNode.get(m.getId());
            Long parentId = m.getParentId();
            if (parentId == null || parentId == 0) {
                roots.add(node);
            } else {
                Map<String, Object> parent = idToNode.get(parentId);
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }
}

