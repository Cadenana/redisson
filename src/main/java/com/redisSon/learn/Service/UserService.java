package com.redisSon.learn.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.redisSon.learn.entity.User;

public interface UserService extends IService<User> {

    public boolean delete(Long id);
    public User findById(Long id);
    public User update(User user);
    public User insert(User user);
}
