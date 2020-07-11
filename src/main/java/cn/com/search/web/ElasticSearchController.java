package cn.com.search.web;

import cn.com.search.core.Result;
import cn.com.search.core.ResultGenerator;
import cn.com.search.model.ReadBooks;
import cn.com.search.service.ElasticSearchService;
import cn.com.search.service.ReadBooksService;
import cn.com.search.util.LuceneUtils;
import cn.com.search.vo.BookSearchParam;
import cn.com.search.vo.BookSearchResultVo;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/es")
@Slf4j
public class ElasticSearchController {

    @Autowired
    ReadBooksService booksService;
    @Autowired
    ElasticSearchService esSeachService;

    @GetMapping("/creatIndex")
    public Result creatIndex() {
        booksService.creatIndex();        //做一次全量数据插入 做一次就行了。
        return ResultGenerator.genSuccessResult();
    }

    @GetMapping("/index")
    public Result creatIndexByLucene() {
        booksService.index();        //做一次全量数据插入 做一次就行了。
        return ResultGenerator.genSuccessResult();
    }

    @GetMapping("/search")
    public Result search(@RequestParam(required = false) String keyWord) {

        BookSearchParam bookSearchParam = new BookSearchParam();
        bookSearchParam.setDesc("童话故事");
        List<BookSearchResultVo> books = esSeachService.queryDocumentByParam("test-es", "test-type", bookSearchParam);
        return ResultGenerator.genSuccessResult(books);
    }

    @GetMapping("/search/lucene")
    public Result searchByLucene(@RequestParam(required = false) String keyWord) {
        long l = System.currentTimeMillis();
        List<BookSearchResultVo> books = esSeachService.search(keyWord);
        long l1 = System.currentTimeMillis();
        System.out.println("搜索" + keyWord + "耗时: " + (l1 - l) + " 毫秒");
        return ResultGenerator.genSuccessResult(books);
    }

    @GetMapping("/search/sql")
    public Result searchBySql(@RequestParam(required = false) String keyWord) {
        long l = System.currentTimeMillis();
        List<BookSearchResultVo> books = booksService.findBySql(keyWord);
        long l1 = System.currentTimeMillis();
        System.out.println("查询" + keyWord + "耗时: " + (l1 - l) + " 毫秒");
        return ResultGenerator.genSuccessResult(books);
    }


    @GetMapping("/create")
    public Result create() {
        PageHelper.startPage(1,10000);
        List<ReadBooks> all = booksService.findAll();
        for (int i = 0; i < all.size(); i++) {
            ReadBooks readBooks = all.get(i);
            String str = "中国";
            if (i % 5 == 1) {
                str = "感性";
                readBooks.setAuthor("李四");
            } else if (i % 5 == 2) {
                str = "剧情";
                readBooks.setAuthor("王二麻子");
            } else if (i % 5 == 3) {
                str = "悬疑";
                readBooks.setAuthor("张三");
            } else if (i % 5 == 4) {
                str = "恐怖";
                readBooks.setAuthor("赵六");
            } else {
                str = "玄幻";
                readBooks.setAuthor("王五");
            }
            readBooks.setDiscription(str + readBooks.getDiscription() + "id：" + readBooks.getId());
            readBooks.setId(null);
        }
        booksService.save(all);
        try {
            LuceneUtils.addAll(all);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResultGenerator.genSuccessResult();
    }
}
