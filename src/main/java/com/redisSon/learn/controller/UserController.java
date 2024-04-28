package com.redisSon.learn.controller;

import com.redisSon.learn.entity.User;
import com.redisSon.learn.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redisson")
public class UserController {
    @Autowired
    UserService userService;
@GetMapping("/findById")
public User findById(@RequestParam long id)
{
    return userService.findById(id);
}

@DeleteMapping("/deleteById")
    public boolean deleteById(@RequestParam long id)
{
    return userService.delete(id);
}

@PostMapping("/updateById")
    public User updateById(@RequestBody User user)
{
    return userService.update(user);
}
@PostMapping("/insert")
    public User insert(@RequestBody User user)
{
    return userService.insert(user);
}

}
