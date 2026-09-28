package com.school.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.lostfound.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
