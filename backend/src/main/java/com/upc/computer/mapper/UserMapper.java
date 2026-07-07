package com.upc.computer.mapper;

import com.upc.computer.entity.User;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface UserMapper {

    // 查询所有用户
    @Select("SELECT user_id, role_id, username, password_hash, real_name, employee_no, phone, email, department, status, last_login_at, created_at, updated_at FROM `user`")
    public ArrayList<User> userList();

    // 根据主键查询用户
    @Select("SELECT user_id, role_id, username, password_hash, real_name, employee_no, phone, email, department, status, last_login_at, created_at, updated_at FROM `user` WHERE user_id = #{userId}")
    public User getUserById(Long userId);

    // 根据用户名查询用户
    @Select("SELECT user_id, role_id, username, password_hash, real_name, employee_no, phone, email, department, status, last_login_at, created_at, updated_at FROM `user` WHERE username = #{username}")
    public User getUserByUsername(String username);

    // 根据用户名和密码查询用户（登录）
    @Select("SELECT user_id, role_id, username, password_hash, real_name, employee_no, phone, email, department, status, last_login_at, created_at, updated_at FROM `user` WHERE username = #{username} AND password_hash = #{passwordHash}")
    public User getUserByUsernameAndPassword(String username, String passwordHash);

    // 新增用户
    @Insert("INSERT INTO `user` (user_id, role_id, username, password_hash, real_name, employee_no, phone, email, department, status, last_login_at, created_at, updated_at) VALUES (#{userId}, #{roleId}, #{username}, #{passwordHash}, #{realName}, #{employeeNo}, #{phone}, #{email}, #{department}, #{status}, #{lastLoginAt}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    public void insertUser(User user);

    // 修改用户
    @Update("UPDATE `user` SET role_id=#{roleId}, username=#{username}, password_hash=#{passwordHash}, real_name=#{realName}, employee_no=#{employeeNo}, phone=#{phone}, email=#{email}, department=#{department}, status=#{status}, last_login_at=#{lastLoginAt}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE user_id = #{userId}")
    public void updateUser(User user);

    // 删除用户
    @Delete("DELETE FROM `user` WHERE user_id = #{userId}")
    public void deleteUser(Long userId);

}
