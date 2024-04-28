package com.redisSon.learn.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@TableName("user")
@NoArgsConstructor
public class User implements Serializable {
    @TableId
    Long id;
    @TableField("user_name")
    String userName;
    @TableField("sex")
    Boolean sex;
    @TableField("age")
    Integer age;
}
