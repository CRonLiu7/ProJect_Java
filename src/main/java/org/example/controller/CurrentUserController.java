package org.example.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Menu;
import org.example.entity.Role;
import org.example.entity.UserRole;
import org.example.mapper.MenuMapper;
import org.example.mapper.RoleMapper;
import org.example.mapper.UserRoleMapper;
import org.example.security.UserPrincipal;
import org.example.service.RedisService;
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
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public CurrentUserController(UserRoleMapper userRoleMapper, RoleMapper roleMapper, MenuMapper menuMapper,
                                 RedisService redisService, ObjectMapper objectMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/menus")
    public Map<String, Object> currentUserMenus(Authentication authentication) throws Exception {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.getId();

        String cached = redisService.getMenuCache(userId);
        if (cached != null) {
            return objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {});
        }

        List<UserRole> userRoles = userRoleMapper.findByUserId(userId);
        Set<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());

        List<Role> roles = roleIds.stream()
                .map(roleMapper::findById)
                .filter(Objects::nonNull)
                .toList();
        List<String> roleCodes = roles.stream().map(Role::getCode).toList();

        List<Menu> allMenus = menuMapper.findAll();
        List<Map<String, Object>> tree = buildMenuTree(allMenus);

        Map<String, Object> result = Map.of(
                "roles", roleCodes,
                "menus", tree
        );
        redisService.setMenuCache(userId, objectMapper.writeValueAsString(result));
        return result;
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

