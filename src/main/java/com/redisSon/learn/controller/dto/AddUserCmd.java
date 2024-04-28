package com.redisSon.learn.controller.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddUserCmd {
    String userName;

    Boolean sex;

    Integer age;
}
