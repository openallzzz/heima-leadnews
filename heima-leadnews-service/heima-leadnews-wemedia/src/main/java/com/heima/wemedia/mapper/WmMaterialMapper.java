package com.heima.wemedia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.wemedia.pojos.WmMaterial;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WmMaterialMapper extends BaseMapper<WmMaterial> {

    @Update("update wm_material set is_collection = #{state} where id = #{id}")
    void updateCollect(Integer id, Short state);

}