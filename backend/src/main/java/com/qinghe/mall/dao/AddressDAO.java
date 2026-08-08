package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.AddressDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AddressDAO {

    int insert(AddressDO addressDO);

    int update(AddressDO addressDO);

    int deleteById(@Param("id") Long id);

    /** 清除该用户所有默认标记（设置新默认地址前调用） */
    int clearDefaultByUserId(@Param("userId") Long userId);

    /** 将指定地址设为默认 */
    int setDefault(@Param("id") Long id, @Param("userId") Long userId);

    AddressDO findById(@Param("id") Long id);

    List<AddressDO> findByUserId(@Param("userId") Long userId);

    AddressDO findDefaultByUserId(@Param("userId") Long userId);
}
