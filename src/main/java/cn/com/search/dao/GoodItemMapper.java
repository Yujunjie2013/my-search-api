package cn.com.search.dao;

import cn.com.search.core.Mapper;
import cn.com.search.model.GeoInfo;
import cn.com.search.model.GoodItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodItemMapper extends Mapper<GoodItem> {

    List<GeoInfo> selectAllGeo();

    GeoInfo findGeoInfoById(@Param("id") Long id);
}