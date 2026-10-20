package com.hufng.common.dto;

import java.util.List;
import java.util.Objects;

/**
 * @auther macrozheng
 * @description User information wrapper class used in the permission framework
 * @date 2024/1/30
 * @github https://github.com/macrozheng
 */
public class UserDto {
    private Long id;
    private String username;
    private String clientId;
    private List<String> permissionList;

    public UserDto() {
    }

    public UserDto(Long id, String username, String clientId, List<String> permissionList) {
        this.id = id;
        this.username = username;
        this.clientId = clientId;
        this.permissionList = permissionList;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getPermissionList() {
        return permissionList;
    }

    public void setPermissionList(List<String> permissionList) {
        this.permissionList = permissionList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDto userDto)) return false;
        return Objects.equals(id, userDto.id)
                && Objects.equals(username, userDto.username)
                && Objects.equals(clientId, userDto.clientId)
                && Objects.equals(permissionList, userDto.permissionList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, clientId, permissionList);
    }
}
