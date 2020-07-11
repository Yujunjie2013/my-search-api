package cn.com.search.service;
import cn.com.search.model.ReadBooks;

import java.util.List;

import cn.com.search.core.Service;
import cn.com.search.vo.BookSearchResultVo;


/**
 * Created by CodeGenerator on 2018/12/16.
 */
public interface ReadBooksService extends Service<ReadBooks> {

	List<ReadBooks> search(String keyWords);
	
	public Boolean creatIndex();

    void index();

	List<BookSearchResultVo> findBySql(String keyWord);
}
