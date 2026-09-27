package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.UserDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 用户 Mapper。
 */
@Repository
public interface UserMapper {

    UserDO selectById(@Param("id") Long id);

    UserDO selectByUsername(@Param("username") String username);

    int insert(UserDO user);

    int updateById(UserDO user);
}
