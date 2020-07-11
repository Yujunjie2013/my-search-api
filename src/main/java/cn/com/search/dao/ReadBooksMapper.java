package cn.com.search.dao;

import cn.com.search.core.Mapper;
import cn.com.search.model.ReadBooks;
import cn.com.search.vo.BookSearchResultVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReadBooksMapper extends Mapper<ReadBooks> {
    List<BookSearchResultVo> findByDesc(@Param("keyWord") String keyWord);
}